package com.supermarket.database

import kotlinx.serialization.*

/**
 * This file contains data classes for reading data in from our static JSON file containing product data.
 * The classes tell Kotlin how to read the JSON data and turn it into Kotlin objects that we can work with in our code.
 * These objects will be used to then insert data into the database.
 */

@Serializable
data class JsonProduct(
    val name: String,
    val description: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("section_name") val sectionName: SectionName,
    @SerialName("promo") val onOffer: Boolean,
    val price: Float,
    @SerialName("quantity") val stockLevel: Int,
    @SerialName("sold_by_weight") val soldByWeight: Boolean,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("waste_bag") val wasteBag: WasteBags,
    val barcode: String,
    val location: String,
    @SerialName("substitute_barcodes") val substituteBarcodes: List<String>

)