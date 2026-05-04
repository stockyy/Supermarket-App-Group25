package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class ManagerDashboardFilters(
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val category: String?,
    val search: String?,
)

@Serializable
data class ManagerDashboardResponse(
    val generatedAt: String,
    val categories: List<String>,
    val kpis: ManagerKpis,
    val salesByCategory: List<CategorySales>,
    val topProducts: List<ProductSales>,
    val activeOrders: List<ManagerOrderSummary>,
    val picklistWorkload: List<PicklistSectionSummary>,
    val recentPicklists: List<PicklistSummary>,
    val staffPerformance: List<StaffPerformanceSummary>,
    val recentOffsales: List<OffsaleLogSummary>,
    val recentWastage: List<WastageLogSummary>,
    val lowStockProducts: List<LowStockProduct>,
)

@Serializable
data class ManagerKpis(
    val totalProducts: Int,
    val lowStockProducts: Int,
    val activeOrders: Int,
    val activeOrderValue: Float,
    val openPicklists: Int,
    val averagePickRate: Int,
    val staffMembers: Int,
    val pendingOffsales: Int,
    val activeCarts: Int,
    val activeCartValue: Float,
)

@Serializable
data class CategorySales(
    val category: String,
    val orderCount: Int,
    val itemsSold: Int,
    val revenue: Float,
)

@Serializable
data class ProductSales(
    val productId: Int,
    val productName: String,
    val category: String,
    val itemsSold: Int,
    val revenue: Float,
)

@Serializable
data class ManagerOrderSummary(
    val orderId: Int,
    val customerName: String,
    val status: String,
    val totalCost: Float,
    val itemCount: Int,
    val orderTime: String,
    val deliveryWindow: String,
)

@Serializable
data class PicklistSectionSummary(
    val section: String,
    val totalPicklists: Int,
    val unassigned: Int,
    val inProgress: Int,
    val completed: Int,
)

@Serializable
data class PicklistSummary(
    val picklistId: Int,
    val pickerName: String,
    val section: String,
    val status: String,
    val quantity: Int,
    val pickedQuantity: Int,
    val pickRate: Int,
)

@Serializable
data class StaffPerformanceSummary(
    val staffId: String,
    val name: String,
    val role: String,
    val completedPicklists: Int,
    val averagePickRate: Int,
    val lastActive: String,
)

@Serializable
data class OffsaleLogSummary(
    val logId: Int,
    val productName: String,
    val staffName: String,
    val status: String,
    val dateTime: String,
)

@Serializable
data class WastageLogSummary(
    val logId: Int,
    val productName: String,
    val staffName: String,
    val reason: String,
    val amount: String,
    val dateTime: String,
)

@Serializable
data class LowStockProduct(
    val productId: Int,
    val productName: String,
    val category: String,
    val stockLevel: Int,
    val price: Float,
)

object ManagementAnalyticsRepository {
    private const val LOW_STOCK_THRESHOLD = 10

    fun getDashboard(filters: ManagerDashboardFilters): ManagerDashboardResponse =
        transaction {
            val now = LocalDateTime.now()
            val dateStart = filters.dateFrom?.atStartOfDay()
            val dateEnd = filters.dateTo?.plusDays(1)?.atStartOfDay()
            val categoryFilter = filters.category?.trim()?.takeIf { it.isNotEmpty() && it != "all" }
            val searchFilter =
                filters.search
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.isNotEmpty() }

            fun inDateRange(dateTime: LocalDateTime): Boolean =
                (dateStart == null || !dateTime.isBefore(dateStart)) &&
                    (dateEnd == null || dateTime.isBefore(dateEnd))

            val categories = Category.selectAll().map { it[Category.name] }.sorted()
            val sectionsById = Section.selectAll().associate { it[Section.id] to it[Section.name].name }
            val categoriesById = Category.selectAll().associate { it[Category.id] to it[Category.name] }
            val users = Users.selectAll().toList()
            val usersById = users.associateBy { it[Users.id] }
            val products = Product.selectAll().toList()
            val productsById = products.associateBy { it[Product.id] }
            val orders = Order.selectAll().toList()
            val orderItems = OrderItem.selectAll().toList()
            val picklists = Picklist.selectAll().toList()
            val pickItems = PickItem.selectAll().toList()
            val offsaleLogs = OffsaleLog.selectAll().toList()
            val wastageLogs = WastageLog.selectAll().toList()
            val carts = Cart.selectAll().toList()
            val cartItems = CartItem.selectAll().toList()

            val filteredProducts =
                products.filter { product ->
                    val categoryName = categoriesById[product[Product.categoryId]].orEmpty()
                    val name = product[Product.name].lowercase()
                    val matchesCategory = categoryFilter == null || categoryName == categoryFilter
                    val matchesSearch = searchFilter == null || name.contains(searchFilter)
                    matchesCategory && matchesSearch
                }
            val filteredProductIds = filteredProducts.map { it[Product.id] }.toSet()
            val hasProductFilter = categoryFilter != null || searchFilter != null

            val orderIdsWithFilteredProducts =
                orderItems
                    .filter { it[OrderItem.productID] in filteredProductIds }
                    .map { it[OrderItem.orderId] }
                    .toSet()

            val ordersInRange =
                orders.filter { order ->
                    val orderId = order[Order.id]
                    val searchMatchesOrder =
                        searchFilter == null ||
                            orderId.toString().contains(searchFilter) ||
                            orderId in orderIdsWithFilteredProducts
                    val productMatches = categoryFilter == null || orderId in orderIdsWithFilteredProducts
                    inDateRange(order[Order.orderTime]) && productMatches && searchMatchesOrder
                }
            val ordersById = orders.associateBy { it[Order.id] }

            val saleOrderIds = ordersInRange.map { it[Order.id] }.toSet()
            val saleItems =
                orderItems.filter { item ->
                    item[OrderItem.orderId] in saleOrderIds &&
                        (!hasProductFilter || item[OrderItem.productID] in filteredProductIds)
                }

            val itemsByOrder = orderItems.groupBy { it[OrderItem.orderId] }
            val activeOrders =
                ordersInRange
                    .filter { it[Order.status] != OrderStatus.DELIVERED }
                    .sortedBy { it[Order.deliveryWindowStart] }

            val activeOrderSummaries =
                activeOrders.take(12).map { order ->
                    val customer = usersById[order[Order.userId]]
                    ManagerOrderSummary(
                        orderId = order[Order.id],
                        customerName = customer.fullName(),
                        status = order[Order.status].name,
                        totalCost = order[Order.totalCost],
                        itemCount = itemsByOrder[order[Order.id]].orEmpty().sumOf { it[OrderItem.quantity] ?: 1 },
                        orderTime = formatDateTime(order[Order.orderTime]),
                        deliveryWindow =
                            "${formatDateTime(order[Order.deliveryWindowStart])} - " +
                                formatTime(order[Order.deliveryWindowEnd]),
                    )
                }

            val salesByCategory =
                saleItems
                    .groupBy { item ->
                        val product = productsById[item[OrderItem.productID]]
                        categoriesById[product?.get(Product.categoryId)].orEmpty()
                    }.map { (categoryName, items) ->
                        CategorySales(
                            category = categoryName.ifBlank { "Uncategorised" },
                            orderCount = items.map { it[OrderItem.orderId] }.toSet().size,
                            itemsSold = items.sumOf { it[OrderItem.quantity] ?: 1 },
                            revenue = items.sumOf { it[OrderItem.priceAtOrder].toDouble() }.toFloat(),
                        )
                    }.sortedByDescending { it.revenue }

            val topProducts =
                saleItems
                    .groupBy { it[OrderItem.productID] }
                    .mapNotNull { (productId, items) ->
                        val product = productsById[productId] ?: return@mapNotNull null
                        ProductSales(
                            productId = productId,
                            productName = product[Product.name],
                            category = categoriesById[product[Product.categoryId]].orEmpty(),
                            itemsSold = items.sumOf { it[OrderItem.quantity] ?: 1 },
                            revenue = items.sumOf { it[OrderItem.priceAtOrder].toDouble() }.toFloat(),
                        )
                    }.sortedByDescending { it.revenue }
                    .take(8)

            val pickItemsByPicklist = pickItems.groupBy { it[PickItem.picklistId] }
            val picklistSections =
                pickItemsByPicklist.mapValues { (_, items) ->
                    val firstProductId = items.firstOrNull()?.get(PickItem.productId)
                    val firstProduct = firstProductId?.let { productsById[it] }
                    sectionsById[firstProduct?.get(Product.sectionId)] ?: "UNKNOWN"
                }

            val picklistWorkload =
                picklists
                    .groupBy { picklistSections[it[Picklist.id]] ?: "UNKNOWN" }
                    .map { (section, lists) ->
                        PicklistSectionSummary(
                            section = section,
                            totalPicklists = lists.size,
                            unassigned = lists.count { it[Picklist.pickerId] == null },
                            inProgress = lists.count { it[Picklist.pickerId] != null && it[Picklist.timeEnd] == null },
                            completed = lists.count { it[Picklist.timeEnd] != null },
                        )
                    }.sortedBy { it.section }

            val recentPicklists =
                picklists
                    .sortedByDescending { it[Picklist.id] }
                    .take(10)
                    .map { picklist ->
                        val id = picklist[Picklist.id]
                        val pickedQuantity = pickItemsByPicklist[id].orEmpty().sumOf { it[PickItem.qtyPicked] }
                        PicklistSummary(
                            picklistId = id,
                            pickerName = usersById[picklist[Picklist.pickerId]].fullName(),
                            section = picklistSections[id] ?: "UNKNOWN",
                            status = picklistStatus(picklist[Picklist.pickerId], picklist[Picklist.timeEnd]),
                            quantity = picklist[Picklist.quantity],
                            pickedQuantity = pickedQuantity,
                            pickRate =
                                calculatePickRate(
                                    picklist[Picklist.quantity],
                                    picklist[Picklist.timeStart],
                                    picklist[Picklist.timeEnd],
                                ),
                        )
                    }

            val completedPicklists = picklists.filter { it[Picklist.timeEnd] != null }
            val totalPickedQuantity = completedPicklists.sumOf { it[Picklist.quantity] }
            val totalPickSeconds =
                completedPicklists.sumOf {
                    val start = it[Picklist.timeStart]
                    val end = it[Picklist.timeEnd]
                    if (start != null && end != null) Duration.between(start, end).seconds else 0L
                }
            val averagePickRate =
                if (totalPickSeconds > 0) ((totalPickedQuantity.toDouble() / totalPickSeconds) * 3600).roundToInt() else 0

            val staffUsers = users.filter { it[Users.role] != UserRole.CUSTOMER }
            val staffPerformance =
                staffUsers
                    .map { user ->
                        val userPicklists = picklists.filter { it[Picklist.pickerId] == user[Users.id] }
                        val userCompleted = userPicklists.filter { it[Picklist.timeEnd] != null }
                        val userSeconds =
                            userCompleted.sumOf {
                                val start = it[Picklist.timeStart]
                                val end = it[Picklist.timeEnd]
                                if (start != null && end != null) Duration.between(start, end).seconds else 0L
                            }
                        val userQuantity = userCompleted.sumOf { it[Picklist.quantity] }
                        val lastActive =
                            userPicklists
                                .mapNotNull { it[Picklist.timeEnd] ?: it[Picklist.timeStart] }
                                .maxOrNull()

                        StaffPerformanceSummary(
                            staffId = user[Users.staffId] ?: "N/A",
                            name = user.fullName(),
                            role = user[Users.role].name,
                            completedPicklists = userCompleted.size,
                            averagePickRate = if (userSeconds > 0) ((userQuantity.toDouble() / userSeconds) * 3600).roundToInt() else 0,
                            lastActive = lastActive?.let(::formatDateTime) ?: "No activity",
                        )
                    }.sortedWith(compareByDescending<StaffPerformanceSummary> { it.averagePickRate }.thenBy { it.name })

            val recentOffsales =
                offsaleLogs
                    .filter { log ->
                        inDateRange(log[OffsaleLog.dateTime]) &&
                            (!hasProductFilter || log[OffsaleLog.productId] in filteredProductIds)
                    }.sortedByDescending { it[OffsaleLog.dateTime] }
                    .take(8)
                    .map { log ->
                        OffsaleLogSummary(
                            logId = log[OffsaleLog.id],
                            productName = productsById[log[OffsaleLog.productId]]?.get(Product.name) ?: "Unknown product",
                            staffName = usersById[log[OffsaleLog.userId]].fullName(),
                            status = if (log[OffsaleLog.managerReviewed]) "Reviewed" else "Pending review",
                            dateTime = formatDateTime(log[OffsaleLog.dateTime]),
                        )
                    }

            val recentWastage =
                wastageLogs
                    .filter { log ->
                        inDateRange(log[WastageLog.dateTime]) &&
                            (!hasProductFilter || log[WastageLog.productId] in filteredProductIds)
                    }.sortedByDescending { it[WastageLog.dateTime] }
                    .take(8)
                    .map { log ->
                        WastageLogSummary(
                            logId = log[WastageLog.id],
                            productName = productsById[log[WastageLog.productId]]?.get(Product.name) ?: "Unknown product",
                            staffName = usersById[log[WastageLog.userId]].fullName(),
                            reason = log[WastageLog.reason].name.replace('_', ' '),
                            amount = log[WastageLog.quantity]?.let { "$it units" } ?: "${"%.2f".format(log[WastageLog.weight] ?: 0f)} kg",
                            dateTime = formatDateTime(log[WastageLog.dateTime]),
                        )
                    }

            val allLowStockProducts =
                filteredProducts
                    .filter { it[Product.stockLevel] <= LOW_STOCK_THRESHOLD }
                    .sortedBy { it[Product.stockLevel] }

            val lowStockProducts =
                allLowStockProducts
                    .take(12)
                    .map {
                        LowStockProduct(
                            productId = it[Product.id],
                            productName = it[Product.name],
                            category = categoriesById[it[Product.categoryId]].orEmpty(),
                            stockLevel = it[Product.stockLevel],
                            price = it[Product.price],
                        )
                    }

            val cartsWithItems = cartItems.map { it[CartItem.cartId] }.toSet()
            val activeCarts = carts.filter { it[Cart.id] in cartsWithItems && it[Cart.totalCost] > 0f }

            ManagerDashboardResponse(
                generatedAt = formatDateTime(now),
                categories = categories,
                kpis =
                    ManagerKpis(
                        totalProducts = filteredProducts.size,
                        lowStockProducts = allLowStockProducts.size,
                        activeOrders = activeOrders.size,
                        activeOrderValue = activeOrders.sumOf { it[Order.totalCost].toDouble() }.toFloat(),
                        openPicklists = picklists.count { it[Picklist.timeEnd] == null },
                        averagePickRate = averagePickRate,
                        staffMembers = staffUsers.size,
                        pendingOffsales = offsaleLogs.count { !it[OffsaleLog.managerReviewed] },
                        activeCarts = activeCarts.size,
                        activeCartValue = activeCarts.sumOf { it[Cart.totalCost].toDouble() }.toFloat(),
                    ),
                salesByCategory = salesByCategory,
                topProducts = topProducts,
                activeOrders = activeOrderSummaries,
                picklistWorkload = picklistWorkload,
                recentPicklists = recentPicklists,
                staffPerformance = staffPerformance,
                recentOffsales = recentOffsales,
                recentWastage = recentWastage,
                lowStockProducts = lowStockProducts,
            )
        }

    private fun org.jetbrains.exposed.v1.core.ResultRow?.fullName(): String {
        if (this == null) return "Unassigned"
        return "${this[Users.firstName]} ${this[Users.lastName]}"
    }

    private fun picklistStatus(
        pickerId: Int?,
        timeEnd: LocalDateTime?,
    ): String =
        when {
            timeEnd != null -> "Completed"
            pickerId != null -> "In progress"
            else -> "Unassigned"
        }

    private fun calculatePickRate(
        quantity: Int,
        timeStart: LocalDateTime?,
        timeEnd: LocalDateTime?,
    ): Int {
        if (timeStart == null || timeEnd == null) return 0
        val seconds = Duration.between(timeStart, timeEnd).seconds
        return if (seconds > 0) ((quantity.toDouble() / seconds) * 3600).roundToInt() else 0
    }

    private fun formatDateTime(value: LocalDateTime): String = value.toString().replace('T', ' ').take(16)

    private fun formatTime(value: LocalDateTime): String = value.toLocalTime().toString().take(5)
}
