package com.supermarket.database

import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.*
import java.io.InputStream
import net.datafaker.Faker
import java.util.Locale
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.random.Random.Default.nextFloat

const val PRODUCT_DATA_INFILE = "/productData.json"
const val NUM_PRODUCTS = 200
val faker = Faker(Locale.UK)

fun seedDatabase() {
    val productsToSeed = parseProductData(PRODUCT_DATA_INFILE)

    seedCategoriesAndSections(productsToSeed)
    seedProducts(productsToSeed)
    seedSubstitutes(productsToSeed)
    seedUsers()
    seedCarts()
    seedAddresses()
    seedPastOrders()
    seedNewOrders()
}

fun refreshDatabase() {
    transaction {
        val allTables = arrayOf(
            CartItem,
            PickItem,
            OrderItem,
            SubstituteItem,
            ProductSubstituteMap,
            WastageLog,
            OffsaleLog,
            Crate,
            Picklist,
            Cart,
            Order,
            Product,
            Category,
            Route,
            Address,
            Section,
            Users
        )

        SchemaUtils.drop(*allTables)
        SchemaUtils.create(*allTables)
    }
    seedDatabase()
}

fun parseProductData(infile: String): List<JsonProduct> {
    println("Starting Database JSON Seeding Process...")

    // Parse the JSON file into a list of objects
    val inputStream: InputStream? = object {}.javaClass.getResourceAsStream(infile)
    if (inputStream == null) {
        println("Could not find productData.json in resources folder.")
        return emptyList()
    }

    val jsonString = inputStream.bufferedReader().use { it.readText() }
    val productsToSeed: List<JsonProduct> = Json.decodeFromString(jsonString)

    println("Successfully parsed ${productsToSeed.size} products from ${infile}.")
    return productsToSeed
}

fun seedCategoriesAndSections(products: List<JsonProduct>) {
    println("Beginning seeding of categories and sections...")
    transaction {
        // Get all unique categories & sections
        val uniqueCategories = products.map { it.categoryName }.distinct()
        val uniqueSections = products.map { it.sectionName }.distinct()

        // Loop through categories and insert them into the database if they don't already exist
        for (category in uniqueCategories) {
            if (Category.selectAll().where { Category.name eq category }.empty()) {
                Category.insert {
                    it[name] = category
                }
            }
        }

        // Loop through sections and insert them into the database if they don't already exist
        for (section in uniqueSections) {
            if (Section.selectAll().where { Section.name eq section }.empty()) {
                Section.insert {
                    it[name] = section
                }
            }
        }
    }
    println("Done seeding categories and sections")
}

fun seedProducts(products: List<JsonProduct>) {
    println("Beginning seeding of products...")
    transaction {
        for (product in products) {
            try {
                // Get the categoryId and sectionId for each product
                // if there are either no or multiple categories with a matching name then an exception will be thrown
                // (because of .single())
                val categoryId =
                    Category.selectAll().where { Category.name eq product.categoryName }.single()[Category.id]
                val sectionId = Section.selectAll().where { Section.name eq product.sectionName }.single()[Section.id]

                // Add database entry
                Product.insert {
                    it[name] = product.name
                    it[description] = product.description
                    it[Product.categoryId] = categoryId
                    it[Product.sectionId] = sectionId
                    it[onOffer] = product.onOffer
                    it[price] = product.price
                    it[stockLevel] = product.stockLevel
                    it[soldByWeight] = product.soldByWeight
                    it[imageUrl] = product.imageUrl
                    it[wasteBag] = product.wasteBag
                    it[barcode] = product.barcode
                    it[location] = product.location
                }
            } catch (e: Exception) {
                println("Could not add product: ${product.name} - ${e.message}")
            }
        }
    }
    println("Done seeding products")
}

fun seedSubstitutes(products: List<JsonProduct>) {
    println("Beginning seeding of substitutes...")
    transaction {
        for (product in products) {
            // Exception will be thrown there are either zero or multiple products that have the same barcode as the
            // substituted product
            try {
                // Get possible substitutes for each product
                val substitutes = product.substituteBarcodes

                // If a product has substitutes, add them to the database
                if (substitutes.isNotEmpty()) {
                    // Use the barcode to find the original product's id
                    val originalId =
                        Product.selectAll().where { Product.barcode eq product.barcode }.single()[Product.id]

                    // Loop through all substitutes, find the substitute product id using its barcode,
                    // and then add the substitution to the database
                    for (substituteBarcode in substitutes) {
                        val subId =
                            Product.selectAll().where { Product.barcode eq substituteBarcode }.single()[Product.id]
                        ProductSubstituteMap.insert {
                            it[originalProductId] = originalId
                            it[substituteProductId] = subId
                        }
                    }
                }
            } catch (e: Exception) {
                println("Could not add substitute: ${product.name} - ${e.message}")
            }
        }
    }
    println("Done seeding substitutes")
}

fun seedUsers(numCustomers: Int = 20, numWorkers: Int = 3, numManagers: Int = 1, numDrivers: Int = 2) {
    println("Beginning seeding of users...")
    transaction {
        insertUsers(numCustomers, UserRole.CUSTOMER)
        insertUsers(numWorkers, UserRole.WORKER)
        insertUsers(numManagers, UserRole.MANAGER)
        insertUsers(numDrivers, UserRole.DRIVER)
    }
    println("Done seeding users")
}

fun insertUsers(numUsers: Int, role: UserRole) {
    // Insert a number of users with the specified role, using datafaker for fake user info
    Users.batchInsert(1..numUsers) {
        this[Users.email] = faker.internet().emailAddress()
        this[Users.phoneNumber] = faker.phoneNumber().cellPhone()
        this[Users.password] = faker.credentials().password(8, 16)
        this[Users.firstName] = faker.name().firstName()
        this[Users.lastName] = faker.name().lastName()
        this[Users.role] = role

        val dobString = faker.timeAndDate().birthday(18, 99, "yyyy-MM-dd")
        this[Users.dob] = LocalDate.parse(dobString, DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

fun seedCarts() {
    println("Beginning seeding of carts & cart items...")
    transaction {
        // Assign a cart to every single customer
        val customers = Users.selectAll().where { Users.role eq UserRole.CUSTOMER }
        for (user in customers) {
            // create cart
            Cart.insert {
                it[Cart.userId] = user[Users.id]
                it[Cart.totalCost] = 0.0f
            }
            val cartId = Cart.selectAll().where { Cart.userId eq user[Users.id] }.single()[Cart.id]

            // Variable to hold running total of price of cart
            var priceTotal = 0.0f

            // Add a random number of random items to each cart
            val numItems = (3..40).random()
            for (i in 1..numItems) {

                // Get a random product
                val productNum = (1..NUM_PRODUCTS).random()
                val product = Product.selectAll().where { Product.id eq productNum }.single()

                // find if product is sold by weight
                val soldByWeight = product[Product.soldByWeight]

                // If product is already in cart then just add 1 to quantity/weight
                if (CartItem.selectAll().where { (CartItem.productId eq productNum) and (CartItem.cartId eq cartId) }
                        .empty() == false) {
                    // if sold by weight then update weight
                    if (soldByWeight) {
                        CartItem.update({ (CartItem.productId eq productNum) and (CartItem.cartId eq cartId) }) {
                            it[CartItem.weight] = CartItem.weight + 1f
                        }
                        priceTotal += product[Product.price]
                    }

                    // otherwise update quantity
                    else {
                        CartItem.update({ (CartItem.productId eq productNum) and (CartItem.cartId eq cartId) }) {
                            it[CartItem.quantity] = CartItem.quantity + 1
                        }
                        priceTotal += product[Product.price]
                    }
                }

                // Otherwise create new CartItem entry
                else {
                    CartItem.insert {
                        it[CartItem.productId] = productNum
                        it[CartItem.cartId] = cartId
                        // If sold by weight then assign a random weight between 0.1 and 5.0 kg & update priceTotal
                        if (soldByWeight) {
                            val weight = 0.1f + (4.9f * nextFloat())
                            it[CartItem.weight] = weight
                            priceTotal += weight * product[Product.price]

                            // otherwise assign a random quantity between 1 and 10 & update priceTotal
                        } else {
                            val quantity = (1..10).random()
                            it[CartItem.quantity] = quantity
                            priceTotal += quantity * product[Product.price]
                        }
                    }
                }
            }
            // Update cart total cost
            Cart.update({ Cart.id eq cartId }) {
                it[Cart.totalCost] = priceTotal
            }
        }
    }
    println("Done seeding cart & cart items")
}

fun seedAddresses() {
    println("Beginning seeding of addresses...")
    transaction {
        val users = Users.selectAll()
        for (user in users) {
            Address.insert {
                it[Address.userId] = user[Users.id]
                it[Address.line1] = faker.address().streetAddress()
                it[Address.line2] = faker.address().secondaryAddress()
                it[Address.city] = faker.address().city()
                it[Address.postcode] = faker.address().zipCode()
            }
        }
    }
    println("Done seeding addresses")
}

// Every customer has between 0 and 10 previous orders
fun seedPastOrders() {
    println("Beginning seeding of past orders...")
    transaction {
        val customers = Users.selectAll().where { Users.role eq UserRole.CUSTOMER }

        for (customer in customers) {
            // get customer information
            val customerId = customer[Users.id]
            val addressId = Address.selectAll().where { Address.userId eq customerId }.single()[Address.id]

            // Each customer has between 0 and 10 past orders
            val numOrders = (0..10).random()
            for (i in 1..numOrders) {
                // Orders are placed in the last 6 months, but not in the last week
                val orderDateTime = LocalDateTime.now().minusDays(Random.nextLong(7, 180)).minusHours(
                    Random.nextLong(1, 12)
                )
                val deliveryStart =
                    orderDateTime.plusDays(Random.nextLong(1, 4)).withHour(Random.nextInt(8, 18)).withMinute(0)
                        .withSecond(0)

                // Saving the data from the insert to a variable that can be queried
                val insertStatement = Order.insert {
                    it[Order.userId] = customerId
                    it[Order.totalCost] = 0.0f
                    it[Order.orderTime] = orderDateTime
                    it[Order.deliveryWindowStart] = deliveryStart
                    it[Order.deliveryWindowEnd] = deliveryStart.plusHours(4) // 4-hour delivery window
                    it[Order.status] = OrderStatus.DELIVERED
                    it[Order.deliveryAddressId] = addressId
                }

                // Create running price total variable
                var priceTotal = 0.0f

                val orderId = insertStatement[Order.id]

                // Add between 3 and 40 items to each order
                val numItems = (3..40).random()
                for (i in 1..numItems) {
                    // Seed item & update price total
                    priceTotal += seedRandomOrderItem(orderId)
                }
                // update order cost with priceTotal
                Order.update({ Order.id eq orderId }) {
                    it[Order.totalCost] = priceTotal
                }
            }
        }
    }
    println("Done seeding past order and order items")
}

fun createSubstitutionIfPossible(
    orderId: Int, originalProductRow: ResultRow, quantity: Int?, weight: Float?
): Int? {
    val originalId = originalProductRow[Product.id]

    // Find a mapped substitute
    val possibleSubstitutes = ProductSubstituteMap
        .selectAll()
        .where { ProductSubstituteMap.originalProductId eq originalId }
        .map { it[ProductSubstituteMap.substituteProductId] }

    // If the list is empty, return null
    val substituteProductId = possibleSubstitutes.firstOrNull() ?: return null

    // Fetch the new product's details so we can
    val newProductRow = Product.selectAll().where { Product.id eq substituteProductId }.single()

    // Create the Substitution record
    val insertStatement = SubstituteItem.insert {
        it[SubstituteItem.orderId] = orderId
        it[SubstituteItem.originalProductId] = originalId
        it[SubstituteItem.newProductId] = substituteProductId
        it[SubstituteItem.originalPrice] = originalProductRow[Product.price]
        it[SubstituteItem.newPrice] = newProductRow[Product.price]

        // We will assume the warehouse substituted the exact original requested amount
        it[quantitySubstituted] = quantity
        it[weightSubstituted] = weight
    }

    // return the substitute id
    return insertStatement[SubstituteItem.id]
}

// For viewing new orders (i.e. Orders that are not yet picked) for picklist generation
// Every customer has a new order due to be delivered on the exact same day (tomorrow), so that there are enough orders...
// to test the picklist generation algorithm with
// These orders also contain more products also for the sake of testing picklist generation
fun seedNewOrders() {
    // Add between 15 and 50 items per order
    println("Beginning seeding of new orders...")
    transaction {
        val customers = Users.selectAll().where { Users.role eq UserRole.CUSTOMER }

        // Each customer has one order due to be delivered tomorrow
        for (customer in customers) {
            // get customer information
            val customerId = customer[Users.id]
            val addressId = Address.selectAll().where { Address.userId eq customerId }.single()[Address.id]

            // Orders are placed in the last 6 months, but not in the last week
            val orderDateTime = LocalDateTime.now()

            val deliveryStart = orderDateTime.plusDays(1).withHour(Random.nextInt(8, 18)).withMinute(0)
                .withSecond(0)

            // Saving the data from the insert to a variable that can be queried
            val insertStatement = Order.insert {
                it[Order.userId] = customerId
                it[Order.totalCost] = 0.0f
                it[Order.orderTime] = orderDateTime
                it[Order.deliveryWindowStart] = deliveryStart
                it[Order.deliveryWindowEnd] = deliveryStart.plusHours(4) // 4-hour delivery window
                it[Order.status] = OrderStatus.WAITING
                it[Order.deliveryAddressId] = addressId
            }

            // Create running price total variable
            var priceTotal = 0.0f

            val orderId = insertStatement[Order.id]

            // Add between 20 and 50 items to each order
            val numItems = (20..50).random()
            for (i in 1..numItems) {
                // Seed item & update price total
                priceTotal += seedRandomOrderItem(orderId, false)
            }
            // update order cost with priceTotal
            Order.update({ Order.id eq orderId }) {
                it[Order.totalCost] = priceTotal
            }
        }
        println("Done seeding new order and order items")
    }
}

fun seedRandomOrderItem(orderId: Int, subsAllowed: Boolean = true): Float {
    // Get a random product & weight/quantity
    val productNum = (1..NUM_PRODUCTS).random()
    val product = Product.selectAll().where { Product.id eq productNum }.single()

    // find if product is sold by weight & then generate either quantity or weight
    val soldByWeight = product[Product.soldByWeight]
    val quantity = if (soldByWeight) null else (1..10).random()
    val weight = if (soldByWeight) 0.1f + (4.9f * nextFloat()) else null

    // 20% chance of substituting the item
    val randomNum = (1..10).random()
    val subId = if (randomNum <= 2 && subsAllowed) {
        createSubstitutionIfPossible(orderId, product, quantity, weight)
    } else null

    // Determine the which price to charge (original vs substitution)
    val finalPricePerUnit = if (subId != null) {
        val newProductId = SubstituteItem.selectAll().where { SubstituteItem.id eq subId }
            .single()[SubstituteItem.newProductId]
        Product.selectAll().where { Product.id eq newProductId }.single()[Product.price]
    } else {
        product[Product.price]
    }

    // find total cost of the line
    val lineTotal = if (soldByWeight) {
        finalPricePerUnit * weight!!
    } else {
        finalPricePerUnit * quantity!!
    }

    // create new orderItem entry
    OrderItem.insert {
        it[OrderItem.productID] = productNum
        it[OrderItem.orderId] = orderId
        if (subId != null) {
            it[OrderItem.substitutionID] = subId
        }
        // If sold by weight then assign a random weight between 0.1 and 5.0 kg & update priceTotal
        if (soldByWeight) {
            it[OrderItem.weight] = weight
            it[OrderItem.priceAtOrder] = lineTotal

        // otherwise assign a random quantity between 1 and 10 & update priceTotal
        } else {
            it[OrderItem.quantity] = quantity
            it[OrderItem.priceAtOrder] = lineTotal
        }
    }
    return lineTotal
}