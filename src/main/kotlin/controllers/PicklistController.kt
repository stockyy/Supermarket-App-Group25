package com.supermarket.controllers

import com.supermarket.database.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.LocalDate

object PicklistController {

    // The constraints of the warehouse
    private const val MAX_ITEMS_PER_CRATE = 20
    private const val MAX_CRATES_PER_TROLLEY = 6
    private const val MAX_ITEMS_PER_LIST = 80

    fun generatePicklists(targetDate: LocalDate): Int {
        val totalListsCreated = 0

        // Convert the target LocalDate to the very start and very end of that day
        val startOfDay = targetDate.atStartOfDay()
        val endOfDay = targetDate.plusDays(1).atStartOfDay()

        transaction {
            // Collect all relevant information for orders due on the targeted day
            val pendingItems = (Order innerJoin OrderItem innerJoin Product innerJoin Section).selectAll().where {
                (Order.status eq OrderStatus.WAITING) and
                        (Order.deliveryWindowStart greaterEq startOfDay) and
                        (Order.deliveryWindowEnd less endOfDay)
            }.toList()

            // Sort the items by Section/Zone
            val itemsByZone = pendingItems.groupBy { it[Section.name] }

            for ((sectionName, itemsInSection) in itemsByZone) {
                println("Starting generation of $sectionName picklists")
                println("There are ${itemsInSection.size} items in this zone")

                // Group items in each zone by which order they are in.
                val itemsByOrder = itemsInSection.groupBy { it[Order.id] }

                for ((orderId, orderItems) in itemsByOrder) {
                    // Find how many crates need to be assigned to each order
                    val numItems = orderItems.size
                    val numCratesRequired = numItems / MAX_ITEMS_PER_CRATE
                }
            }

            if (pendingItems.isEmpty()) return@transaction

        }

        return totalListsCreated
    }
}