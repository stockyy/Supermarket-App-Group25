package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class CustomerOrderItemResponse(
    val productId: Int,
    val name: String,
    val imageUrl: String,
    val quantity: Int?,
    val weight: Float?,
    val soldByWeight: Boolean,
    val lineTotal: Float,
)

@Serializable
data class CustomerOrderHistoryResponse(
    val orderId: Int,
    val status: String,
    val deliveryWindowStart: String,
    val deliveryWindowEnd: String,
    val orderTime: String,
    val totalCost: Float,
    val itemCount: Int,
    val deliveryAddress: String,
    val items: List<CustomerOrderItemResponse>,
)

object OrderHistoryRepository {
    fun getOrdersForUser(userId: Int): List<CustomerOrderHistoryResponse> =
        transaction {
            val orders =
                (Order innerJoin Address)
                    .selectAll()
                    .where { Order.userId eq userId }
                    .orderBy(Order.deliveryWindowStart to SortOrder.DESC)
                    .toList()

            val orderIds = orders.map { it[Order.id] }
            val itemsByOrderId =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    (OrderItem innerJoin Product)
                        .selectAll()
                        .where { OrderItem.orderId inList orderIds }
                        .groupBy { it[OrderItem.orderId] }
                        .mapValues { entry ->
                            entry.value.map { itemRow ->
                                CustomerOrderItemResponse(
                                    productId = itemRow[Product.id],
                                    name = itemRow[Product.name],
                                    imageUrl = itemRow[Product.imageUrl] ?: "",
                                    quantity = itemRow[OrderItem.quantity],
                                    weight = itemRow[OrderItem.weight],
                                    soldByWeight = itemRow[Product.soldByWeight],
                                    lineTotal = itemRow[OrderItem.priceAtOrder],
                                )
                            }
                        }
                }

            orders.map { row ->
                val items = itemsByOrderId[row[Order.id]] ?: emptyList()
                CustomerOrderHistoryResponse(
                    orderId = row[Order.id],
                    status = row[Order.status].name,
                    deliveryWindowStart = row[Order.deliveryWindowStart].toString(),
                    deliveryWindowEnd = row[Order.deliveryWindowEnd].toString(),
                    orderTime = row[Order.orderTime].toString(),
                    totalCost = row[Order.totalCost],
                    itemCount = countOrderItems(items),
                    deliveryAddress =
                        formatAddress(
                            row[Address.line1],
                            row[Address.line2],
                            row[Address.city],
                            row[Address.postcode],
                        ),
                    items = items,
                )
            }
        }

    private fun countOrderItems(items: List<CustomerOrderItemResponse>): Int =
        items.sumOf { item ->
            item.quantity ?: item.weight?.toInt()?.coerceAtLeast(1) ?: 1
        }

    private fun formatAddress(
        line1: String,
        line2: String?,
        city: String,
        postcode: String,
    ): String =
        listOfNotNull(
            line1,
            line2?.takeIf { it.isNotBlank() },
            city,
            postcode,
        ).joinToString(", ")
}
