package com.supermarket.database

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*

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

    // READ ALL PRODUCTS
    fun getAllProductsString(): String {
        return transaction {
            val productQuery = Product.selectAll()

            buildString {
                // Define the table layout.
                val tableFormat = "%-25s | %-30s | %-6s | %-6s | %-8s | %-8s | %-8s | %-10s | %-25s | %-10s | %-15s\n"

                // Print the Header Row
                append(
                    String.format(
                        tableFormat,
                        "NAME", "DESCRIPTION", "CAT_ID", "SEC_ID", "ON_OFFER",
                        "PRICE", "STOCK", "BY_WEIGHT", "IMAGE_URL", "WASTE_BAG", "BARCODE"
                    ) + "\n"
                )

                // Print a separator line underneath the header
                append("-".repeat(175) + "\n")

                // Print the Data Rows
                Product.selectAll().forEach { row ->
                    append(
                        String.format(
                            tableFormat,
                            // Using .toString().take(N) reduce long strings so they don't break the table format
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
}

object UserRepository {
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