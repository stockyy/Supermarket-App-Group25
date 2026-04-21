package com.supermarket

import com.supermarket.database.*
import com.supermarket.routes.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            val html = call.application.javaClass
                .getResource("/static/views/index.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("/ page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/components") {
            val html = call.application.javaClass
                .getResource("/static/views/components.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("/ page not found", status = HttpStatusCode.NotFound)
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
        
        static("/static") {
            resources("static")
        }
        // Skeleton set up
    }
}
