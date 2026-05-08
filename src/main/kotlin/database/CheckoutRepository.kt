package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Serializable
data class DeliveryWindowResponse(
    val id: String,
    val start: String,
    val end: String,
    val label: String,
    val fee: Float,
    val available: Boolean,
)

@Serializable
data class CheckoutAddressRequest(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val postcode: String,
)

@Serializable
data class PlaceOrderRequest(
    val deliveryWindowStart: String,
    val deliveryWindowEnd: String,
    val address: CheckoutAddressRequest,
    val payment: CustomerPaymentUpdateRequest? = null,
    val paymentId: Int? = null,
)

@Serializable
data class PlaceOrderResponse(
    val orderId: Int,
    val totalCost: Float,
    val itemCount: Int,
    val deliveryWindow: String,
    val deliveryAddress: String,
)

object CheckoutRepository {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)

    fun getDeliveryWindows(): List<DeliveryWindowResponse> {
        val today = LocalDate.now(ZoneId.of("Europe/London"))
        val slotHours = listOf(8, 10, 12, 14, 16, 18)
        val windows = mutableListOf<DeliveryWindowResponse>()

        for (dayOffset in 1..5) {
            val date = today.plusDays(dayOffset.toLong())

            for (startHour in slotHours) {
                val start = date.atTime(startHour, 0)
                val end = start.plusHours(2)
                val isPeak = startHour == 12 || dayOffset >= 4
                val fee = if (isPeak) 4.99f else 3.99f
                val available = !(dayOffset == 1 && startHour == 16)

                windows.add(
                    DeliveryWindowResponse(
                        id = start.toString(),
                        start = start.toString(),
                        end = end.toString(),
                        label =
                            "${start.format(displayDateFormatter)}, " +
                                "${start.format(displayTimeFormatter)} - " +
                                end.format(displayTimeFormatter),
                        fee = fee,
                        available = available,
                    ),
                )
            }
        }

        return windows
    }

    fun placeOrder(
        userId: Int,
        request: PlaceOrderRequest,
    ): PlaceOrderResult =
        transaction {
            val deliveryStart =
                parseDeliveryDateTime(request.deliveryWindowStart)
                    ?: return@transaction PlaceOrderResult.Error("invalid_delivery_window")
            val deliveryEnd =
                parseDeliveryDateTime(request.deliveryWindowEnd)
                    ?: return@transaction PlaceOrderResult.Error("invalid_delivery_window")

            val selectedWindow =
                getDeliveryWindows().firstOrNull {
                    it.start == deliveryStart.toString() &&
                        it.end == deliveryEnd.toString() &&
                        it.available
                } ?: return@transaction PlaceOrderResult.Error("invalid_delivery_window")

            val cartRow =
                Cart
                    .selectAll()
                    .where { Cart.userId eq userId }
                    .firstOrNull()
                    ?: return@transaction PlaceOrderResult.Error("empty_basket")

            val cartId = cartRow[Cart.id]
            val cartItems =
                (CartItem innerJoin Product)
                    .selectAll()
                    .where { CartItem.cartId eq cartId }
                    .toList()

            if (cartItems.isEmpty()) {
                return@transaction PlaceOrderResult.Error("empty_basket")
            }

            val addressId =
                upsertDeliveryAddress(userId, request.address)
                    ?: return@transaction PlaceOrderResult.Error("invalid_address")

            if (request.paymentId != null) {
                if (!PaymentRepository.paymentBelongsToUser(userId, request.paymentId)) {
                    return@transaction PlaceOrderResult.Error("invalid_payment")
                }
            } else {
                val payment =
                    request.payment
                        ?: return@transaction PlaceOrderResult.Error("missing_fields")
                val paymentResult = PaymentRepository.upsertPaymentForUser(userId, payment)
                if (paymentResult != "SUCCESS") {
                    return@transaction PlaceOrderResult.Error(paymentResult)
                }
            }

            var orderTotal = 0.0f
            var itemCount = 0
            val orderLines =
                cartItems.mapNotNull { row ->
                    val quantity = row[CartItem.quantity]
                    val weight = row[CartItem.weight]
                    val soldByWeight = row[Product.soldByWeight]
                    val unitPrice = row[Product.price]
                    val lineTotal =
                        when {
                            soldByWeight && weight != null -> unitPrice * weight
                            !soldByWeight && quantity != null -> unitPrice * quantity
                            else -> return@mapNotNull null
                        }

                    orderTotal += lineTotal
                    itemCount += quantity ?: 1

                    PendingOrderLine(
                        productId = row[Product.id],
                        quantity = quantity,
                        weight = weight,
                        lineTotal = lineTotal,
                    )
                }

            if (orderLines.isEmpty()) {
                return@transaction PlaceOrderResult.Error("empty_basket")
            }

            val insertedOrder =
                Order.insert {
                    it[Order.userId] = userId
                    it[Order.deliveryAddressId] = addressId
                    it[Order.deliveryWindowStart] = deliveryStart
                    it[Order.deliveryWindowEnd] = deliveryEnd
                    it[Order.totalCost] = orderTotal
                    it[Order.orderTime] = LocalDateTime.now(ZoneId.of("Europe/London"))
                    it[Order.status] = OrderStatus.WAITING
                }

            val orderId = insertedOrder[Order.id]

            OrderItem.batchInsert(orderLines) { line ->
                this[OrderItem.productID] = line.productId
                this[OrderItem.orderId] = orderId
                this[OrderItem.priceAtOrder] = line.lineTotal
                this[OrderItem.quantity] = line.quantity
                this[OrderItem.weight] = line.weight
            }

            CartItem.deleteWhere { CartItem.cartId eq cartId }
            Cart.update({ Cart.id eq cartId }) {
                it[Cart.totalCost] = 0.0f
            }

            PlaceOrderResult.Success(
                PlaceOrderResponse(
                    orderId = orderId,
                    totalCost = orderTotal,
                    itemCount = itemCount,
                    deliveryWindow = selectedWindow.label,
                    deliveryAddress = formatAddress(request.address),
                ),
            )
        }

    private fun parseDeliveryDateTime(value: String): LocalDateTime? =
        try {
            LocalDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            null
        }

    private fun upsertDeliveryAddress(
        userId: Int,
        address: CheckoutAddressRequest,
    ): Int? {
        val line1 = address.line1.trim()
        val line2 = address.line2?.trim()?.takeIf { it.isNotEmpty() }
        val city = address.city.trim()
        val postcode = address.postcode.trim()

        if (line1.isBlank() || city.isBlank() || postcode.isBlank()) {
            return null
        }

        val existingAddress =
            Address
                .selectAll()
                .where {
                    (Address.userId eq userId) and
                        (Address.line1 eq line1) and
                        (Address.line2 eq line2) and
                        (Address.city eq city) and
                        (Address.postcode eq postcode)
                }.firstOrNull()

        if (existingAddress != null) {
            return existingAddress[Address.id]
        }

        return Address.insert {
            it[Address.userId] = userId
            it[Address.line1] = line1
            it[Address.line2] = line2
            it[Address.city] = city
            it[Address.postcode] = postcode
        }[Address.id]
    }

    private fun formatAddress(address: CheckoutAddressRequest): String =
        listOfNotNull(
            address.line1.trim(),
            address.line2?.trim()?.takeIf { it.isNotEmpty() },
            address.city.trim(),
            address.postcode.trim(),
        ).joinToString(", ")
}

sealed class PlaceOrderResult {
    data class Success(
        val response: PlaceOrderResponse,
    ) : PlaceOrderResult()

    data class Error(
        val reason: String,
    ) : PlaceOrderResult()
}

private data class PendingOrderLine(
    val productId: Int,
    val quantity: Int?,
    val weight: Float?,
    val lineTotal: Float,
)
