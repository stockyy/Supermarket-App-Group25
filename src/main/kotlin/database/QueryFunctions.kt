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

private fun wrapInPre(content: String): String {
    return "<html><body style='font-family: monospace; white-space: pre; padding: 20px;'>$content</body></html>"
}

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

            val text = buildString {
                // Define the table layout.
                val tableFormat = "%-3s | %-30s | %-40s | %-6s | %-6s | %-8s | %-8s | %-8s | %-10s | %-35s | %-10s | %-15s\n"

                // Print the Header Row
                append(
                    String.format(
                        tableFormat,
                        "ID", "NAME", "DESCRIPTION", "CAT_ID", "SEC_ID", "ON_OFFER",
                        "PRICE", "STOCK", "BY_WEIGHT", "IMAGE_URL", "WASTE_BAG", "BARCODE"
                    ) + "\n"
                )

                // Print a separator line underneath the header
                append("-".repeat(200) + "\n")

                // Print the Data Rows
                productQuery.forEach { row ->
                    append(
                        String.format(
                            tableFormat,
                            row[Product.id],
                            row[Product.name].take(29),
                            row[Product.description].toString().take(39),
                            row[Product.categoryId],
                            row[Product.sectionId],
                            row[Product.onOffer],
                            "£${row[Product.price]}",
                            row[Product.stockLevel],
                            row[Product.soldByWeight],
                            row[Product.imageUrl].toString().take(34),
                            row[Product.wasteBag],
                            row[Product.barcode]
                        ) + "\n"
                    )
                }
            }
            wrapInPre(text)
        }
    }

    fun getAllProducts(): List<ProductResponse> {
        return transaction {
            Product.selectAll().map { row ->
                ProductResponse(
                    id = row[Product.id],
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
            }
        }
    }

    fun getProductsByCategory(categoryName: String): List<ProductResponse> {
        return transaction {
            val categoryRow = Category.selectAll().where { Category.name eq categoryName }.singleOrNull()

            if (categoryRow == null) {
                return@transaction emptyList()
            }
            val categoryId = categoryRow[Category.id]
            Product.selectAll().where { Product.categoryId eq categoryId }
                .map { row ->
                    ProductResponse(
                        id = row[Product.id],
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
                }
        }
    }



    /**
     * Searches for a product based on id
     * returns a ProductResponse data class
     * or null if product doesn't exist
     */

    fun searchProductsByName(searchProduct: String): List<ProductResponse> {
        return transaction {
            Product.selectAll().where { Product.name like "%$searchProduct%" }
                .map { row ->
                    ProductResponse(
                        id = row[Product.id],
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
                }
        }
    }


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

    fun createProduct(request: ProductRequest): Int? {
        return transaction {
            val categoryExists = Category.selectAll().where { Category.id eq request.categoryId }.count() > 0
            val sectionExists = Section.selectAll().where { Section.id eq request.sectionId }.count() > 0

            if (!categoryExists || !sectionExists) {
                return@transaction null
            }

            val insertProduct = Product.insert {
                it[name] = request.name
                it[description] = request.description
                it[categoryId] = request.categoryId
                it[sectionId] = request.sectionId
                it[onOffer] = request.onOffer
                it[price] = request.price
                it[stockLevel] = request.stockLevel
                it[soldByWeight] = request.soldByWeight
                it[imageUrl] = request.imageUrl
                it[wasteBag] = request.wasteBag
                it[barcode] = request.barcode
                it[location] = request.location
            }

            insertProduct[Product.id]
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

    fun getAllCategories(): List<String> {
        return transaction {
            Category.selectAll().map { row ->
                row[Category.name]
            }
        }
    }

    fun updateProduct(productId: Int, request: ProductRequest): Boolean {
        return transaction {
            val updatedRows = Product.update({ Product.id eq productId }) {
                it[name] = request.name
                it[description] = request.description
                it[categoryId] = request.categoryId
                it[sectionId] = request.sectionId
                it[onOffer] = request.onOffer
                it[price] = request.price
                it[stockLevel] = request.stockLevel
                it[soldByWeight] = request.soldByWeight
                it[imageUrl] = request.imageUrl
                it[wasteBag] = request.wasteBag
                it[barcode] = request.barcode
                it[location] = request.location
            }

            updatedRows > 0
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

            val text = buildString {
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
            wrapInPre(text)
        }
    }
    fun getAllOffsaleLogsString(): String {
        return transaction {
            // select the data
            val logQuery = OffsaleLog.selectAll().orderBy(OffsaleLog.dateTime, SortOrder.DESC)

            // build the string
            val text = buildString {
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
            wrapInPre(text)
        }
    }
    fun getAllWorkersString(): String {
        return transaction {
            // Execute Query to fins all users that are not customers, sorted by role
            val staffQuery = Users.selectAll().where (Users.role neq UserRole.CUSTOMER).orderBy(Users.role to SortOrder.ASC)
            val text = buildString {
                // Define the table layout with specific column widths
                val tableFormat = "%-5s | %-20s | %-20s | %-30s | %-15s | %-10s | %-12s | %-20s\n"

                // Print the Header Row
                append(String.format(tableFormat,
                    "ID", "FIRST NAME", "SURNAME", "EMAIL", "PHONE", "ROLE", "DOB", "PASSWORD"
                )
                )

                // Print a separator line
                append("-".repeat(160) + '\n')

                // Print each user
                staffQuery.forEach { row ->
                    append(String.format(
                        tableFormat,
                        row[Users.id].toString(),
                        row[Users.firstName],
                        row[Users.lastName],
                        row[Users.email],
                        row[Users.phoneNumber],
                        row[Users.role].name,
                        row[Users.dob].toString(),
                        row[Users.password]
                    )
                    )
                    append("\n")
                }
            }
            wrapInPre(text)
        }

    }

    fun getAllUsersString(): String {
        return transaction {
            val query = Users.selectAll().orderBy(Users.id, SortOrder.ASC)
            val text = buildString {
                val tableFormat = "%-5s | %-20s | %-20s | %-30s | %-15s | %-10s | %-12s\n"
                append(String.format(tableFormat, "ID", "FIRST NAME", "SURNAME", "EMAIL", "PHONE", "ROLE", "DOB"))
                append("-".repeat(130) + '\n')
                query.forEach { row ->
                    append(String.format(
                        tableFormat,
                        row[Users.id],
                        row[Users.firstName],
                        row[Users.lastName],
                        row[Users.email],
                        row[Users.phoneNumber],
                        row[Users.role].name,
                        row[Users.dob].toString()
                    ))
                }
            }
            wrapInPre(text)
        }
    }

    fun getAllOrdersString(): String {
        return transaction {
            val query = Order.selectAll().orderBy(Order.orderTime, SortOrder.DESC)
            val text = buildString {
                val tableFormat = "%-5s | %-8s | %-10s | %-25s | %-10s | %-50s\n"
                append(String.format(tableFormat, "ID", "USER ID", "COST", "TIME", "STATUS", "ITEMS"))
                append("-".repeat(130) + '\n')
                query.forEach { row ->
                    val orderId = row[Order.id]
                    val items = (OrderItem innerJoin Product).selectAll().where { OrderItem.orderId eq orderId }
                        .map { "${it[Product.name]}(x${it[OrderItem.quantity]})" }
                        .joinToString(", ")
                    append(String.format(
                        tableFormat,
                        orderId,
                        row[Order.userId],
                        "£${row[Order.totalCost]}",
                        row[Order.orderTime].toString(),
                        row[Order.status],
                        items
                    ))
                }
            }
            wrapInPre(text)
        }
    }

    fun getAllCartsString(): String {
        return transaction {
            val query = Cart.selectAll().orderBy(Cart.id, SortOrder.ASC)
            val text = buildString {
                val tableFormat = "%-5s | %-8s | %-10s | %-50s\n"
                append(String.format(tableFormat, "ID", "USER ID", "COST", "ITEMS"))
                append("-".repeat(100) + '\n')
                query.forEach { row ->
                    val cartId = row[Cart.id]
                    val items = (CartItem innerJoin Product).selectAll().where { CartItem.cartId eq cartId }
                        .map { "${it[Product.name]}(x${it[CartItem.quantity]})" }
                        .joinToString(", ")
                    append(String.format(
                        tableFormat,
                        cartId,
                        row[Cart.userId],
                        "£${row[Cart.totalCost]}",
                        items
                    ))
                }
            }
            wrapInPre(text)
        }
    }
}

object HtmlRepository {
    fun getAllProductsHtml(): String {
        return transaction {
            val query = Product.selectAll().orderBy(Product.id, SortOrder.ASC)
            buildString {
                append("<html><head><style>")
                append("table { width: 100%; border-collapse: collapse; font-family: sans-serif; }")
                append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                append("th { background-color: #f2f2f2; }")
                append("img { max-width: 50px; height: auto; border-radius: 4px; }")
                append("</style></head><body>")
                append("<h2>All Products</h2>")
                append("<table>")
                append("<tr><th>ID</th><th>Photo</th><th>Name</th><th>Price</th><th>Stock</th><th>Barcode</th><th>Location</th></tr>")
                query.forEach { row ->
                    val imgUrl = row[Product.imageUrl] ?: ""
                    append("<tr>")
                    append("<td>${row[Product.id]}</td>")
                    append("<td><img src='$imgUrl' alt='No Image'></td>")
                    append("<td>${row[Product.name]}</td>")
                    append("<td>£${row[Product.price]}</td>")
                    append("<td>${row[Product.stockLevel]}</td>")
                    append("<td>${row[Product.barcode]}</td>")
                    append("<td>${row[Product.location]}</td>")
                    append("</tr>")
                }
                append("</table></body></html>")
            }
        }
    }
}