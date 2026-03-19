package com.supermarket

import com.supermarket.database.OffsaleLog.potentialOffsale
import com.supermarket.database.OffsaleSummary
import com.supermarket.database.Product
import com.supermarket.database.ProductRepository
import com.supermarket.database.StringRepository
import com.supermarket.database.UserRepository
import com.supermarket.database.WasteReasons
import com.supermarket.database.refreshDatabase
import com.supermarket.database.seedDatabaseIfNeeded
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.update

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText(
                """
    <html>
        <head>
            <title>Supermarket API Admin</title>
            <style>
                body { font-family: sans-serif; line-height: 1.6; padding: 20px; max-width: 800px; }
                h2 { color: #2c3e50; border-bottom: 2px solid #eee; padding-bottom: 5px; }
                .card { background: #f9f9f9; padding: 15px; margin-bottom: 15px; border-radius: 8px; border: 1px solid #ddd; }
                input[type="number"] { width: 80px; padding: 5px; }
                button { padding: 6px 12px; background: #3498db; color: white; border: none; border-radius: 4px; cursor: pointer; }
                button:hover { background: #2980b9; }
                .btn-danger { background: #e74c3c; }
                .btn-danger:hover { background: #c0392b; }
                a { text-decoration: none; color: #3498db; font-weight: bold; }
            </style>
            <script>
                function testOffsale() {
                    let id = document.getElementById('off-id').value || 1;
                    let pot = document.getElementById('off-pot').checked;
                    let rev = document.getElementById('off-rev').checked;
                    window.location.href = `/offsale/${'$'}{id}?potential=${'$'}{pot}&reviewed=${'$'}{rev}`;
                }
                function testWastage() {
                    let id = document.getElementById('waste-id').value || 1;
                    let qty = document.getElementById('waste-qty').value || 1;
                    window.location.href = `/wastage/${'$'}{id}?qty=${'$'}{qty}`;
                }
                
                // NEW: Javascript to test the PUT request for Stock Updates
                async function testStock() {
                    let id = document.getElementById('stock-id').value || 1;
                    let qty = document.getElementById('stock-qty').value || 0;
                    
                    const response = await fetch(`/update-stock/${'$'}{id}?qty=${'$'}{qty}`, { method: 'PUT' });
                    const text = await response.text();
                    alert(text);
                }

                async function testReseed() {
                    if(confirm("Are you sure you want to wipe and re-seed the database?")) {
                        const response = await fetch('/re-seed-db', { method: 'PUT' });
                        const text = await response.text();
                        alert(text);
                    }
                }
            </script>
        </head>
        <body>
            <h2>Admin Quick Links</h2>
            <div class="card">
                <a href="/print-all-products">📦 View All Products</a> | 
                <a href="/print-all-workers">👷 View All Workers</a> | 
                <a href="/print-offsale-logs">📜 View Offsale Logs</a> | 
                <a href="/print-wastage-logs">🗑️ View Wastage Logs</a>
            </div>

            <h2>Interactive Testers</h2>
            
            <div class="card">
                <b>⚠️ Test Offsale Route</b><br/>
                Product ID: <input type="number" id="off-id" value="1" />
                <label><input type="checkbox" id="off-pot"> Potential?</label>
                <label><input type="checkbox" id="off-rev"> Reviewed?</label>
                <button onclick="testOffsale()">Run Offsale Test</button>
            </div>

            <div class="card">
                <b>🗑️ Test Wastage Route</b><br/>
                Product ID: <input type="number" id="waste-id" value="1" />
                Quantity: <input type="number" id="waste-qty" value="1" />
                <button onclick="testWastage()">Run Wastage Test</button>
            </div>
            
            <div class="card">
                <b>📦 Change Stock Level</b><br/>
                Product ID: <input type="number" id="stock-id" value="1" />
                New Stock Level: <input type="number" id="stock-qty" value="50" />
                <button onclick="testStock()">Update Stock</button>
            </div>
            
            <div class="card">
                <b>🔄 Database Management</b><br/><br/>
                <button class="btn-danger" onclick="testReseed()">Force Re-Seed Database</button>
            </div>
        </body>
    </html>
    """.trimIndent(),
                ContentType.Text.Html
            )
        }

        put("/re-seed-db") {
            try {
                refreshDatabase()
                call.respondText("SUCCESS: Database wiped, rebuilt, and re-seeded with fresh data", status = HttpStatusCode.OK)
            } catch (e: Exception) {
                call.respondText("ERROR: ${e.message}", status = HttpStatusCode.InternalServerError)
            }
        }

        productRouting()
        userRouting()
    }
}

fun Route.productRouting() {
        route("/print-all-products") {
            get {
                val productText = ProductRepository.getAllProductsString()
                call.respondText(productText, ContentType.Text.Plain)
            }
        }

        route("/product/{id}") {
            get {
                val productId = call.parameters["id"]?.toIntOrNull()

                if (productId == null) {
                    call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                    return@get
                }

                val product = ProductRepository.getProductById(productId)

                if (product != null) {
                    call.respond(HttpStatusCode.OK, product)
                } else {
                    call.respondText("Product not found!", status = HttpStatusCode.NotFound)
                }
            }
        }

        route("/update-stock/{id}") {
            put {
                // Get id & Quantity
                val productId = call.parameters["id"]?.toIntOrNull()
                val quantity = call.request.queryParameters["qty"]?.toIntOrNull()

                // Check for valid data
                if (quantity == null) {
                    call.respondText("Invalid Quantity", status = HttpStatusCode.BadRequest)
                    return@put
                }
                if (productId == null) {
                    call.respondText("Invalid Stock ID format", status = HttpStatusCode.BadRequest)
                } else {
                    val update = ProductRepository.updateProductQuantity(productId, quantity)

                    if (update != null) {
                        call.respondText("Stock updated to $quantity successfully!", status = HttpStatusCode.OK)
                    } else {
                        call.respondText("Database update failed (Product ID might not exist)", status = HttpStatusCode.NotFound)
                    }
                }
            }
        }

        // Random userId
        // Potential Offsale & manager Reviewed done via flags
        route("/offsale/{id}") {
            // SHOULD BE PUT BECAUSE WE ARE MODIFYING DATA
            // GET FOR TESTING
            get {
                // Grab product ID
                val productId = call.parameters["id"]?.toIntOrNull()

                // Optional flag for potential offsales, if not supplied that it is false
                //TO us: .../offsale/id?potential=true
                val potential = call.request.queryParameters["potential"]?.toBoolean() ?: false

                // Optional flag for manager reviewed, if not supplied the default is false
                // To use: .../offsale/id?reviewed=true
                val reviewed = call.request.queryParameters["reviewed"]?.toBoolean() ?: false

                // Check that product id was an int
                if (productId == null) {
                    call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                    return@get
                }

                // Get product details
                val productBefore = ProductRepository.getProductById(productId)
                // Verify product exists (id is valid)
                if (productBefore == null) {
                    call.respond(HttpStatusCode.OK, "Product not found!")
                    return@get
                }

                // Arbitrary UserId until login system is working
                val success = ProductRepository.createOffsaleLog(
                    productId,
                    userId = 6,
                    potentialOffsale = potential,
                    managerReview = reviewed
                )

                if (!success) {
                    call.respond("Failed to update database")
                    return@get
                } else {
                    // Get new product information
                    val productAfter = ProductRepository.getProductById(productId)
                    // Print out offsale summary
                    val summary = OffsaleSummary(
                        productName = productBefore.name,
                        quantityBefore = productBefore.stockLevel,
                        quantityAfter = productAfter?.stockLevel
                            ?: 67676767, //Only get stock level if not null, else 67676767
                        potentialOffsale = potential,
                        status = "Successfully marked offsale"
                    )
                    call.respond(HttpStatusCode.OK, summary)
                    return@get
                }
            }
        }

        route("/print-offsale-logs") {
            get {
                val offsaleLogText = StringRepository.getAllOffsaleLogsString()
                call.respondText(offsaleLogText, ContentType.Text.Plain)
            }
        }

        // random userId & Waste reason
        // Waste is passed in as a query paramater "/wastage/id?qty=3
        route("/wastage/{id}") {
            get {
                // Get & Verify product ID is int
                val productId = call.parameters["id"]?.toIntOrNull()
                if (productId == null) {
                    call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                    return@get
                } else {
                    // Verify product Id & check if weighted or not
                    var weighted = false
                    val product = ProductRepository.getProductById(productId)
                    if (product != null) {
                        if (product.soldByWeight == false) {
                            weighted = false
                        } else {
                            weighted = true
                        }
                    }

                    // get quantity - should be different if product is wieghted or not, yet to implement
                    if (weighted) {
                        // Get quantity
                        val qtyWasted = call.request.queryParameters["qty"]?.toIntOrNull() ?: 1 // should be toFloatOrNull
                        // Add to database
                        val success = ProductRepository.createWastageLog(
                            productId,
                            8,
                            WasteReasons.entries.random(),
                            quantity = qtyWasted
                        )
                        if (!success) {
                            call.respondText { "Database interaction failed" }
                            return@get
                        } else {
                            call.respond(HttpStatusCode.OK, success)
                        }
                    } else {
                        // Get Quantity
                        val qtyWasted = call.request.queryParameters["qty"]?.toIntOrNull() ?: 1
                        // Add to database
                        val success = ProductRepository.createWastageLog(
                            productId,
                            8,
                            WasteReasons.entries.random(),
                            quantity = qtyWasted
                        )
                        if (!success) {
                            call.respondText { "Database interaction failed" }
                            return@get
                        } else {
                            call.respond(HttpStatusCode.OK, success)
                        }
                    }
                }
            }
        }

        route("/print-wastage-logs") {
            get {
                val wastageLogText = StringRepository.getALlWastageLogsString()
                call.respondText(wastageLogText, ContentType.Text.Plain)
            }
        }

    }

fun Route.userRouting() {
        route("/print-all-workers") {
            get {
                val userText = StringRepository.getAllWorkersString()
                call.respondText(userText, ContentType.Text.Plain)
            }
        }
    }

