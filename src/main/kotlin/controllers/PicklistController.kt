package com.supermarket.controllers

import com.supermarket.database.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.collections.*

object PicklistController {

    // The constraints of the warehouse
    private const val MAX_ITEMS_PER_CRATE = 20
    private const val MAX_CRATES_PER_TROLLEY = 6
    private const val MAX_ITEMS_PER_LIST = 80

    // Generates pick lists for a certain date, by default it is for tomorrow
    fun generatePicklists(targetDate: LocalDate = LocalDate.now().plusDays(1)): Int {
        var totalListsCreated = 0

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

                // Variables used to make sure picklists don't exceed the physical trolley limits
                var currentTrolleyItems = 0
                var currentTrolleyCrates = 0

                // Holds the order items for the picklist
                var picklistItems = mutableListOf<ResultRow>()

                // Find the number of items in each order & the number of crates that need to be assigned to each order
                for ((orderId, orderItems) in itemsByOrder) {
                    var thisOrderItemCount = 0

                    for (orderItem in orderItems) {
                        thisOrderItemCount += orderItem[OrderItem.quantity]
                            ?: 1 // Any item measure by weight is counted as having qty 1
                    }

                    val numCratesRequired = ceil(thisOrderItemCount.toDouble() / MAX_ITEMS_PER_CRATE).toInt()

                    // If adding this order overflows the trolley then create a picklist with the items already in trolley
                    if (currentTrolleyItems + thisOrderItemCount > MAX_ITEMS_PER_LIST || currentTrolleyCrates + numCratesRequired > MAX_CRATES_PER_TROLLEY) {
                        val insertStatement = Picklist.insert {
                            it[Picklist.quantity] = currentTrolleyItems
                            it[Picklist.pickerId] = null
                            it[Picklist.expectedPickRate] = null
                            it[Picklist.actualPickRate] = null
                            it[Picklist.timeStart] = null
                            it[Picklist.timeEnd] = null
                        }

                        val newPicklistId = insertStatement[Picklist.id]

                        PickItem.batchInsert(picklistItems) { row ->
                            this[PickItem.productId] = row[OrderItem.productID]
                            this[PickItem.picklistId] = newPicklistId
                            this[PickItem.crateId] = null
                            this[PickItem.orderId] = row[Order.id]
                            this[PickItem.quantity] = row[OrderItem.quantity]
                            this[PickItem.weight] = row[OrderItem.weight]
                            this[PickItem.substituted] = false
                        }

                        // Picklist has been created
                        totalListsCreated += 1

                        // reset trolley data
                        picklistItems.clear()
                        currentTrolleyItems = 0
                        currentTrolleyCrates = 0
                    }

                    // Trolley not yet full - Add the items from this order to temporary trolley data
                    picklistItems.addAll(orderItems)
                    currentTrolleyItems += thisOrderItemCount
                    currentTrolleyCrates += numCratesRequired

                }

                // Check if there are leftover items that haven't yet been added
                if (picklistItems.isNotEmpty()) {
                    val finalInsert = Picklist.insert {
                        it[Picklist.quantity] = currentTrolleyItems
                        it[Picklist.pickerId] = null
                        it[Picklist.expectedPickRate] = null
                        it[Picklist.actualPickRate] = null
                        it[Picklist.timeStart] = null
                        it[Picklist.timeEnd] = null
                    }

                    val finalPicklistId = finalInsert[Picklist.id]

                    PickItem.batchInsert(picklistItems) { row ->
                        this[PickItem.productId] = row[OrderItem.productID]
                        this[PickItem.picklistId] = finalPicklistId
                        this[PickItem.crateId] = null
                        this[PickItem.orderId] = row[Order.id]
                        this[PickItem.quantity] = row[OrderItem.quantity]
                        this[PickItem.weight] = row[OrderItem.weight]
                        this[PickItem.substituted] = false
                    }
                }

            }
            if (pendingItems.isEmpty()) return@transaction
        }
        return totalListsCreated
    }

    // Returns the number of picklsit that are yet to be picked or selected by a worker
    fun getAvailablePicklistCounts(): Map<String, Int> {
        return transaction {
            // gets all unpicked picks
            val query = (Picklist innerJoin PickItem innerJoin Product innerJoin Section)
                .select(Section.name)
                .where { Picklist.pickerId eq null }
                .groupBy(Picklist.id, Section.name)

            // setup map to return
            val counts = mutableMapOf(
                "CHILLED" to 0,
                "AMBIENT" to 0,
                "FROZEN" to 0,
                "FRV_AND_BRD" to 0
            )

            // update map values
            query.forEach { row ->
                val sectionName = row[Section.name].name
                counts[sectionName] = counts.getOrDefault(sectionName, 0) + 1
            }
            counts
        }
    }
}