package com.supermarket.routes

import com.supermarket.controllers.CustomerAuthController
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File


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
                dobInput.isNullOrBlank() || emailInput.isNullOrBlank() || passwordInput.isNullOrBlank())
            {
                call.respondRedirect("/customers/register?error=missing_fields")
                return@post
            }

            // Give info to controller
            val result = CustomerAuthController.registerNewUser(
                firstNameInput,
                lastNameInput,
                dobInput,
                emailInput,
                passwordInput,
                phoneNumberInput
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

            if (result != null) {
                // success
                call.respondRedirect("/customers/landing")
            }
            else {
                call.respondRedirect("/customers/login?error=missing_fields")
            }
        }

        get("/login") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/login.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/register") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/register.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/forgotPassword") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/forgotPassword.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/forgotPasswordConf") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/forgotPasswordConf.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/landing") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/landing.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/products-listing") {
            val html = call.application.javaClass
                .getResource("/static/views/customer/product_listing.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }



        post("/logout") {
            // logoutCustomer
        }

        get("/session") {
            // validateSession
        }

        get {
            // getAllCustomers
        }

        get("/{id}") {

        }

        put("/{id}") {

        }

        put("/{id}/password") {

        }

        delete("/{id}") {

        }
    }
}