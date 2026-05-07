package com.supermarket.routes

import com.supermarket.controllers.CustomerAuthController
import com.supermarket.controllers.UserSession
import com.supermarket.database.AddressRepository
import com.supermarket.database.CustomerAddressUpdateRequest
import com.supermarket.database.CustomerPasswordUpdateRequest
import com.supermarket.database.CustomerPaymentUpdateRequest
import com.supermarket.database.CustomerProfileRepository
import com.supermarket.database.CustomerProfileUpdateRequest
import com.supermarket.database.PaymentRepository
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.customerRoutes() {
    route("/customers") {
        post("/register") {
            val formParameters = call.receiveParameters()

            val firstNameInput = formParameters["firstname"]
            val lastNameInput = formParameters["surname"]
            val dobInput = formParameters["date_of_birth"]
            val emailInput = formParameters["email"]
            val passwordInput = formParameters["password"]
            val phoneNumberInput = formParameters["phone"]

            // Check that all required fields are filled
            if (firstNameInput.isNullOrBlank() || lastNameInput.isNullOrBlank() ||
                dobInput.isNullOrBlank() || emailInput.isNullOrBlank() || passwordInput.isNullOrBlank()
            ) {
                call.respondRedirect("/customers/register?error=missing_fields")
                return@post
            }

            // Give info to controller
            val result =
                CustomerAuthController.registerNewUser(
                    firstNameInput,
                    lastNameInput,
                    dobInput,
                    emailInput,
                    passwordInput,
                    phoneNumberInput,
                )

            // Check what controller decided & redirect to user
            if (result == "SUCCESS") {
                // Send user to login page with success message
                call.respondRedirect("/customers/login?registered=true")
            } else {
                // If failed then send user back to registration page with error message
                call.respondRedirect("/customers/register?error=$result")
            }
        }

        post("/login") {
            val formParameters = call.receiveParameters()

            val email = formParameters["email"]
            val password = formParameters["password"]

            if (email.isNullOrBlank() || password.isNullOrBlank()) {
                call.respondRedirect("/customers/login?error=invalid_credentials")
                return@post
            }

            val result = CustomerAuthController.verifyCustomerLogin(email, password)

            // If success, set cookie if user wants to, then redirect to landing page
            if (result != null) {
                call.sessions.set(UserSession(userId = result))
                call.respondRedirect("/customers/landing")
            } else {
                // otherwise redirect back to login page
                call.respondRedirect("/customers/login?error=invalid_credentials")
            }
        }

        get("/login") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/login.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/register") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/register.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/forgotPassword") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/forgotPassword.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/forgotPasswordConf") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/forgotPasswordConf.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/landing") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/landing.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/products-listing") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/productListing.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/product-detail") {
            val html =
                call.application.javaClass
                    .getResource("/static/views/customer/productDetail.html")
                    ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        post("/basket") {
        }

        authenticate("customer-auth") {
            get("/basket") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/customer/basket.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                }
            }

            get("/checkout") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/customer/checkout.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                }
            }

            get("/profile") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/customer/profile.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                }
            }

            get("/order-history") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/customer/orderHistory.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                }
            }

            get("/edit-order") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/customer/editOrder.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                }
            }
        }

        authenticate("customer-api-auth") {
            get("/me") {
                val session = call.sessions.get<UserSession>()!!
                val profile = CustomerProfileRepository.getProfile(session.userId)

                if (profile == null) {
                    call.sessions.clear<UserSession>()
                    call.respond(HttpStatusCode.Unauthorized, "No active customer session")
                    return@get
                }

                call.respond(HttpStatusCode.OK, profile)
            }

            put("/me") {
                val session = call.sessions.get<UserSession>()!!
                val request =
                    try {
                        call.receive<CustomerProfileUpdateRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                        return@put
                    }

                when (val result = CustomerProfileRepository.updateProfile(session.userId, request)) {
                    "SUCCESS" -> {
                        call.respond(HttpStatusCode.OK, CustomerProfileRepository.getProfile(session.userId)!!)
                    }

                    "not_found" -> {
                        call.sessions.clear<UserSession>()
                        call.respond(HttpStatusCode.Unauthorized, "No active customer session")
                    }

                    else -> {
                        call.respond(HttpStatusCode.BadRequest, result)
                    }
                }
            }

            put("/me/password") {
                val session = call.sessions.get<UserSession>()!!
                val request =
                    try {
                        call.receive<CustomerPasswordUpdateRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                        return@put
                    }

                when (val result = CustomerProfileRepository.updatePassword(session.userId, request)) {
                    "SUCCESS" -> {
                        call.respond(HttpStatusCode.OK, "Password updated")
                    }

                    "not_found" -> {
                        call.sessions.clear<UserSession>()
                        call.respond(HttpStatusCode.Unauthorized, "No active customer session")
                    }

                    "invalid_current_password" -> {
                        call.respond(HttpStatusCode.Forbidden, result)
                    }

                    else -> {
                        call.respond(HttpStatusCode.BadRequest, result)
                    }
                }
            }

            get("/me/address") {
                val session = call.sessions.get<UserSession>()!!
                val address = AddressRepository.getAddress(session.userId)

                if (address == null) {
                    call.respond(HttpStatusCode.NotFound, "Address not found")
                    return@get
                }

                call.respond(HttpStatusCode.OK, address)
            }

            put("/me/address") {
                val session = call.sessions.get<UserSession>()!!
                val request =
                    try {
                        call.receive<CustomerAddressUpdateRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                        return@put
                    }

                when (val result = AddressRepository.upsertAddress(session.userId, request)) {
                    "SUCCESS" -> call.respond(HttpStatusCode.OK, AddressRepository.getAddress(session.userId)!!)
                    else -> call.respond(HttpStatusCode.BadRequest, result)
                }
            }

            get("/me/payment") {
                val session = call.sessions.get<UserSession>()!!
                val payment = PaymentRepository.getPayment(session.userId)

                if (payment == null) {
                    call.respond(HttpStatusCode.NotFound, "Payment details not found")
                    return@get
                }

                call.respond(HttpStatusCode.OK, payment)
            }

            put("/me/payment") {
                val session = call.sessions.get<UserSession>()!!
                val request =
                    try {
                        call.receive<CustomerPaymentUpdateRequest>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Invalid request body")
                        return@put
                    }

                when (val result = PaymentRepository.upsertPayment(session.userId, request)) {
                    "SUCCESS" -> call.respond(HttpStatusCode.OK, PaymentRepository.getPayment(session.userId)!!)
                    else -> call.respond(HttpStatusCode.BadRequest, result)
                }
            }
        }

        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respondRedirect("/customers/landing")
        }

        get("/session") {
            val session = call.sessions.get<UserSession>()
            if (session == null) {
                call.respond(HttpStatusCode.Unauthorized, "No active customer session")
                return@get
            }

            val customer = CustomerAuthController.getCurrentCustomer(session.userId)
            if (customer == null) {
                call.sessions.clear<UserSession>()
                call.respond(HttpStatusCode.Unauthorized, "No active customer session")
                return@get
            }

            call.respond(HttpStatusCode.OK, customer)
        }
    }
}
