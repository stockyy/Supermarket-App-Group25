package com.supermarket

import com.supermarket.controllers.PicklistController
import com.supermarket.database.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import kotlin.test.*

class WorkerPickingE2ETest {

    @Test
    fun `test complete end to end worker picking flow`() {
        // Boot & seed the test database
        DatabaseCreation.init()
        refreshDatabase()

        // Run picklist generation
        PicklistController.generatePicklists()

        // Dynamically grab a seeded worker's ID
        val workerId = transaction {
            Users.selectAll().where { Users.role eq UserRole.WORKER }.first()[Users.id]
        }

        // Claim an ambient picklist
        val claimResult = PicklistController.claimPicklist(workerId, "AMBIENT")
        assertNotNull(claimResult, "Worker should be able to claim an available Ambient picklist")
        val picklistId = claimResult.first

        // Bind crates to the picklist
        val cratesToBind = listOf("CRATE-001", "CRATE-002", "CRATE-003", "CRATE-004", "CRATE-005", "CRATE-006")
        val bindError = PicklistController.bindCrates(picklistId, cratesToBind)
        assertNull(bindError, "Binding valid seeded crates should not return an error")

        // Fetch the first item to pick
        val firstItem = PicklistController.getNextItemToPick(picklistId)
        assertNotNull(firstItem, "There should be items to pick in the claimed list")

        // Get initial product ids and stock levels
        val actualProductId = transaction {
            PickItem.selectAll().where { PickItem.id eq firstItem.pickItemId }.single()[PickItem.productId]
        }
        val initialStock = transaction {
            Product.selectAll().where { Product.id eq actualProductId }.single()[Product.stockLevel]
        }

        // Get the new product stock level after picking
        val finalStock = transaction {
            Product.selectAll().where { Product.id eq actualProductId }.single()[Product.stockLevel]
        }

        // Calclulate teh
        var expectedStock = initialStock - firstItem.quantityRequired
        if (expectedStock < 0) { expectedStock = 0 }

        // Check if the database was updated with the correct qty
        assertEquals(
            expectedStock,
            finalStock,
            "Stock should decrement by exactly the picked quantity"
        )
    }
}