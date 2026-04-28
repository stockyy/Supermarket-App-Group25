package com.supermarket.routes

import com.supermarket.controllers.StaffSession
import com.supermarket.database.UserRole
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*

fun Route.warehouseRoutes() {
    route("/warehouse") {

        get("/dashboard") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/dashboard.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/select-picks") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/selectPicks.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/see-full-list") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/seeFullList.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/picking-a-list") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/pickingAList.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/not-on-shelf") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/notOnShelf.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/add-item-to-cart") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/addItemToCrate.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }


        get("/settings") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/settings.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/scan-crates") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/scanCrate.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/offsales") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/offSales.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/stock-levels") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/stockLevels.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/wastage") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/wastage.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/picklist-finished") {
            val html = call.application.javaClass
                .getResource("/static/views/warehouse/pickListFinished.html")
                ?.readText()

            if (html != null) {
                call.respondText(html, ContentType.Text.Html)
            } else {
                call.respondText("Login page not found", status = HttpStatusCode.NotFound)
            }
        }

        get("/reports/sales") {

        }

        get("/reports/orders") {

        }

        get("/audit-log") {

        }

        route("/staff") {

            put("/{id}") {

            }

            delete("/{id}") {

            }
        }
    }
}