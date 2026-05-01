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
    val location: String
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
data class PutawayCrate(val crateNumber: Int, val crateBarcode: String, val putawayLocation: String
)