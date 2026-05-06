package com.supermarket.routes

import com.supermarket.controllers.ManagementAuthController
import com.supermarket.controllers.PicklistController
import com.supermarket.controllers.StaffSession
import com.supermarket.database.DeleteUserResult
import com.supermarket.database.ManagementAnalyticsRepository
import com.supermarket.database.ManagementUserRepository
import com.supermarket.database.ManagerDashboardFilters
import com.supermarket.database.UserRole
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class PicklistGenerationRequest(
    val date: String,
)

@Serializable
data class PicklistGenerationResponse(
    val date: String,
    val picklistsCreated: Int,
)

@Serializable
data class DeleteUserResponse(
    val message: String,
)

fun Route.managementRoutes() {
    route("/management") {
        // Public route (login page)    }
        route("/login") {
            get {
                val html =
                    call.application.javaClass
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
                    call.respondRedirect("/management/login?error=missing_fields")
                    return@post
                }

                val result = ManagementAuthController.verifyStaffLogin(staffId, password)

                if (result != null) {
                    val (userId, role) = result

                    call.sessions.set(StaffSession(userId = userId, role = role.name))

                    if (role == UserRole.MANAGER) {
                        call.respondRedirect("/management/dashboard")
                    } else if (role == UserRole.ANALYST) {
                        call.respondRedirect("/management/dashboard")
                    } else {
                        // Should direct user to main picking dashboard
                        call.respondRedirect("/warehouse/dashboard")
                    }
                } else {
                    // Failed login
                    call.respondRedirect("/management/login?error=invalid_credentials")
                }
            }
        }

        route("/logout") {
            post {
                // clears the Staff Session cookie
                call.sessions.clear<StaffSession>()

                // Redirects the user back to the public storefront
                call.respondRedirect("/")
            }
        }

        get("/reports/sales") {
        }

        get("/reports/orders") {
        }

        get("/audit-log") {
        }

        authenticate("manager-auth") {
            get("/dashboard") {
                val html =
                    call.application.javaClass
                        .getResource("/static/views/management/dashboard.html")
                        ?.readText()

                if (html != null) {
                    call.respondText(html, ContentType.Text.Html)
                } else {
                    call.respondText("Manager dashboard page not found", status = HttpStatusCode.NotFound)
                }
            }

            get("/api/dashboard") {
                val query = call.request.queryParameters
                val filters =
                    ManagerDashboardFilters(
                        dateFrom = query["dateFrom"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        dateTo = query["dateTo"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        category = query["category"],
                        search = query["search"],
                    )

                call.respond(ManagementAnalyticsRepository.getDashboard(filters))
            }

            post("/api/picklists/generate") {
                val request = call.receive<PicklistGenerationRequest>()
                val targetDate =
                    runCatching { LocalDate.parse(request.date) }.getOrNull()
                        ?: return@post call.respondText(
                            "Invalid date. Use yyyy-MM-dd.",
                            status = HttpStatusCode.BadRequest,
                        )

                val created = PicklistController.generatePicklists(targetDate)
                call.respond(PicklistGenerationResponse(date = targetDate.toString(), picklistsCreated = created))
            }

            delete("/api/users/{id}") {
                val targetUserId =
                    call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respondText(
                            "Invalid user id.",
                            status = HttpStatusCode.BadRequest,
                        )
                val session =
                    call.sessions.get<StaffSession>()
                        ?: return@delete call.respondText(
                            "Unauthorized",
                            status = HttpStatusCode.Unauthorized,
                        )

                when (ManagementUserRepository.deleteUser(targetUserId, session.userId)) {
                    DeleteUserResult.DELETED -> call.respond(DeleteUserResponse("User account deleted."))
                    DeleteUserResult.NOT_FOUND ->
                        call.respondText(
                            "User account not found.",
                            status = HttpStatusCode.NotFound,
                        )
                    DeleteUserResult.SELF_DELETE ->
                        call.respondText(
                            "You cannot delete your own manager account while logged in.",
                            status = HttpStatusCode.BadRequest,
                        )
                    DeleteUserResult.LAST_MANAGER ->
                        call.respondText(
                            "At least one manager account must remain.",
                            status = HttpStatusCode.BadRequest,
                        )
                }
            }

            route("/staff") {
                route("/create") {
                    get {
                        val html =
                            call.application.javaClass
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
                        val result =
                            ManagementAuthController.createStaffAccount(
                                firstName,
                                lastName,
                                dob,
                                email,
                                phone,
                                password,
                                role,
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

                put("/{id}") {
                }

                delete("/{id}") {
                }
            }
        }
    }
}
