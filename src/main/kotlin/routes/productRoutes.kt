package com.supermarket.routes

import com.supermarket.database.OffsaleSummary
import com.supermarket.database.ProductRepository
import com.supermarket.database.ProductRequest
import com.supermarket.database.StringRepository
import com.supermarket.database.WasteReasons
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.productRoutes() {

    // PRODUCT CRUD ENDPOINTS

    route("/products") {

        // GET /products/getAll
        get("/getAll") {
            val products = ProductRepository.getAllProducts()
            call.respond(HttpStatusCode.OK, products)
        }

        // GET /products/categories
        get("/categories") {
            val categories = ProductRepository.getAllCategories()
            call.respond(HttpStatusCode.OK, categories)
        }

        // GET /products/search
        // Search like this: http://localhost:8080/products/search?name=bread
        get("/search") {
            val searchProduct = call.request.queryParameters["name"]

            if (searchProduct.isNullOrBlank()) {
                call.respondText("Please enter a product to seasrch", status = HttpStatusCode.BadRequest)
                return@get
            }

            val results = ProductRepository.searchProductsByName(searchProduct)
            call.respond(HttpStatusCode.OK, results)
        }

        // GET /products/category/{category}
        get("/category/{category}") {
            val categoryName = call.parameters["category"]

            if (categoryName.isNullOrBlank()) {
                call.respondText("Category name missing cuh...", status = HttpStatusCode.BadRequest)
                return@get
            }

            val products = ProductRepository.getProductsByCategory(categoryName)
            call.respond(HttpStatusCode.OK, products)

            if (products.isEmpty()) {
                call.respondText("No products found for category: $categoryName", status = HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, products)
            }
        }
        // GET /products/sections
        get("/sections") {
        }

        // GET /products/sections/{section}
        get("/sections/{section}") {
        }

        // GET /products/promos
        get("/promos") {
        }

        // GET /products/barcode/{barcode}
        get("/barcode/{barcode}") {
        }

        // GET /products/{id}
        get("/{id}") {
            val productId = call.parameters["id"]?.toIntOrNull()

            if (productId == null) {
                call.respondText("Invalid product ID", status = HttpStatusCode.BadRequest)
                return@get
            }

            val product = ProductRepository.getProductById(productId)

            if (product != null) {
                call.respond(HttpStatusCode.OK, product)
            } else {
                call.respondText("Product not found", status = HttpStatusCode.NotFound)
            }
        }

        // POST /products
        post {
        }

        // PUT /products/{id}
        put("/{id}") {
            val productId = call.parameters["id"]?.toIntOrNull()

            if (productId == null) {
                call.respondText("Invalid product ID", status = HttpStatusCode.BadRequest)
                return@put
            }

            try {
                val request = call.receive<ProductRequest>()
                val updated = ProductRepository.updateProduct(productId!!, request)

                if (updated) {
                    call.respondText("Product $productId updated successfully", status = HttpStatusCode.OK)
                } else {
                    call.respondText("Product not found", status = HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                call.respondText("Invalid request body : ${e.message}", status = HttpStatusCode.BadRequest)
            }
        }

        // DELETE /products/{id}
        delete("/{id}") {
        }
    }

    // PRODUCT QUERY ENDPOINTS

    // GET /print-all-products - returns all products as plain text
    route("/print-all-products") {
        get {
            val productText = ProductRepository.getAllProductsString()
            call.respondText(productText, ContentType.Text.Html)
        }
    }

    // GET /product/{id} - returns a single product by ID
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

    // STOCK MANAGEMENT ENDPOINTS

    // PUT /update-stock/{id}?qty= - updates stock quantity for a product
    route("/update-stock/{id}") {
        put {
            val productId = call.parameters["id"]?.toIntOrNull()
            val quantity = call.request.queryParameters["qty"]?.toIntOrNull()

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

    // OFFSALE ENDPOINTS

    // GET /offsale/{id}?potential=&reviewed= - marks a product as offsale
    // Note: should be PUT (using GET temporarily for testing)
    // Uses arbitrary userId until login system is working
    route("/offsale/{id}") {
        get {
            val productId = call.parameters["id"]?.toIntOrNull()

            val potential = call.request.queryParameters["potential"]?.toBoolean() ?: false
            val reviewed = call.request.queryParameters["reviewed"]?.toBoolean() ?: false

            if (productId == null) {
                call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                return@get
            }

            val productBefore = ProductRepository.getProductById(productId)
            if (productBefore == null) {
                call.respond(HttpStatusCode.OK, "Product not found!")
                return@get
            }

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
                val productAfter = ProductRepository.getProductById(productId)
                val summary = OffsaleSummary(
                    productName = productBefore.name,
                    quantityBefore = productBefore.stockLevel,
                    quantityAfter = productAfter?.stockLevel
                        ?: 67676767,
                    potentialOffsale = potential,
                    status = "Successfully marked offsale"
                )
                call.respond(HttpStatusCode.OK, summary)
                return@get
            }
        }
    }

    // GET /print-offsale-logs - returns all offsale logs as plain text
    route("/print-offsale-logs") {
        get {
            val offsaleLogText = StringRepository.getAllOffsaleLogsString()
            call.respondText(offsaleLogText, ContentType.Text.Html)
        }
    }

    post {
        try {
            val request = call.receive<ProductRequest>()
            val newProductId = ProductRepository.createProduct(request)

            if (newProductId != null) {
                call.respondText("Product created with ID: $newProductId", status = HttpStatusCode.Created)
            } else {
                call.respondText("Invalid Categoy/ID", status = HttpStatusCode.BadRequest)
            }
        } catch (e: Exception) {
            call.respondText("Invalid request body: ${e.message}", status = HttpStatusCode.BadRequest)
        }
    }



    // WASTAGE ENDPOINTS

    // GET /wastage/{id}?qty= - records product wastage
    // Note: should be PUT (using GET temporarily for testing)
    // Uses arbitrary userId and random waste reason
    route("/wastage/{id}") {
        get {
            val productId = call.parameters["id"]?.toIntOrNull()
            if (productId == null) {
                call.respondText("Invalid Product ID format", status = HttpStatusCode.BadRequest)
                return@get
            } else {
                var weighted = false
                val product = ProductRepository.getProductById(productId)
                if (product != null) {
                    if (product.soldByWeight == false) {
                        weighted = false
                    } else {
                        weighted = true
                    }
                }

                if (weighted) {
                    val qtyWasted = call.request.queryParameters["qty"]?.toIntOrNull() ?: 1
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
                    val qtyWasted = call.request.queryParameters["qty"]?.toIntOrNull() ?: 1
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

    // GET /print-wastage-logs - returns all wastage logs as plain text
    route("/print-wastage-logs") {
        get {
            val wastageLogText = StringRepository.getALlWastageLogsString()
            call.respondText(wastageLogText, ContentType.Text.Html)
        }
    }
}

//- getAllProducts *
//- getProductById *
//- getProductByName (serach product) *
//- getProducstByCategory *
//- getCategories *
//- createProduct *
//- updateProduct
//- deleteProduct
//- getSections ( secondary i.e. make sure the all other functions are working before these)
//- getProductsBySection ( secondary )
//- getPromoProducts ( secondary )
//- getProductByBarcode ( secondary )