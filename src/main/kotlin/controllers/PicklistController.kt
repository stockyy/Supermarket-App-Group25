package com.supermarket.controllers

import com.supermarket.database.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.*
import kotlin.math.ceil

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
            val pendingItems =
                (Order innerJoin OrderItem innerJoin Product innerJoin Section)
                    .selectAll()
                    .where {
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
                    if (currentTrolleyItems + thisOrderItemCount > MAX_ITEMS_PER_LIST ||
                        currentTrolleyCrates + numCratesRequired > MAX_CRATES_PER_TROLLEY
                    ) {
                        val insertStatement =
                            Picklist.insert {
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
                    val finalInsert =
                        Picklist.insert {
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
            val query =
                (Picklist innerJoin PickItem innerJoin Product innerJoin Section)
                    .select(Section.name)
                    .where { Picklist.pickerId eq null }
                    .groupBy(Picklist.id, Section.name)

            // setup map to return
            val counts =
                mutableMapOf(
                    "CHILLED" to 0,
                    "AMBIENT" to 0,
                    "FROZEN" to 0,
                    "FRV_AND_BRD" to 0,
                )

            // update map values
            query.forEach { row ->
                val sectionName = row[Section.name].name
                counts[sectionName] = counts.getOrDefault(sectionName, 0) + 1
            }
            counts
        }
    }

    // Allows a specific worker to claim a picklist
    fun claimPicklist(
        workerId: Int,
        sectionNameStr: String,
    ): Pair<Int, Int>? {
        return transaction {
            val targetSection = SectionName.valueOf(sectionNameStr.uppercase())

            // Find the first available picklist
            val availableListId =
                (Picklist innerJoin PickItem innerJoin Product innerJoin Section)
                    .select(Picklist.id)
                    .where { (Picklist.pickerId.isNull()) and (Section.name eq targetSection) }
                    .limit(1)
                    .singleOrNull()?.get(Picklist.id) ?: return@transaction null

            // Claim the list
            Picklist.update({ Picklist.id eq availableListId }) {
                it[pickerId] = workerId
            }

            // calculate the number of crates needed based on distinct orders
            val pickItems = PickItem.selectAll().where { PickItem.picklistId eq availableListId }.toList()
            val itemsByOrder = pickItems.groupBy { it[PickItem.orderId] }

            var cratesNeeded = 0
            for ((_, items) in itemsByOrder) {
                var itemCount = 0
                for (item in items) {
                    itemCount += item[PickItem.quantity] ?: 1
                }
                cratesNeeded += ceil(itemCount.toDouble() / MAX_ITEMS_PER_CRATE).toInt()
            }

            // picklist ID along with the num of crates needed
            Pair(availableListId, cratesNeeded)
        }
    }

    // Unclaims a picklist from a worker for if they hit the back button before starting the pick
    fun unclaimPicklist(
        picklistId: Int,
        workerId: Int,
    ) {
        transaction {
            // Only unclaim if crates haven't been assigned yet, and the worker owns it
            val isUnbound =
                PickItem.selectAll().where { (PickItem.picklistId eq picklistId) and (PickItem.crateId.isNotNull()) }
                    .empty()

            // unclaim picklist
            if (isUnbound) {
                Picklist.update({ (Picklist.id eq picklistId) and (Picklist.pickerId eq workerId) }) {
                    it[pickerId] = null
                }
            }
        }
    }

    // Bind crates & lock in pick to worker
    fun bindCrates(
        picklistId: Int,
        scannedBarcodes: List<String>,
    ): String? {
        return transaction {
            // Validate crates exist and aren't in use
            val cratesToAssign = Crate.selectAll().where { Crate.barcode inList scannedBarcodes }.toList()

            if (cratesToAssign.size != scannedBarcodes.size) {
                rollback() // Undo any changes
                return@transaction "One or more scanned crates do not exist."
            }
            if (cratesToAssign.any { it[Crate.orderId] != null }) {
                rollback()
                return@transaction "Crate already in use!"
            }

            // Order the items on the pick list by their order id
            val pickItems = PickItem.selectAll().where { PickItem.picklistId eq picklistId }.toList()
            val itemsByOrder = pickItems.groupBy { it[PickItem.orderId] }
            var crateIndex = 0

            // Assign crates to pickItems in each order
            for ((orderId, items) in itemsByOrder) {
                // Calculate the number of crates needed
                var itemCount = 0
                for (item in items) {
                    itemCount += item[PickItem.quantity] ?: 1 // if weighted, just treat qty as 1
                }
                val cratesNeeded = ceil(itemCount.toDouble() / MAX_ITEMS_PER_CRATE).toInt()

                // Claim the crates for this order and store their ids in a list
                val orderCrates = mutableListOf<Int>()
                for (i in 1..cratesNeeded) {
                    if (crateIndex >= cratesToAssign.size) return@transaction "Not enough crates scanned!"
                    val crateId = cratesToAssign[crateIndex][Crate.id]

                    // assign order id to the crate
                    Crate.update({ Crate.id eq crateId }) { it[Crate.orderId] = orderId }
                    orderCrates.add(crateId)
                    crateIndex++
                }

                // Distribute pickItems amongst the selected crates
                var currentActiveCrateIndex = 0
                var currentCrateFill = 0

                for (item in items) {
                    val qty = item[PickItem.quantity] ?: 1 // if weighted, just treat qty as 1
                    val pickItemId = item[PickItem.id]

                    // If adding this item overflows the current crate, swap to the next crate
                    if (currentCrateFill + qty > 20) {
                        currentActiveCrateIndex++
                        currentCrateFill = 0
                    }

                    val targetCrateId = orderCrates[currentActiveCrateIndex]

                    // Tell the system which crate the pickItem should go into
                    PickItem.update({ PickItem.id eq pickItemId }) {
                        it[crateId] = targetCrateId
                    }

                    // Log that the crate is now holding these items
                    currentCrateFill += qty
                }
            }
            // Success - start pick timer
            Picklist.update({ Picklist.id eq picklistId }) { it[Picklist.timeStart] = LocalDateTime.now() }
            null
        }
    }

    fun getNextItemToPick(picklistId: Int): NextPickItem? {
        return transaction {
            // Fetch all items for this picklist
            val allItems =
                (PickItem innerJoin Product innerJoin Section)
                    .selectAll()
                    .where { (PickItem.picklistId eq picklistId) }
                    .orderBy(Product.location to SortOrder.ASC)

            // Find the first item where the worker hasn't picked the required amount
            val nextItemRow =
                allItems.firstOrNull { row ->
                    val required = row[PickItem.quantity] ?: 1
                    val picked = row[PickItem.qtyPicked]
                    picked < required
                }

            // Null means that the pick list is finished
            if (nextItemRow == null) return@transaction null

            // Calculate wty left to pick for this specific item
            val totalRequired = nextItemRow[PickItem.quantity] ?: 1
            val alreadyPicked = nextItemRow[PickItem.qtyPicked]
            val remainingToPick = totalRequired - alreadyPicked

            // Load data into data class & return it
            NextPickItem(
                pickItemId = nextItemRow[PickItem.id],
                picklistId = picklistId,
                productName = nextItemRow[Product.name],
                orderId = nextItemRow[PickItem.orderId],
                // needs default value bc crateId is nullable
                crateId = nextItemRow[PickItem.crateId] ?: 1,
                quantityRequired = remainingToPick,
                categoryName = nextItemRow[Section.name].name,
                wasteBag = nextItemRow[Product.wasteBag],
                imageDir = nextItemRow[Product.imageUrl],
                location = nextItemRow[Product.location],
                isSubstitute = nextItemRow[PickItem.substituted],
            )
        }
    }

    fun getRemainingItems(picklistId: Int): List<NextPickItem> {
        return transaction {
            // Fetch all items for this picklist
            val allItems =
                (PickItem innerJoin Product innerJoin Section)
                    .selectAll()
                    .where { (PickItem.picklistId eq picklistId) }
                    .orderBy(Product.location to SortOrder.ASC)

            // Find all items where the worker hasn't picked the required amount
            val remainingItems =
                allItems.filter { row ->
                    val required = row[PickItem.quantity] ?: 1
                    val picked = row[PickItem.qtyPicked]
                    picked < required
                }

            // Map the remaining items to the NextPickItem data class
            remainingItems.map { row ->
                val totalRequired = row[PickItem.quantity] ?: 1
                val alreadyPicked = row[PickItem.qtyPicked]
                val remainingToPick = totalRequired - alreadyPicked

                NextPickItem(
                    pickItemId = row[PickItem.id],
                    picklistId = picklistId,
                    productName = row[Product.name],
                    orderId = row[PickItem.orderId],
                    crateId = row[PickItem.crateId] ?: 1,
                    quantityRequired = remainingToPick,
                    categoryName = row[Section.name].name,
                    wasteBag = row[Product.wasteBag],
                    imageDir = row[Product.imageUrl],
                    location = row[Product.location],
                    isSubstitute = row[PickItem.substituted],
                )
            }
        }
    }

    // Save the quantity picked to the database
    fun confirmPickItem(
        pickItemId: Int,
        qtyPicked: Int,
    ): Boolean {
        return transaction {
            // Find out qty already picked
            val currentItem =
                PickItem.selectAll().where { PickItem.id eq pickItemId }.singleOrNull() ?: return@transaction false
            val currentlyPicked = currentItem[PickItem.qtyPicked]

            // Add the new input to the existing total
            val updatedRows =
                PickItem.update({ PickItem.id eq pickItemId }) {
                    it[PickItem.qtyPicked] = currentlyPicked + qtyPicked
                }

            // Decrease Product Stock Level
            val productId = currentItem[PickItem.productId]

            val currentStock =
                Product.select(Product.stockLevel)
                    .where { Product.id eq productId }.singleOrNull()
                    ?.get(Product.stockLevel) ?: 0

            Product.update({ Product.id eq productId }) {
                it[stockLevel] = (currentStock - qtyPicked).coerceAtLeast(0) // Ensure stock doesn't drop below 0
            }

            val currentPicklistId = currentItem[PickItem.picklistId]

            val allListItems = PickItem.selectAll().where { PickItem.picklistId eq currentPicklistId }

            // Check if all items on the list have been picked
            val isFinished =
                allListItems.all { row ->
                    val required = row[PickItem.quantity] ?: 1
                    val picked = row[PickItem.qtyPicked]
                    picked >= required
                }

            // If the list is picked, updated pick list ending time
            if (isFinished) {
                Picklist.update({ Picklist.id eq currentPicklistId }) {
                    it[timeEnd] = LocalDateTime.now()
                }
                updateOrdersStatusForPicklist(currentPicklistId)
            }
            updatedRows > 0
        }
    }

    // Fetch the barcode of a crate using its ID
    fun getCrateBarcode(crateId: Int): String? {
        return transaction {
            Crate.selectAll().where { Crate.id eq crateId }
                .singleOrNull()?.get(Crate.barcode)
        }
    }

    // Testing tool - Instantly pick all remaining items on a list
    fun autoPickEntireList(picklistId: Int): Boolean {
        return transaction {
            // Find all items in the picklist
            val items =
                PickItem.selectAll()
                    .where { PickItem.picklistId eq picklistId }
                    .toList()

            // Loop through items and set the picked amount to the required amount
            for (item in items) {
                // Get qty info for updating fields in database
                val requiredQty = item[PickItem.quantity] ?: 1
                val alreadyPicked = item[PickItem.qtyPicked]
                val remainingToPick = requiredQty - alreadyPicked
                val productId = item[PickItem.productId]

                // Set the pickitem qty picked to the required qty
                PickItem.update({ PickItem.id eq item[PickItem.id] }) {
                    it[qtyPicked] = requiredQty
                }

                // Lower the stock levels for the picked product
                val currentStock =
                    Product.select(Product.stockLevel).where { Product.id eq productId }.singleOrNull()
                        ?.get(Product.stockLevel) ?: 0
                Product.update({ Product.id eq productId }) {
                    it[stockLevel] = (currentStock - remainingToPick).coerceAtLeast(0)
                }
            }

            // Mark the list itself as finished
            Picklist.update({ Picklist.id eq picklistId }) {
                it[timeEnd] = LocalDateTime.now()
            }
            updateOrdersStatusForPicklist(picklistId)

            true
        }
    }

    // Get the putaway locations for all crates in a finished picklist
    fun getPutawayDetails(picklistId: Int): List<PutawayCrate> {
        return transaction {
            // Find all crates used in this picklist and the section of the picklist
            val query =
                (PickItem innerJoin Crate innerJoin Product innerJoin Section)
                    .select(Crate.id, Crate.barcode, Crate.orderId, Section.name)
                    .where { (PickItem.picklistId eq picklistId) and (PickItem.crateId.isNotNull()) }
                    .withDistinct()
                    .toList()

            if (query.isEmpty()) return@transaction emptyList()

            // The section name is the same for all items in a specific picklist
            val sectionName = query.first()[Section.name].name

            // Map database rows to the PutawayCrate data class
            var crateCounter = 1
            query.distinctBy { it[Crate.id] }.map { row ->
                val barcode = row[Crate.barcode]
                val orderId = row[Crate.orderId] ?: 0
                val prefix =
                    when (sectionName) {
                        "CHILLED" -> "CHILLER"
                        "FROZEN" -> "FREEZER"
                        else -> "STAGING" // Ambient and FRV/Bread
                    }

                PutawayCrate(
                    crateNumber = crateCounter++,
                    crateBarcode = barcode,
                    putawayLocation = "$prefix-ORD$orderId",
                )
            }
        }
    }

    fun getSubstituteDetails(pickItemId: Int): List<SubstitutionDetails> {
        return transaction {
            // Get info regarding the original pick item
            val pickItemRow =
                PickItem.selectAll().where { PickItem.id eq pickItemId }.singleOrNull()
                    ?: return@transaction emptyList()
            val originalProdId = pickItemRow[PickItem.productId]

            // Calculate how many items actually need to be substituted
            val remainingQty = (pickItemRow[PickItem.quantity] ?: 1) - pickItemRow[PickItem.qtyPicked]

            // Grab the original product's price
            val originalPrice =
                Product.select(Product.price)
                    .where { Product.id eq originalProdId }
                    .singleOrNull()?.get(Product.price) ?: 0.0f

            // Query the Map table to find subs
            val subRows =
                ProductSubstituteMap.innerJoin(Product) {
                    ProductSubstituteMap.substituteProductId eq Product.id
                }.select(
                    Product.id,
                    Product.name,
                    Product.imageUrl,
                    Product.location,
                    Product.price,
                ).where {
                    ProductSubstituteMap.originalProductId eq originalProdId
                }.toList()

            // Map the sub rows to the data class & return it
            subRows.map { subRow ->
                SubstitutionDetails(
                    substituteProductId = subRow[Product.id],
                    name = subRow[Product.name],
                    imageUrl = subRow[Product.imageUrl],
                    originalPrice = originalPrice,
                    // Get the price of the new substitute
                    newPrice = subRow[Product.price],
                    // Suggest a 1-to-1 substitution ratio
                    quantitySubstituted = remainingQty,
                    location = subRow[Product.location],
                )
            }
        }
    }

    fun applyAndConfirmSubstitution(
        pickItemId: Int,
        substituteProductId: Int,
        qtyPickedInput: Int,
    ): Boolean {
        return transaction {
            val originalPickItem =
                PickItem.selectAll().where { PickItem.id eq pickItemId }.singleOrNull() ?: return@transaction false
            val originalQty = originalPickItem[PickItem.quantity] ?: 1
            val originalProdId = originalPickItem[PickItem.productId]
            val picklistId = originalPickItem[PickItem.picklistId]
            val orderId = originalPickItem[PickItem.orderId]
            val crateId = originalPickItem[PickItem.crateId]

            // Log the sub in the SubstituteItem table
            val originalPrice =
                Product.select(Product.price).where { Product.id eq originalProdId }.singleOrNull()?.get(Product.price)
                    ?: 0.0f
            val newPrice =
                Product.select(Product.price).where { Product.id eq substituteProductId }.singleOrNull()
                    ?.get(Product.price) ?: 0.0f

            SubstituteItem.insert {
                it[this.orderId] = orderId
                it[this.originalProductId] = originalProdId
                it[this.newProductId] = substituteProductId
                it[this.originalPrice] = originalPrice
                it[this.newPrice] = newPrice
                it[this.quantitySubstituted] = qtyPickedInput
            }

            // Reduce the quantity of the original pick item
            val newOriginalQty = originalQty - qtyPickedInput
            if (newOriginalQty <= 0) {
                // If substituted everything, just finish the original item at its current qtyPicked
                PickItem.update({ PickItem.id eq pickItemId }) {
                    it[quantity] = originalPickItem[PickItem.qtyPicked]
                }
            } else {
                // If partial substitution then decrease original required quantity
                PickItem.update({ PickItem.id eq pickItemId }) {
                    it[quantity] = newOriginalQty
                }
            }

            // Create a new pickitem for the chosen sub that is already "picked"
            PickItem.insert {
                it[PickItem.productId] = substituteProductId
                it[PickItem.picklistId] = picklistId
                it[PickItem.orderId] = orderId
                it[PickItem.crateId] = crateId
                it[PickItem.quantity] = qtyPickedInput
                it[PickItem.qtyPicked] = qtyPickedInput
                it[PickItem.substituted] = true
            }

            // Decrease sub product stock level
            val currentStock =
                Product.select(Product.stockLevel).where {
                    Product.id eq substituteProductId
                }.singleOrNull()?.get(Product.stockLevel) ?: 0
            Product.update({ Product.id eq substituteProductId }) {
                it[stockLevel] = (currentStock - qtyPickedInput).coerceAtLeast(0)
            }

            // Check if list is finished & if so then end time
            val allListItems = PickItem.selectAll().where { PickItem.picklistId eq picklistId }
            val isFinished =
                allListItems.all { row ->
                    val required = row[PickItem.quantity] ?: 1
                    val picked = row[PickItem.qtyPicked]
                    picked >= required
                }
            if (isFinished) {
                Picklist.update({ Picklist.id eq picklistId }) { it[timeEnd] = LocalDateTime.now() }
                updateOrdersStatusForPicklist(picklistId)
            }
            true
        }
    }

    fun reportOffsale(
        pickItemId: Int,
        workerId: Int,
    ): Boolean {
        return transaction {
            // Get the original pick item
            val originalPickItem =
                PickItem.selectAll().where { PickItem.id eq pickItemId }.singleOrNull() ?: return@transaction false
            val productId = originalPickItem[PickItem.productId]
            val picklistId = originalPickItem[PickItem.picklistId]
            val qtyPicked = originalPickItem[PickItem.qtyPicked]

            // log the offsale
            val logSuccess = ProductRepository.createOffsaleLog(productId, workerId, false, false)
            if (!logSuccess) return@transaction false

            // Complete the PickItem by setting its required quantity equal to what was already picked
            PickItem.update({ PickItem.id eq pickItemId }) {
                it[quantity] = qtyPicked
            }

            // Check if the entire list is now finished
            val allListItems = PickItem.selectAll().where { PickItem.picklistId eq picklistId }
            val isFinished =
                allListItems.all { row ->
                    val required = row[PickItem.quantity] ?: 1
                    val picked = row[PickItem.qtyPicked]
                    picked >= required
                }
            if (isFinished) {
                Picklist.update({ Picklist.id eq picklistId }) { it[timeEnd] = LocalDateTime.now() }
                updateOrdersStatusForPicklist(picklistId)
            }

            true
        }
    }

    private fun updateOrdersStatusForPicklist(picklistId: Int) {
        transaction {
            // Get all order IDs from the finished picklist
            val orderIdsToCheck =
                PickItem.select(PickItem.orderId)
                    .where { PickItem.picklistId eq picklistId }
                    .withDistinct()
                    .map { it[PickItem.orderId] }

            for (orderId in orderIdsToCheck) {
                // For each order, check if all its items (across all picklists) are picked
                val allOrderItems = PickItem.selectAll().where { PickItem.orderId eq orderId }
                val isOrderComplete =
                    allOrderItems.all {
                        val required = it[PickItem.quantity] ?: 1
                        val picked = it[PickItem.qtyPicked]
                        picked >= required
                    }

                // If the entire order is complete, update its status
                if (isOrderComplete) {
                    Order.update({ Order.id eq orderId }) {
                        it[status] = OrderStatus.PICKED
                    }
                }
            }
        }
    }

    fun getWorkerProfile(workerId: Int): WorkerProfileResponse? {
        return transaction {
            val user = Users.selectAll().where { Users.id eq workerId }.singleOrNull() ?: return@transaction null

            // Get all completed picklists
            val picklists =
                Picklist.selectAll().where {
                    (Picklist.pickerId eq workerId) and (Picklist.timeEnd.isNotNull())
                }.toList()

            val picksCompleted = picklists.size

            // Calculate the total time and total quantity picked
            var totalQuantity = 0
            var totalSeconds = 0L

            for (pick in picklists) {
                val qty = pick[Picklist.quantity]
                val start = pick[Picklist.timeStart]
                val end = pick[Picklist.timeEnd]

                if (start != null && end != null) {
                    totalQuantity += qty
                    totalSeconds += java.time.Duration.between(start, end).seconds
                }
            }

            // Calculate Average Pick Rate (items per hour)
            val avgPickRate =
                if (totalSeconds > 0) {
                    ((totalQuantity.toDouble() / totalSeconds) * 3600).toInt()
                } else {
                    0
                }

            WorkerProfileResponse(
                name = "${user[Users.firstName]} ${user[Users.lastName]}",
                staffId = user[Users.staffId] ?: "N/A",
                email = user[Users.email],
                phone = user[Users.phoneNumber] ?: "N/A",
                role = user[Users.role].name,
                dob = user[Users.dob].toString(),
                totalPicksCompleted = picksCompleted,
                averagePickRate = avgPickRate,
            )
        }
    }
}
