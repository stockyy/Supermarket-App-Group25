package com.supermarket

import com.supermarket.controllers.PicklistController
import com.supermarket.database.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.*

class WorkerPickingE2ETest {
    @Test
    fun `test logging an offsale during picking zeroes stock and completes item`() {
        // Initialise data
        DatabaseCreation.init()
        refreshDatabase()
        PicklistController.generatePicklists()

        // Get random worker Id & start a pick list
        val workerId = transaction { Users.selectAll().where { Users.role eq UserRole.WORKER }.first()[Users.id] }
        val picklistId = PicklistController.claimPicklist(workerId, "AMBIENT")!!.first
        PicklistController.bindCrates(
            picklistId,
            listOf("CRATE-001", "CRATE-002", "CRATE-003", "CRATE-004", "CRATE-005", "CRATE-006"),
        )

        // Grab the item to be offsaled
        val item = PicklistController.getNextItemToPick(picklistId)!!
        val productId =
            transaction { PickItem.selectAll().where { PickItem.id eq item.pickItemId }.single()[PickItem.productId] }

        // Log offsale
        val success = PicklistController.reportOffsale(item.pickItemId, workerId)
        assertTrue(success, "Offsale reporting should succeed")

        // Stock should be entirely zeroed out across the system
        val finalStock =
            transaction { Product.selectAll().where { Product.id eq productId }.single()[Product.stockLevel] }
        assertEquals(0, finalStock, "Stock should be 0 after an offsale is reported")
    }

    @Test
    fun `test logging wastage accurately decrements stock`() {
        // Initialise data
        DatabaseCreation.init()
        refreshDatabase()

        // Testing with the first product in the DB
        val workerId = transaction { Users.selectAll().where { Users.role eq UserRole.WORKER }.first()[Users.id] }
        val productId = 1
        val wasteQty = 3

        val initialStock =
            transaction { Product.selectAll().where { Product.id eq productId }.single()[Product.stockLevel] }

        // Log wastage
        val success = ProductRepository.createWastageLog(productId, workerId, WasteReasons.DAMAGED, wasteQty)
        assertTrue(success, "Wastage logging should succeed")

        // Check if new stock level is accurate
        val finalStock =
            transaction { Product.selectAll().where { Product.id eq productId }.single()[Product.stockLevel] }
        // Calculate what the stock level should be, set it to 0 if it should go negative bc database does not allow 0's
        var expectedStock = initialStock - wasteQty
        if (expectedStock < 0) {
            expectedStock = 0
        }

        assertEquals(initialStock - wasteQty, finalStock, "Stock should decrement by exactly the wasted quantity")
    }

    @Test
    fun `test manual stock level amendment overwrites database correctly`() {
        DatabaseCreation.init()
        refreshDatabase()

        val productId = 1
        val newStockLevel = 150

        // Update stock manually
        val success = ProductRepository.updateProductQuantity(productId, newStockLevel)
        assertTrue(success, "Stock update should succeed")

        // Check if stock is at the new set level
        val finalStock =
            transaction { Product.selectAll().where { Product.id eq productId }.single()[Product.stockLevel] }
        assertEquals(newStockLevel, finalStock, "Stock should exactly match the manual amendment")
    }

    @Test
    fun `test complete end to end worker picking flow`() {
        // Boot & seed the test database
        DatabaseCreation.init()
        refreshDatabase()

        // Run picklist generation
        PicklistController.generatePicklists()

        // Dynamically grab a seeded worker's ID
        val workerId =
            transaction {
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
        val actualProductId =
            transaction {
                PickItem.selectAll().where { PickItem.id eq firstItem.pickItemId }.single()[PickItem.productId]
            }
        val initialStock =
            transaction {
                Product.selectAll().where { Product.id eq actualProductId }.single()[Product.stockLevel]
            }

        // Actually Pick the item
        val pickSuccess = PicklistController.confirmPickItem(firstItem.pickItemId, firstItem.quantityRequired)
        assertTrue(pickSuccess, "Confirming the pick should update the database successfully")

        // Get the new product stock level after picking
        val finalStock =
            transaction {
                Product.selectAll().where { Product.id eq actualProductId }.single()[Product.stockLevel]
            }

        // Calculate what the stock level should be, set it to 0 if it should go negative bc database does not allow 0's
        var expectedStock = initialStock - firstItem.quantityRequired
        if (expectedStock < 0) {
            expectedStock = 0
        }

        // Check if the database was updated with the correct qty
        assertEquals(
            expectedStock,
            finalStock,
            "Stock should decrement by exactly the picked quantity",
        )
    }
}
