package com.supermarket.database

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.date

/**
 * The Object repositories in this file contain functions for interacting
 * with different parts of the database.
 * These functions then can be used when setting up a route for a frontend
 * to query.
 */

object ProductRepository {

    // ADD PRODUCT
    fun addProduct(name: String, description: String, categoryId: Int, sectionId: Int, onOffer: Boolean, promo: Boolean, price: Float,
                    stockLevel: Int, soldByWeight: Boolean, wasteBagId: Int, barcode: String) {
        transaction {
            Product.insert{
                it[Product.name] = name
                it[Product.description] = description
                it[Product.categoryId] = categoryId
                it[Product.sectionId] = sectionId
                it[Product.onOffer] = onOffer
                it[Product.price] = price
                it[Product.stockLevel] = stockLevel
                it[Product.soldByWeight] = soldByWeight
                it[Product.wasteBag] = wasteBag
                it[Product.barcode] = barcode
            }
        }
    }

    // READ ALL PRODUCTS FROM DB AS A STRING
    fun getAllProductsString(): String {
        return transaction {
            val productQuery = Product.selectAll()

            buildString {
                // Define the table layout.
                val tableFormat = "%-2s | %-25s | %-30s | %-6s | %-6s | %-8s | %-8s | %-8s | %-10s | %-25s | %-10s | %-15s\n"

                // Print the Header Row
                append(
                    String.format(
                        tableFormat,
                        "ID", "NAME", "DESCRIPTION", "CAT_ID", "SEC_ID", "ON_OFFER",
                        "PRICE", "STOCK", "BY_WEIGHT", "IMAGE_URL", "WASTE_BAG", "BARCODE"
                    ) + "\n"
                )

                // Print a separator line underneath the header
                append("-".repeat(175) + "\n")

                // Print the Data Rows
                productQuery.forEach { row ->
                    append(
                        String.format(
                            tableFormat,
                            // Using .toString().take(N) reduce long strings so they don't break the table format
                            row[Product.id],
                            row[Product.name].take(24),
                            row[Product.description].toString().take(29),
                            row[Product.categoryId],
                            row[Product.sectionId],
                            row[Product.onOffer],
                            "£${row[Product.price]}",
                            row[Product.stockLevel],
                            row[Product.soldByWeight],
                            row[Product.imageUrl].toString().take(24),
                            row[Product.wasteBag],
                            row[Product.barcode]
                        ) + "\n"
                    )
                }
            }
        }
    }

    /**
     * Searches for a product based on id
     * returns a ProductResponse data class
     * or null if product doesn't exist
     */
    fun getProductById(productId: Int): ProductResponse? {
        return transaction {
            val query = Product.selectAll().where{Product.id eq productId}

            val productRow = query.map { row ->
                ProductResponse(
                    id=row[Product.id],
                    name = row[Product.name],
                    description = row[Product.description].toString(),
                    categoryId = row[Product.categoryId],
                    sectionId = row[Product.sectionId],
                    onOffer = row[Product.onOffer],
                    price = row[Product.price],
                    stockLevel = row[Product.stockLevel],
                    soldByWeight = row[Product.soldByWeight],
                    imageUrl = row[Product.imageUrl].toString(),
                    wasteBag = row[Product.wasteBag],
                    barcode = row[Product.barcode]
                )
            }.singleOrNull()

        // Returns either the product, or null if the id didn't exist
        return@transaction productRow
        }
    }

    fun createOffsaleLog(productId: Int, userId: Int, potentialOffsale: Boolean, managerReview: Boolean): Boolean {
        return transaction {
            // ENFORCE BUSINESS RULE: A potential offsale cannot be reviewed by definition.
            val isActuallyReviewed = if (potentialOffsale) false else managerReview

            // Set stock to 0 if not a potential offsale
            if (!potentialOffsale) {
                val updateStockRow = Product.update({ Product.id eq productId }) { row ->
                    row[Product.stockLevel] = 0
                }
                // If stock update failed, then return false
                if (updateStockRow == 0) {
                    return@transaction false
                }
            }

            var updatedCount = 0

            // If manager is reviewing an offsale, update attributes
            if (isActuallyReviewed) {
                updatedCount = OffsaleLog.update({ (OffsaleLog.productId eq productId) and (OffsaleLog.managerReviewed eq false) }) {
                        it[OffsaleLog.managerReviewed] = true
                    }
            }

            // Always create a new log
            OffsaleLog.insert {
                it[OffsaleLog.productId] = productId
                it[OffsaleLog.userId] = userId
                it[OffsaleLog.potentialOffsale] = potentialOffsale
                it[OffsaleLog.managerReviewed] = isActuallyReviewed
            }
            // Offsale Log successfully processed
            return@transaction true

        }
    }

    fun createWastageLog(productId: Int, userId: Int, wasteReason: WasteReasons, quantity: Int): Boolean {
        return transaction {
            val update = Product.update({Product.id eq productId}) {
                it.update(Product.stockLevel, Product.stockLevel - quantity)
            }
            if (update == 0) {
                return@transaction false
            }

            WastageLog.insert {
                it[WastageLog.productId] = productId
                it[WastageLog.quantity] = quantity
                it[WastageLog.userId] = userId
                it[WastageLog.reason] = wasteReason
            }
            return@transaction true
        }
    }

    fun updateProductQuantity(productId: Int, quantity: Int): Boolean {
        return transaction {
            val update = Product.update({Product.id eq productId}) {
                it[Product.stockLevel] = quantity
            }
            if (update == 0) {
                return@transaction false
            }
            else {
                return@transaction true
            }
        }
    }
}

object UserRepository {
}

object StringRepository {
    fun getALlWastageLogsString(): String {
        return transaction {
            val query = WastageLog.selectAll().orderBy(WastageLog.dateTime, SortOrder.DESC)

            buildString {
                val tableFormat = "%-8s | %-8s | %-10s | %-8s | %-15s | %-25s\n"

                append(String.format(tableFormat, "LOG ID", "PROD ID", "USER ID", "QTY", "REASON", "DATETIME"))

                append("-".repeat(95) + '\n')

                query.forEach {row ->
                    append(String.format(tableFormat,
                        row[WastageLog.id],
                        row[WastageLog.productId],
                        row[WastageLog.userId],
                        row[WastageLog.quantity],
                        row[WastageLog.reason],
                        row[WastageLog.dateTime]))
                }
            }
        }
    }
    fun getAllOffsaleLogsString(): String {
        return transaction {
            // select the data
            val logQuery = OffsaleLog.selectAll().orderBy(OffsaleLog.dateTime, SortOrder.DESC)

            // build the string
            buildString {
                val tableFormat = "%-8s | %-8s | %-8s | %-12s | %-10s | %-25s\n"

                append(String.format(tableFormat,
                    "LOG ID", "PROD ID", "USER ID", "POTENTIAL", "REVIEWED", "DATETIME"))

                append("-".repeat(90) + '\n')

                logQuery.forEach {row ->
                    append(String.format(tableFormat,
                        row[OffsaleLog.id],
                        row[OffsaleLog.productId],
                        row[OffsaleLog.userId],
                        row[OffsaleLog.potentialOffsale],
                        row[OffsaleLog.managerReviewed],
                        row[OffsaleLog.dateTime]
                    ) + "\n")
                }
            }
        }
    }
    fun getAllWorkersString(): String {
        return transaction {
            // Execute Query to fins all users that are not customers, sorted by role
            val staffQuery = Users.selectAll().where (Users.role neq UserRole.CUSTOMER).orderBy(Users.role to SortOrder.ASC)
            buildString {
                // Define the table layout with specific column widths
                val tableFormat = "%-5s | %-15s | %-15s | %-30s | %-15s | %-10s | %-12s | %-20s\n"

                // Print the Header Row
                append(String.format(tableFormat,
                    "ID", "FIRST NAME", "SURNAME", "EMAIL", "PHONE", "ROLE", "DOB", "PASSWORD"
                )
                )

                // Print a separator line
                append("-".repeat(140) + '\n')

                // Print each user
                staffQuery.forEach { row ->
                    append(String.format(
                        tableFormat,
                        row[Users.id].toString(),
                        row[Users.firstName].take(14),
                        row[Users.surname].take(14),
                        row[Users.email].take(29),
                        row[Users.phoneNumber].take(14),
                        row[Users.role].name,
                        row[Users.dob].toString(),
                        row[Users.password].take(19)
                    )
                    )
                    append("\n")
                }
            }
        }

    }
}