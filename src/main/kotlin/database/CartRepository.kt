package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import io.ktor.server.request.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere

@Serializable
data class CartItemResponse(
    val cartItemId: Int,
    val productId: Int,
    val name: String,
    val imageUrl: String,
    val unitPrice: Float,
    val quantity: Int?,
    val weight: Float?,
    val soldByWeight: Boolean,
    val lineTotal: Float
)

@Serializable
data class BasketResponse(
    val items: List<CartItemResponse>,
    val totalCost: Float,
    val itemCount: Int
)

@Serializable
data class AddToBasketRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class UpdateQuantityRequest(
    val quantity: Int
)


object CartRepository {

    fun getBasketForUser(userId: Int): BasketResponse {
        return transaction {
            // Find useres cart row
            val cartRow = Cart.selectAll().where { Cart.userId eq userId }.firstOrNull()

            // if they don't have one make them one
            val cartId: Int
            if (cartRow == null) {
                val insertResult = Cart.insert {
                    it[Cart.userId] = userId
                    it[Cart.totalCost] = 0.0f
                }
                cartId = insertResult[Cart.id]
            } else {
                cartId = cartRow[Cart.id]
            }

            // get all cart items for this card
            val cartItemRows = CartItem.selectAll()
                .where { CartItem.cartId eq cartId }
                .toList()

            // make this list object to send back
            val itemsList = mutableListOf<CartItemResponse>()
            var runningTotal = 0.0f
            var runningItemCount = 0

            for (cartItemRow in cartItemRows) {
                // finding product info per product in the cart
                val productId = cartItemRow[CartItem.productId]
                val productRow = Product.selectAll()
                    .where { Product.id eq productId }
                    .single()

                val unitPrice = productRow[Product.price]
                val quantity = cartItemRow[CartItem.quantity]
                val weight = cartItemRow[CartItem.weight]
                val soldByWeight = productRow[Product.soldByWeight]

                // getting the total cost
                var lineTotal = 0.0f
                if (soldByWeight && weight != null) {
                    lineTotal = unitPrice * weight
                } else if (!soldByWeight && quantity != null) {
                    lineTotal = unitPrice * quantity
                }

                // getting image photo iff not there's a fall back
                var imageUrl = productRow[Product.imageUrl]
                if (imageUrl == null) {
                    imageUrl = ""
                }

                // response object
                val itemResponse = CartItemResponse(
                    cartItemId = cartItemRow[CartItem.id],
                    productId = productRow[Product.id],
                    name = productRow[Product.name],
                    imageUrl = imageUrl,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    weight = weight,
                    soldByWeight = soldByWeight,
                    lineTotal = lineTotal
                )

                itemsList.add(itemResponse)

                runningTotal += lineTotal

                // counting the quantity for items
                if (quantity != null) {
                    runningItemCount += quantity
                } else {
                    runningItemCount += 1
                }
            }

            // final response
            return@transaction BasketResponse(
                items = itemsList,
                totalCost = runningTotal,
                itemCount = runningItemCount
            )
        }
    }

    fun addItemToBasket(userId: Int, productId: Int, quantity: Int): Boolean {
        return transaction {
            //same as the previous function
            val cartRow = Cart.selectAll().where { Cart.userId eq userId }.firstOrNull()

            val cartId: Int
            if (cartRow == null) {
                val insertResult = Cart.insert {
                    it[Cart.userId] = userId
                    it[Cart.totalCost] = 0.0f
                }
                cartId = insertResult[Cart.id]
            } else {
                cartId = cartRow[Cart.id]
            }

            // look up the product
            val productRow = Product.selectAll()
                .where { Product.id eq productId }
                .firstOrNull()

            // if it doesn't exist then it fails
            if (productRow == null) {
                return@transaction false
            }

            val soldByWeight = productRow[Product.soldByWeight]

            // this is because we don't actually have the right routes to support this
            if (soldByWeight) {
                println("Cannot add weight-sold product to basket via simple add button")
                return@transaction false
            }

            // check if product already in cart
            val existingCartItem = CartItem.selectAll()
                .where { (CartItem.cartId eq cartId) and (CartItem.productId eq productId) }
                .firstOrNull()

            if (existingCartItem != null) {
                // if in teh cart increase the quantity
                val currentQuantity = existingCartItem[CartItem.quantity] ?: 0
                val newQuantity = currentQuantity + quantity

                CartItem.update({ CartItem.id eq existingCartItem[CartItem.id] }) {
                    it[CartItem.quantity] = newQuantity
                }
            } else {
                // add to cart if it's not already tehre
                CartItem.insert {
                    it[CartItem.cartId] = cartId
                    it[CartItem.productId] = productId
                    it[CartItem.quantity] = quantity
                }
            }

            recalculateCartTotal(cartId)

            return@transaction true
        }
    }

    // recalculates teh cart total once you've added an item
    private fun recalculateCartTotal(cartId: Int) {
        // Get all cart items
        val cartItemRows = CartItem.selectAll()
            .where { CartItem.cartId eq cartId }
            .toList()

        var newTotal = 0.0f

        // adding every item together
        for (cartItemRow in cartItemRows) {
            val productId = cartItemRow[CartItem.productId]
            val productRow = Product.selectAll()
                .where { Product.id eq productId }
                .single()

            val unitPrice = productRow[Product.price]
            val soldByWeight = productRow[Product.soldByWeight]
            val quantity = cartItemRow[CartItem.quantity]
            val weight = cartItemRow[CartItem.weight]

            if (soldByWeight && weight != null) {
                newTotal += unitPrice * weight
            } else if (!soldByWeight && quantity != null) {
                newTotal += unitPrice * quantity
            }
        }

        // update cart's cot field
        Cart.update({ Cart.id eq cartId }) {
            it[Cart.totalCost] = newTotal
        }
    }

    fun updateCartItemQuantity(userId: Int, cartItemId: Int, newQuantity: Int): Boolean {
        return transaction {
            // get teh cart item
            val cartItemRow = CartItem.selectAll()
                .where { CartItem.id eq cartItemId }
                .firstOrNull()

            if (cartItemRow == null) {
                // Cart item doesn't exist
                return@transaction false
            }

            // cart should be assigned to teh proper user
            val cartId = cartItemRow[CartItem.cartId]
            val cartRow = Cart.selectAll()
                .where { Cart.id eq cartId }
                .single()

            if (cartRow[Cart.userId] != userId) {
                // if cart doesn't belong to the user block them
                println("User $userId tried to update cart item $cartItemId erroneously")
                return@transaction false
            }

            // update the quantity
            CartItem.update({ CartItem.id eq cartItemId }) {
                it[CartItem.quantity] = newQuantity
            }

            recalculateCartTotal(cartId)

            return@transaction true
        }
    }

    fun removeCartItem(userId: Int, cartItemId: Int): Boolean {
        return transaction {
            val cartItemRow = CartItem.selectAll()
                .where { CartItem.id eq cartItemId }
                .firstOrNull()

            if (cartItemRow == null) {
                return@transaction false
            }

            // same security check as before
            val cartId = cartItemRow[CartItem.cartId]
            val cartRow = Cart.selectAll()
                .where { Cart.id eq cartId }
                .single()

            if (cartRow[Cart.userId] != userId) {
                println("User $userId tried to delete cart item $cartItemId that doesn't belong to them")
                return@transaction false
            }

            // delete cart item
            CartItem.deleteWhere { CartItem.id eq cartItemId }

            recalculateCartTotal(cartId)

            return@transaction true
        }
    }

    fun clearBasket(userId: Int): Boolean {
        return transaction {
            val cartRow = Cart.selectAll()
                .where { Cart.userId eq userId }
                .firstOrNull()

            if (cartRow == null) {
                // if nothing to clear then return that it's cleared
                return@transaction true
            }

            val cartId = cartRow[Cart.id]

            // delete all cart item that belongs to this
            CartItem.deleteWhere { CartItem.cartId eq cartId }

            // cart total back to 0
            Cart.update({ Cart.id eq cartId }) {
                it[Cart.totalCost] = 0.0f
            }

            return@transaction true
        }
    }
}