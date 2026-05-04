package com.supermarket.controllers

import com.supermarket.database.WasteBags
import kotlinx.serialization.Serializable

// Data class to hold the next item due for picking
@Serializable
data class NextPickItem(
    val pickItemId: Int,
    val picklistId: Int,
    val productName: String,
    val orderId: Int,
    val crateId: Int,
    val quantityRequired: Int,
    val categoryName: String,
    val imageDir: String?,
    val wasteBag: WasteBags,
    val location: String,
    val isSubstitute: Boolean,
)

@Serializable
data class ConfirmPickRequest(val pickItemId: Int, val qtyPicked: Int)

// data class to receive JSON containing the crate ids from the frontend
@Serializable
data class BindCratesRequest(val picklistId: Int, val barcodes: List<String>)

// data class to hold picklist id for automatically completing the pick
@Serializable
data class AutoPickRequest(val picklistId: Int)

// Data class to hold putaway locations
@Serializable
data class PutawayCrate(val crateNumber: Int, val crateBarcode: String, val putawayLocation: String)

// Data class to hold the information regarding a substitute
@Serializable
data class SubstitutionDetails(
    val substituteProductId: Int,
    val name: String,
    val imageUrl: String?,
    val originalPrice: Float,
    val newPrice: Float,
    val quantitySubstituted: Int,
    val location: String,
)

// Allows a substitution to be picked
@Serializable
data class ConfirmSubstitutionRequest(val pickItemId: Int, val substituteProductId: Int, val qtyPicked: Int)

// holds the item id for an offsale
@Serializable
data class OffsaleRequest(val pickItemId: Int)

// Gets offsale log info from the frontend
@Serializable
data class ProductOffsaleRequest(val productId: Int)

// Gets wastage log info from the frontend
@Serializable
data class WastageLogRequest(val productId: Int, val quantity: Int, val wasteReason: String)

// Gets stock level info from the frontend
@Serializable
data class StockUpdateRequest(val productId: Int, val newQuantity: Int)

// Holds information regarding the worker for the settings page
@Serializable
data class WorkerProfileResponse(
    val name: String,
    val staffId: String,
    val email: String,
    val phone: String,
    val role: String,
    val dob: String,
    val totalPicksCompleted: Int,
    val averagePickRate: Int,
)
