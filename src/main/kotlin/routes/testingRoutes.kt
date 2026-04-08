package com.supermarket.routes

import com.supermarket.database.getGroceryData
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.testingRoutes() {
    get("/test-api") {
        val apiProducts = getGroceryData()

        if (apiProducts.isNullOrEmpty()) {
            call.respondText(
                "Failed to fetch data, check terminal logs for details.",
                status = HttpStatusCode.InternalServerError
            )
        } else {
            call.respond(apiProducts)
        }
    }
}