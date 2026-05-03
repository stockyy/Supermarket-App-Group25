package com.supermarket.routes

import com.supermarket.controllers.UserSession
import com.supermarket.database.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*


fun Route.orderRoutes() {
    route("/orders") {

        route("/basket") {

            get {
                // logged in session cookie
                val session = call.sessions.get<UserSession>()

                // user isn't logged in if there's no session
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be logged in to view your basket")
                    return@get
                }

                // fetch basket and send it as json
                val basket = CartRepository.getBasketForUser(session.userId)
                call.respond(HttpStatusCode.OK, basket)
            }

            post {
                // make sure users looged in
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be logged in to add to basket")
                    return@post
                }

                // read json from frontend
                val request = try {
                    call.receive<AddToBasketRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                    return@post
                }

                // qaunt has to be atleast 1
                if (request.quantity < 1) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be at least 1")
                    return@post
                }

                // add item
                val success = CartRepository.addItemToBasket(session.userId, request.productId, request.quantity)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Item added to basket")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to add item to basket")
                }
            }

            put("/{itemId}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be logged in")
                    return@put
                }

                // get item from the url
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                if (itemId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid item ID")
                    return@put
                }

                // get the json body
                val request = try {
                    call.receive<UpdateQuantityRequest>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                    return@put
                }

                // ensures valid quantity
                if (request.quantity < 1) {
                    call.respond(HttpStatusCode.BadRequest, "Quantity must be at least 1.")
                    return@put
                }

                // Try to update
                val success = CartRepository.updateCartItemQuantity(session.userId, itemId, request.quantity)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Quantity updated")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Cart item not found or doesn't belong to you")
                }
            }

            delete("/{itemId}") {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be logged in")
                    return@delete
                }

                // get item from the url
                val itemId = call.parameters["itemId"]?.toIntOrNull()
                if (itemId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid item ID")
                    return@delete
                }

                // try to remove the item
                val success = CartRepository.removeCartItem(session.userId, itemId)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Item removed from basket")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Cart item not found or doesn't belong to you")
                }
            }

            delete {
                val session = call.sessions.get<UserSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "You must be logged in")
                    return@delete
                }

                val success = CartRepository.clearBasket(session.userId)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Basket cleared")
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to clear basket")
                }
            }
        }


        post {

        }

        get {

        }

        get("/delivery-windows") {

        }

        get("/{id}") {

        }

        put("/{id}/status") {

        }

        put("/{id}/cancel") {

        }

        post("/{id}/substitutions") {

        }

        get("/{id}/substitutions") {

        }

        put("/{id}/substitutions/{subId}") {

        }
    }
}