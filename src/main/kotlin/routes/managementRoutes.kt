package com.supermarket.routes

import com.supermarket.controllers.ManagementAuthController
import com.supermarket.controllers.StaffSession
import com.supermarket.database.UserRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.managementRoutes() {
    route("/management") {

        get("/reports/sales") {

        }

        get("/reports/orders") {

        }

        get("/audit-log") {

        }

        route("/staff") {

            route("/create") {
                get {
                    val html = call.application.javaClass
                        .getResource("/static/views/warehouse/create.html")
                        ?.readText()

                    if (html != null) {
                        call.respondText(html, ContentType.Text.Html)
                    } else {
                        call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                    }
                }
                post {
                    // Get parameters from frontend
                    val formParameters = call.receiveParameters()

                    val firstName = formParameters["firstname"]
                    val lastName = formParameters["surname"]
                    val dob = formParameters["date_of_birth"]
                    val email = formParameters["email"]
                    val phone = formParameters["phoneNumber"]
                    val role = formParameters["role"]
                    val password = formParameters["password"]

                    // Basic null check
                    if (firstName.isNullOrBlank() || lastName.isNullOrBlank() || dob.isNullOrBlank() ||
                        email.isNullOrBlank() || role.isNullOrBlank() || password.isNullOrBlank()
                    ) {
                        call.respondRedirect("/management/staff/create?error=missing_fields")
                        return@post
                    }

                    // Send to account creation function
                    val result = ManagementAuthController.createStaffAccount(
                        firstName, lastName, dob, email, phone, password, role
                    )

                    // Handle the response
                    if (result == "email_exists" || result == "weak_password") {
                        call.respondRedirect("/management/staff/create?error=$result")
                    } else {
                        // Return the staff ID to the manager as confirmation
                        call.respondRedirect("/management/staff/create?success=$result")
                    }
                }
            }

            route("/login") {
                get {
                    val html = call.application.javaClass
                        .getResource("/static/views/warehouse/login.html")
                        ?.readText()

                    if (html != null) {
                        call.respondText(html, ContentType.Text.Html)
                    } else {
                        call.respondText("Login page not found", status = HttpStatusCode.NotFound)
                    }
                }

                post {
                    val formParameters = call.receiveParameters()
                    val staffId = formParameters["staffId"]
                    val password = formParameters["password"]

                    if (staffId.isNullOrBlank() || password.isNullOrBlank()) {
                        call.respondRedirect("/warehouse-worker/staff/login?error=missing_fields")
                        return@post
                    }

                    val result = ManagementAuthController.verifyStaffLogin(staffId, password)

                    if (result != null) {
                        val (userId, role) = result

                        call.sessions.set(StaffSession(userId = userId, role = role.name))

                        if (role == UserRole.MANAGER) {
                            call.respondRedirect("/warehouse-worker/dashboard")
                        } else {
                            // Should direct user to main picking dashboard
                            call.respondRedirect("/warehouse-worker/worker-dashboard")
                        }
                    } else {
                        // Failed login
                        call.respondRedirect("/warehouse-worker/staff/login?error=invalid_credentials")
                    }
                }
            }

            put("/{id}") {

            }

            delete("/{id}") {

            }
        }
    }
}