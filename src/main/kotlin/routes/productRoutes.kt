package com.supermarket.routes

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes() {
    route("/products") {

        get("/getAll") {
            // getAllProducts
        }
        get("/categories") {

        }

        get("/search") {

        }

        get("/category/{category}") {

        }

        get("/sections") {

        }

        get("/sections/{section}") {

        }

        get("/promos") {

        }

        get("/barcode/{barcode}") {

        }

        // GET /products/{id}
        get("/{id}") {
        }

        // /products
        post {

        }

        // /products/{id}
        put("/{id}") {

        }

        // /products/{id}
        delete("/{id}") {

        }
    }
}