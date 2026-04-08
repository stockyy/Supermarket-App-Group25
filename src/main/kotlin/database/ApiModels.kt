package com.supermarket.database
import kotlinx.serialization.Serializable

/**
 * This file contains data classes for reading data in from the Open Food Facts (OFF) API.
 * The classes tell Kotlin how to read the JSON data returned by the API and turn it into
 * Kotlin objects that we can work with in our code.
 */

@Serializable
data class OpenFoodFactsResponse(
    val count: Int,
    val products: List<OffProduct>
)

@Serializable
data class OffProduct(
    val code: String? = null, // for barcode
    val productName: String? = null,
    val genericName: String? = null,
    val imageUrl: String? = null,
    val categories: String? = null
)