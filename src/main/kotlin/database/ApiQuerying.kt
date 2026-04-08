package com.supermarket.database

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.call.*
import io.ktor.client.request.get

/**
 * Create a single 'HttpClient' engine for the whole app to use.
 * It is configured to use JSON, and to ignore any extra fields Open Food Facts
 * sends that we didn't include in our data classes.
 **/
val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// suspend function so a thread doesn't have to wait for the query to be returned
suspend fun getGroceryData(): List<OffProduct> {
    return try {
        println("Connecting to Open Food Facts API...")

        val url =  "https://world.openfoodfacts.org/api/v2/search?categories_tags_en=groceries&page_size=100&fields=code,product_name,categories,image_url&sort_by=unique_scans_n"
        val response: OpenFoodFactsResponse = httpClient.get(url).body()

        println("Successfully downloaded ${response.products.size} items")

        response.products
    }

    catch (e: Exception) {
        println("Error connecting to Open Food Facts API: ${e.message}")
        emptyList() // return an empty list if there was an error
    }
}