package com.supermarket.database

import net.datafaker.Faker
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import java.util.Locale
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

// val faker = Faker(Locale.UK)

fun seedDatabaseIfNeeded(forceSeed: Boolean) {
    val numUsers = 100
    val numProducts = 300
    val numOrders = 500
    val numOrderItems = 1500
    val numRoutes = 20
    val numCrates = 1000

    val shouldSeed = transaction {
        // If there are 0 users, database is empty and we should seed
        Users.selectAll().empty()
    }

    if (!shouldSeed && !forceSeed) {
        println("Database Already has data")
        return
    }

    else {
        println("Seeding Database")
    }

    val definedCategories = listOf(
        "Milk & Cream" to 1,
        "Butter & Margarine" to 1,
        "Fruit Juices" to 1,
        "Fresh Meat" to 1,
        "Cooking Oils" to 2,
        "Canned Goods" to 2,
        "Cereal & Breakfast" to 2,
        "Frozen Ready Meals" to 3,
        "Ice Cream" to 3,
        "Fresh Bread" to 4,
        "Salad Vegetables" to 4,
        "Fresh Fruit" to 4
    )

    // Order of seeding matters for foreign keys - You cannot seed an Order before the User exists.
    seedSections()
    seedCategories(definedCategories)
    seedUsers(numUsers)
    seedProducts(numProducts, definedCategories.size, SectionName.entries.size)
    seedOrders(numOrders, numUsers)
    seedOrderItems(numOrderItems, numOrders, numProducts)
    seedRoutes(numRoutes, numUsers)
    seedCrates(numCrates, numOrders, numRoutes)

    // Logs & Maps
    seedWastageLogs(50, numProducts, numUsers)
    seedOffsaleLogs(30, numProducts, numUsers)
    seedProductSubstituteMap(50, numProducts)
    seedSubstituteItems(40, numOrders, numProducts)

    println("Database seeding complete!")
}

//fun cartsString(numCarts: Int, numUsers: Int): List<String> {}
//fun cartsString(numCarts: Int, numUsers: Int): List<String> {}
//fun cartItemsString(numItems: Int, numCarts: Int, numProducts: Int): List<String> {}
//fun picklistsString(numPicklists: Int, numUsers: Int): List<String> { ... }
//fun routesString(numRoutes: Int, numUsers: Int): List<String> { ... }
//fun pickItemsString(numPickItems: Int, numProducts: Int, numPicklists: Int, numOrders: Int, numCrates: Int): List<String> { ... }


/**
 * For all of the following seed functions:
 * Data attributes are all random & faked
 * foreign keys therefore do not match & so initial data isn't properly related
 */
fun seedUsers(numUsers: Int) {
    // 1. Open a connection to the database
    transaction {

        // 2. batchInsert is highly optimized for SQLite
        Users.batchInsert(1..numUsers) {

            // 3. Assign Faker data directly to the Exposed columns
            this[Users.email] = faker.internet().emailAddress()
            this[Users.phoneNumber] = faker.phoneNumber().cellPhone()
            this[Users.password] = faker.credentials().password(8, 16)
            this[Users.firstName] = faker.name().firstName()
            this[Users.lastName] = faker.name().lastName() // Matched to your table definition
            this[Users.role] = UserRole.entries.random()

            // Exposed's date() column expects a java.time.LocalDate object, not a String!
            val dobString = faker.timeAndDate().birthday(18, 99, "yyyy-MM-dd")
            this[Users.dob] = LocalDate.parse(dobString, DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
    println("Successfully inserted $numUsers users directly into SQLite.")
}

/**
Category & Section ids don't match up, that will be sorted with API integration
Prices are random
image URLs don't work
Waste Bags don't match up
Fake barcode
double random = min + Math.random() * (max - min);
**/
fun seedProducts(numProducts: Int, numCategories: Int, numSections: Int) {
    transaction {
        Product.batchInsert(1.. numProducts) { i ->
            this[Product.name] = faker.food().ingredient()
            this[Product.description] = faker.food().dish()
            this[Product.categoryId] = Random.nextInt(1, numCategories + 1)
            this[Product.sectionId] = Random.nextInt(1, numSections + 1)
            this[Product.onOffer] = faker.bool().bool()
            this[Product.price] = faker.number().randomDouble(2, 0, 15).toFloat()
            this[Product.stockLevel] = faker.number().numberBetween(10, 500)
            this[Product.soldByWeight] = faker.bool().bool()
            this[Product.imageUrl] = "/img/product_$i.jpg"
            this[Product.wasteBag] = WasteBags.entries.random()
            this[Product.barcode] = faker.code().ean13()
        }
    }
}

fun seedSections() {
    transaction {
        // iterate over sectionName.entries
        Section.batchInsert(SectionName.entries) { sectionName ->
            this[Section.name] = sectionName
        }
    }
    println("Seeded Sections")
}

fun seedCategories(categories: List<Pair<String, Int>>) {
    transaction {
        Category.batchInsert(categories) { categoryPair ->
            this[Category.name] = categoryPair.first
            this[Category.sectionId] = categoryPair.second
        }
    }
    println("Seeded Categories")
}

fun seedOrders(numOrders: Int, numUsers: Int) {
    transaction {
        Order.batchInsert(1..numOrders) {
            this[Order.userId] = Random.nextInt(1, numUsers + 1)
            this[Order.deliveryAddress] = faker.address().fullAddress()

            val orderDateTime = LocalDateTime.now().minusDays(Random.nextLong(0, 30)).minusHours(Random.nextLong(1, 12))
            val deliveryStart =
                orderDateTime.plusDays(Random.nextLong(1, 4)).withHour(Random.nextInt(8, 18)).withMinute(0)
                    .withSecond(0)

            this[Order.orderTime] = orderDateTime
            this[Order.deliveryWindowStart] = deliveryStart
            this[Order.deliveryWindowEnd] = deliveryStart.plusHours(1) // 1 hour window

            this[Order.totalCost] = faker.number().randomDouble(2, 10, 150).toFloat()
            this[Order.status] = OrderStatus.entries.random()
        }
    }
}

fun seedOrderItems(numItems: Int, numOrders: Int, numProducts: Int) {
    transaction {
        (1..numItems).chunked(100).forEach { batch ->
            OrderItem.batchInsert (batch) {
                this[OrderItem.productID] = Random.nextInt(1, numProducts + 1)
                this[OrderItem.orderId] = Random.nextInt(1, numOrders + 1)
                this[OrderItem.priceAtOrder] = faker.number().randomDouble(2, 0, 10).toFloat()

                val isWeighed = faker.bool().bool()

                this[OrderItem.quantity] = if (isWeighed) null else Random.nextInt(1, 6)
                this[OrderItem.weight] = if (isWeighed) faker.number().randomDouble(2, 0, 3).toFloat() else null

                this[OrderItem.substitutionID] = null
            }
        }
    }
    println("Seeded Order Items")
}

fun seedRoutes(numRoutes: Int, numUsers: Int) {
    transaction {
        Route.batchInsert(1..numRoutes) {
            this[Route.driverId] = Random.nextInt(1, numUsers + 1) // Ideally pick a user with DRIVER role
            this[Route.routeDate] = LocalDate.now().plusDays(Random.nextLong(0, 7))
        }
    }
    println("Seeded Routes")
}

fun seedCrates(numCrates: Int, numOrders: Int, numRoutes: Int) {
    transaction {
        Crate.batchInsert(1..numCrates) {
            val isEmpty = Random.nextInt(100) < 30 // 30% chance crate is empty

            this[Crate.orderId] = if (isEmpty) null else Random.nextInt(1, numOrders + 1)
            this[Crate.routeId] = if (isEmpty) null else Random.nextInt(1, numRoutes + 1)
        }
    }
    println("Seeded Crates")
}

fun seedWastageLogs(numLogs: Int, numProducts: Int, numUsers: Int) {
    transaction {
        WastageLog.batchInsert(1..numLogs) {
            this[WastageLog.productId] = Random.nextInt(1, numProducts + 1)
            this[WastageLog.userId] = Random.nextInt(1, numUsers + 1)
            this[WastageLog.reason] = WasteReasons.entries.random()
            this[WastageLog.quantity] = Random.nextInt(1, 20)

            val randomPastDate = faker.timeAndDate().past(30, java.util.concurrent.TimeUnit.DAYS)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            this[WastageLog.dateTime] = randomPastDate
        }
    }
    println("Seeded Wastage Logs")
}

/**
 * UserId doesn't necessarily point to to a worker/manager
 */
fun seedOffsaleLogs(numLogs: Int, numProducts: Int, numUsers: Int) {
    transaction {
        OffsaleLog.batchInsert(1..numLogs) {
            val isPotentialOffsale = faker.bool().bool()
            this[OffsaleLog.productId] = Random.nextInt(1, numProducts + 1)
            this[OffsaleLog.userId] = Random.nextInt(1, numUsers + 1)
            this[OffsaleLog.potentialOffsale] = isPotentialOffsale
            this[OffsaleLog.managerReviewed] = if (isPotentialOffsale) faker.bool().bool() else false
            val randomPastDate = faker.timeAndDate().past(30, java.util.concurrent.TimeUnit.DAYS)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()

            this[OffsaleLog.dateTime] = randomPastDate
        }
    }
    println("Seeded Offsale Logs")
}

fun seedProductSubstituteMap(numMappings: Int, numProducts: Int) {
    val uniquePairs = mutableSetOf<Pair<Int, Int>>()
    while (uniquePairs.size < numMappings) {
        val original = Random.nextInt(1, numProducts + 1)
        var sub = Random.nextInt(1, numProducts + 1)
        while (sub == original) { sub = Random.nextInt(1, numProducts + 1) }
        uniquePairs.add(Pair(original, sub))
    }

    transaction {
        ProductSubstituteMap.batchInsert(uniquePairs) { pair ->
            this[ProductSubstituteMap.originalProductId] = pair.first
            this[ProductSubstituteMap.substituteProductId] = pair.second
        }
    }
    println("Seeded Product Substitute Maps")
}

fun seedSubstituteItems(numSubs: Int, numOrders: Int, numProducts: Int) {
    transaction {
        SubstituteItem.batchInsert(1..numSubs) {
            this[SubstituteItem.orderId] = Random.nextInt(1, numOrders + 1)

            val original = Random.nextInt(1, numProducts + 1)
            var sub = Random.nextInt(1, numProducts + 1)
            while (sub == original) { sub = Random.nextInt(1, numProducts + 1) }

            this[SubstituteItem.originalProductId] = original
            this[SubstituteItem.newProductId] = sub

            this[SubstituteItem.originalPrice] = faker.number().randomDouble(2, 1, 5).toFloat()
            this[SubstituteItem.newPrice] = faker.number().randomDouble(2, 1, 5).toFloat()

            val isWeighed = faker.bool().bool()
            this[SubstituteItem.quantitySubstituted] = if (isWeighed) null else Random.nextInt(1, 4)
            this[SubstituteItem.weightSubstituted] = if (isWeighed) faker.number().randomDouble(2, 0, 2).toFloat() else null
        }
    }
    println("Seeded Substitute Items")
}

fun refreshDatabaseDataFaker() {
    transaction {
        // 1. destroy tables (Children first, Parents last)
        SchemaUtils.drop(
            OrderItem, SubstituteItem, ProductSubstituteMap, WastageLog, OffsaleLog, Crate,
            Order, Route, Product, Category, Section, Users
        )

        // Create tables (Parents first, Children last)
        SchemaUtils.create(
            Users, Section, Category, Product, Route, Order, Crate, OffsaleLog,
            WastageLog, ProductSubstituteMap, SubstituteItem, OrderItem
        )

        println("Database schema successfully wiped and rebuilt.")

        seedDatabaseIfNeeded(true)

        println("Fresh dummy data successfully seeded.")
    }
}
