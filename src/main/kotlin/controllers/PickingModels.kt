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
    val wasteBag: WasteBags
)

// data class to receive JSON containing the crate ids from the frontend
@Serializable
data class BindCratesRequest(val picklistId: Int, val barcodes: List<String>)