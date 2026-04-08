package com.supermarket.database

import kotlinx.serialization.*

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
    val categories: String? = null,

    // SerialName searches for the "value" argument in the JSON, and stores it in the following defined variable
    @SerialName("product_name") val productName: String? = null,
    @SerialName("generic_name") val genericName: String? = null,
    @SerialName("image_url") val imageUrl: String? = null

)