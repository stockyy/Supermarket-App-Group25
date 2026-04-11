package com.supermarket.database

import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.transactions.*
import java.io.InputStream

const val INFILE_PATH = "/productData.json"

fun parseJsonData(infile: String): List<JsonProduct> {
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
    println("Beggining seeding of categories and sections...")
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
    println("Done seeding categories and sections...")
}

fun seedProducts(products: List<JsonProduct>) {
    println("Beginning seeding of products...")
    transaction {
        for (product in products) {
            try {
                // Get the categoryId and sectionId for each product
                // if there are either no or multiple categories with a matching name then an exception will be thrown
                // (because of .single())
                val categoryId = Category.selectAll().where { Category.name eq product.categoryName }.single()[Category.id]
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
    println("Done seeding products...")
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
                    val originalId = Product.selectAll().where { Product.barcode eq product.barcode}.single()[Product.id]

                    // Loop through all substitutes, find the substitute product id using its barcode,
                    // and then add the substitution to the database
                    for (substituteBarcode in substitutes) {
                        val subId = Product.selectAll().where { Product.barcode eq substituteBarcode }.single()[Product.id]
                        ProductSubstituteMap.insert {
                            it[originalProductId] = originalId
                            it[substituteProductId] = subId
                        }
                    }
                }
            }
            catch (e: Exception) {
                println("Could not add substitute: ${product.name} - ${e.message}")
            }
        }
    }
    println("Done seeding substitutes...")
}

fun seedDatabaseJson() {
    val productsToSeed = parseJsonData(INFILE_PATH)

    seedCategoriesAndSections(productsToSeed)
    seedProducts(productsToSeed)
    seedSubstitutes(productsToSeed)
}

fun refreshDatabaseJson() {
    transaction {
        val allTables = arrayOf(CartItem, PickItem, OrderItem, SubstituteItem, ProductSubstituteMap, WastageLog, OffsaleLog, Crate, Picklist, Cart, Order, Product, Category, Route, address, Section, Users)
        
        SchemaUtils.drop(*allTables)
        SchemaUtils.create(*allTables)
    }
    seedDatabaseJson()
}