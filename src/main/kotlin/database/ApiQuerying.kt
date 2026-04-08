package com.supermarket.database

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.isSuccess
import kotlin.collections.*

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
// finds 500 products by default
suspend fun getGroceryData(targetAmount: Int = 500): List<OffProduct> {
    val allFilteredProducts = mutableListOf<OffProduct>()
    var page = 1

    println("Connecting to Open Food Facts API to fetch ${targetAmount} products...")

    return try {
        // keep looping until wee have hit our target number of products
        while (allFilteredProducts.size < targetAmount) {

            val url =
                "https://world.openfoodfacts.org/api/v2/search?categories_tags_en=groceries&page_size=250&page=$page&fields=code,product_name,categories,image_url&sort_by=unique_scans_n"

            // send the request to the API with headers to identify our app so we don't get blocked
            val httpResponse = httpClient.get(url) {
                header("User-Agent", "Supermarket App - Kotlin Student Project")
            }

            // if request was a success, then get response body and filter out products without enough data
            // if request fails, print error message and return empty list
            if (httpResponse.status.isSuccess()) {
                val response: OpenFoodFactsResponse = httpResponse.body()

                // filter out products without enough data
                val filteredProductsBatch = response.products.filter {
                    !it.productName.isNullOrBlank() &&
                            !it.categories.isNullOrBlank() &&
                            !it.code.isNullOrBlank() &&
                            !it.imageUrl.isNullOrBlank()
                }

                allFilteredProducts.addAll(filteredProductsBatch)
                println("Page ${1} processed, ${filteredProductsBatch.size} valid products found. Total valid products so far: ${allFilteredProducts.size}")
                page++

            } else {
                println("Failed to fetch data from Open Food Facts API. Status code: ${httpResponse.status}")
                return emptyList() // return an empty list if the request was not successful
                break
            }

        }
        println("Succeessfully collected ${allFilteredProducts.size} valid products.")
        return allFilteredProducts.take(targetAmount)
    }

// return an empty list if there was an error connecting to the API, and print the error message to the terminal
    catch (e: Exception) {
        println("Error connecting to Open Food Facts API: ${e.message}")
        emptyList()
    }
}