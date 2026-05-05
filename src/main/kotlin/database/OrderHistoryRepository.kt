package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
            val itemCounts =
                if (orderIds.isEmpty()) {
                    emptyMap()
                } else {
                    OrderItem
                        .selectAll()
                        .where { OrderItem.orderId inList orderIds }
                        .groupBy { it[OrderItem.orderId] }
                        .mapValues { entry ->
                            entry.value.sumOf { row -> row[OrderItem.quantity] ?: 1 }
                        }
                }

            orders.map { row ->
                CustomerOrderHistoryResponse(
                    orderId = row[Order.id],
                    status = row[Order.status].name,
                    deliveryWindowStart = row[Order.deliveryWindowStart].toString(),
                    deliveryWindowEnd = row[Order.deliveryWindowEnd].toString(),
                    orderTime = row[Order.orderTime].toString(),
                    totalCost = row[Order.totalCost],
                    itemCount = itemCounts[row[Order.id]] ?: 0,
                    deliveryAddress =
                        formatAddress(
                            row[Address.line1],
                            row[Address.line2],
                            row[Address.city],
                            row[Address.postcode],
                        ),
                )
            }
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
