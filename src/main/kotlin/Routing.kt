package com.supermarket

import com.supermarket.database.*
import com.supermarket.routes.*
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


        customerRoutes()
        productRoutes()
        orderRoutes()
        stockRoutes()
        warehouseRoutes()
        managementRoutes()
        userRoutes()
        testingRoutes()
        // Skeleton set up
    }
}


