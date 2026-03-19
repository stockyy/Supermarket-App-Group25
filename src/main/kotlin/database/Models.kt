package com.supermarket.database

import kotlinx.serialization.Serializable

@Serializable // Tells Kotlin to covert this into JSON
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String,
    val categoryId : Int,
    val sectionId: Int,
    val onOffer: Boolean,
    val price: Float,
    val stockLevel: Int,
    val soldByWeight: Boolean,
    val imageUrl: String,
    val wasteBag: WasteBags,
    val barcode: String
)
@Serializable
data class OffsaleSummary(
    val productName: String,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val status: String,
    val potentialOffsale: Boolean
)

@Serializable
data class WasteSummary(
    val productName: String,
    val quantityBefore: Int,
    val quantityAfter: Int,
)