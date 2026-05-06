package com.supermarket.routes

import com.supermarket.controllers.*
import com.supermarket.database.*
import io.ktor.http.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.*
import java.time.LocalDate

fun Route.testingRoutes() {
    // Only verified employees can view supermarket data
    authenticate("worker-auth", "manager-auth") {
        route("/api") {
            get("/available-picklists") {
                // get available picklist counts
                val counts = PicklistController.getAvailablePicklistCounts()

                // Manually format the map into a JSON string
                val jsonResponse =
                    counts.entries.joinToString(prefix = "{", postfix = "}") {
                        "\"${it.key}\": ${it.value}"
                    }
                // Give to frontend
                call.respondText(jsonResponse, ContentType.Application.Json)
            }

            post("/claim-picklist") {
                // get workerId from session cookie
                val session =
                    call.sessions.get<StaffSession>()
                        ?: return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                val workerId = session.userId

                // Get section for picklist
                val params = call.receiveParameters()
                val section = params["section"] ?: return@post call.respond(HttpStatusCode.BadRequest)

                // Capture the Pair result
                val result = PicklistController.claimPicklist(workerId, section)

                // If claim was successful, return id to frontend
                if (result != null) {
                    val picklistId = result.first
                    val cratesNeeded = result.second
                    call.respondText(
                        """{"picklistId": $picklistId, "cratesNeeded": $cratesNeeded}""",
                        ContentType.Application.Json,
                    )
                } else {
                    // otherwise flag that there wasn't a list available.
                    call.respondText("No lists available", status = HttpStatusCode.NotFound)
                }
            }

            post("/unclaim-picklist") {
                // get workerId from session cookie
                val session =
                    call.sessions.get<StaffSession>()
                        ?: return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                val workerId = session.userId

                // Get picklist Id to give to backend to free up
                val params = call.receiveParameters()
                val picklistId =
                    params["picklistId"]?.toIntOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)

                PicklistController.unclaimPicklist(picklistId, workerId)
                call.respond(HttpStatusCode.OK)
            }

            get("/next-pick-item") {
                // Get the picklistId from the URL query parameters
                val picklistId = call.request.queryParameters["picklistId"]?.toIntOrNull()

                if (picklistId == null) {
                    call.respondText("Missing picklistId", status = HttpStatusCode.BadRequest)
                    return@get
                }

                // Get next pickItem
                val nextItem = PicklistController.getNextItemToPick(picklistId)

                // If there are items left to pick, send the product data to the screen
                if (nextItem != null) {
                    call.respond(nextItem)
                } else {
                    // If no items left to pick then let frontend know
                    call.respondText("LIST_FINISHED", status = HttpStatusCode.OK)
                }
            }

            post("/bind-crates") {
                // get workerId from session cookie
                val session =
                    call.sessions.get<StaffSession>()
                        ?: return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)

                // Receive the crate data
                val request = call.receive<BindCratesRequest>()

                // Pass the crate data to the controller
                val error = PicklistController.bindCrates(request.picklistId, request.barcodes)

                if (error != null) {
                    // If the controller returned an error string, send it back as a bad request
                    call.respondText(error, status = HttpStatusCode.BadRequest)
                } else {
                    call.respond(HttpStatusCode.OK)
                }
            }

            post("/confirm-pick") {
                // Get the JSON data
                val request = call.receive<ConfirmPickRequest>()

                // Save it to the database
                val success = PicklistController.confirmPickItem(request.pickItemId, request.qtyPicked)

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respondText("Failed to update item", status = HttpStatusCode.BadRequest)
                }
            }

            get("/get-crate-barcode") {
                // Grab the crate ID from the URL
                val crateId = call.request.queryParameters["crateId"]?.toIntOrNull()

                if (crateId == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                // Look up the barcode
                val barcode = PicklistController.getCrateBarcode(crateId)

                if (barcode != null) {
                    call.respondText(barcode) // Send the barcode string back
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/auto-pick-all") {
                val request = call.receive<AutoPickRequest>()

                val success = PicklistController.autoPickEntireList(request.picklistId)

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respondText("Failed to auto pick", status = HttpStatusCode.InternalServerError)
                }
            }

            get("/putaway-details") {
                val picklistId = call.request.queryParameters["picklistId"]?.toIntOrNull()

                if (picklistId == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val details = PicklistController.getPutawayDetails(picklistId)
                call.respond(details)
            }

            get("/substitute-details") {
                val pickItemId = call.request.queryParameters["pickItemId"]?.toIntOrNull()

                if (pickItemId == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val detailsList = PicklistController.getSubstituteDetails(pickItemId)

                if (detailsList.isNotEmpty()) {
                    call.respond(detailsList)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            post("/confirm-substitution") {
                val request = call.receive<ConfirmSubstitutionRequest>()
                val success =
                    PicklistController.applyAndConfirmSubstitution(
                        request.pickItemId,
                        request.substituteProductId,
                        request.qtyPicked,
                    )

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/report-offsale") {
                // Get workerId from session
                val session = call.sessions.get<StaffSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receive<OffsaleRequest>()
                val success = PicklistController.reportOffsale(request.pickItemId, session.userId)

                if (success) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            get("/product") {
                val id = call.request.queryParameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Missing product ID")
                    return@get
                }

                val product = ProductRepository.getProductById(id)
                if (product != null) {
                    call.respond(product)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Product not found")
                }
            }

            post("/log-offsale-direct") {
                val session = call.sessions.get<StaffSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receive<ProductOffsaleRequest>()
                val success = ProductRepository.createOffsaleLog(request.productId, session.userId)

                if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.InternalServerError)
            }

            post("/log-wastage") {
                val session = call.sessions.get<StaffSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receive<WastageLogRequest>()
                val reason = WasteReasons.valueOf(request.wasteReason)
                val success =
                    ProductRepository.createWastageLog(request.productId, session.userId, reason, request.quantity)

                if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.InternalServerError)
            }

            post("/update-stock") {
                val session = call.sessions.get<StaffSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized)
                    return@post
                }

                val request = call.receive<StockUpdateRequest>()
                val success = ProductRepository.updateProductQuantity(request.productId, request.newQuantity)

                if (success) call.respond(HttpStatusCode.OK) else call.respond(HttpStatusCode.InternalServerError)
            }

            get("/worker-profile") {
                val session = call.sessions.get<StaffSession>()
                if (session == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Unauthorized")
                    return@get
                }

                val profile = PicklistController.getWorkerProfile(session.userId)
                if (profile != null) {
                    call.respond(profile)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Worker profile not found")
                }
            }
        }
        put("/seed-db") {
            try {
                refreshDatabase()
                call.respondText(
                    "SUCCESS: Database wiped and re-seeded with data from the productData JSON file",
                    status = HttpStatusCode.OK,
                )
            } catch (e: Exception) {
                call.respondText("JSON seeding failed: ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }

        get("/verify-database") {
            val testData =
                transaction {
                    // Fetch the first 10 products and format them into a readable string
                    Product.selectAll().limit(10).map {
                        "Name: ${it[Product.name]} | Price: £${it[Product.price]} | Location: ${it[Product.location]}"
                    }
                }

            if (testData.isEmpty()) {
                call.respondText("The database is empty! Seeding must have failed.")
            } else {
                // This will print the list directly to your web browser
                call.respond(testData)
            }
        }

        get("/print-all-users") {
            val userText = StringRepository.getAllUsersString()
            call.respondText(userText, ContentType.Text.Html)
        }

        get("/print-all-orders") {
            val orderText = StringRepository.getAllOrdersString()
            call.respondText(orderText, ContentType.Text.Html)
        }

        get("/print-all-carts") {
            val cartText = StringRepository.getAllCartsString()
            call.respondText(cartText, ContentType.Text.Html)
        }

        get("/view-all-products") {
            val productHtml = HtmlRepository.getAllProductsHtml()
            call.respondText(productHtml, ContentType.Text.Html)
        }

        get("/picklist-testing") {
            val picklist = PicklistController.generatePicklists(LocalDate.now())
            call.respondText("Total picklists generated: $picklist")
        }

        get("/generate-default-picklists") {
            val picklistCount = PicklistController.generatePicklists()
            call.respondText("Total picklists generated (default): $picklistCount")
        }

        get("/print-all-picklists") {
            val picklistText = StringRepository.getAllPickListsString()
            call.respondText(picklistText, ContentType.Text.Html)
        }
    }
}
