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
            // read the content of admin.html from the resources directory
            val htmlContent = call.application.javaClass.getResource("/admin.html")?.readText()

            if (htmlContent != null) {
                // serve file content as html
                call.respondText(htmlContent, ContentType.Text.Html)
            } else {
                call.respondText("Admin page not found", status = HttpStatusCode.NotFound)
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


