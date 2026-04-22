package com.supermarket.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.*

enum class UserRole {
    CUSTOMER, WORKER, MANAGER, DRIVER
}

enum class SectionName {
    CHILLED, AMBIENT, FROZEN, FRV_AND_BRD
}

enum class OrderStatus {
    WAITING, PICKED, TRANSIT, DELIVERED
}

enum class WasteBags {
    GREEN, BLUE, CLEAR, RED
}

enum class WasteReasons {
    DAMAGED, EXPIRED, POOR_PACKAGING, DROPPED, OTHER
}

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email", 255)
    val phoneNumber = varchar("phoneNumber", 20).nullable()
    val password = varchar("password", 255)
    val firstName = varchar("firstname", 100)
    val lastName = varchar("surname", 100)
    val role = enumerationByName("role", 20, UserRole::class).default(UserRole.CUSTOMER)
    val dob = date("date_of_birth")

    override val primaryKey = PrimaryKey(id)
}


object Product : Table("product") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val categoryId = reference("category_id", Category.id)
    val sectionId = reference("section_id", Section.id)
    val onOffer = bool("promo")
    val price = float("price")
    val stockLevel = integer("quantity") // Might need changing to account for weight
    val soldByWeight = bool("sold_by_weight")
    val imageUrl = varchar("image_url", 255).nullable()
    val wasteBag = enumerationByName("waste_bag", 20, WasteBags::class)
    val barcode = varchar("barcode", 50)
    val location = varchar("location", 50)

    override val primaryKey = PrimaryKey(id)
}


object Order : Table("order") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val deliveryAddressId = reference("delivery_address", Address.id)
    val deliveryWindowStart = datetime("delivery_window_start")
    val deliveryWindowEnd = datetime("delivery_window_end")
    val totalCost = float("total_cost")
    val orderTime = datetime("order_time")
    val status = enumerationByName("status", 20, OrderStatus::class).default(OrderStatus.WAITING)

    override val primaryKey = PrimaryKey(id)
}


object Cart : Table("cart") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val totalCost = float("total_cost")

    override val primaryKey = PrimaryKey(id)
}


object OrderItem : Table("order_item") {
    val id = integer("id").autoIncrement()
    val productID = reference("product_id", Product.id)
    val orderId = reference("order_id", Order.id)
    val priceAtOrder = float("price_at_order")
    val quantity = integer("quantity").nullable()
    val weight = float("weight").nullable()
    val substitutionID = reference("substitution_id", SubstituteItem.id).nullable()

    override val primaryKey = PrimaryKey(id)
}


object Section : Table("section") {
    val id = integer("id").autoIncrement()
    val name = enumerationByName("name", 20, SectionName::class)

    override val primaryKey = PrimaryKey(id)
}


object Category : Table("category") {
    val id = integer("id").autoIncrement()

    // ENUM CLASS MAY BE USEFUL HERE
    val name = varchar("name", 100)

    // Might need to be nullable in case the category has no section:
    val sectionId = reference("section_id", Section.id).nullable()

    override val primaryKey = PrimaryKey(id)
}


object CartItem : Table("cart_item") {
    val id = integer("id").autoIncrement()
    val productId = reference("product_id", Product.id)
    val cartId = reference("cart_id", Cart.id)
    val quantity = integer("quantity").nullable()
    val weight = float("weight").nullable()

    override val primaryKey = PrimaryKey(id)
}

object WastageLog : Table("wastage_log") {
    val id = integer("id").autoIncrement()
    val productId = reference("product_id", Product.id)
    val userId = reference("user_id", Users.id)
    val reason = enumerationByName("reason", 30, WasteReasons::class)
    val dateTime = datetime("dateTime").defaultExpression(CurrentDateTime)
    val quantity = integer("quantity").nullable()
    val weight = float("weight").nullable()

    override val primaryKey = PrimaryKey(id)
}


object OffsaleLog : Table("offsale_log") {
    val id = integer("id").autoIncrement()
    val productId = reference("product_id", Product.id)
    val userId = reference("user_id", Users.id)
    val potentialOffsale = bool("potential_offsale")
    val managerReviewed = bool("manager_reviewed")
    val dateTime = datetime("dateTime").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}


object Picklist : Table("picklist") {
    val id = integer("id").autoIncrement()
    val pickerId = reference("picker_id", Users.id)
    val quantity = integer("quantity")
    val expectedPickRate = float("expected_pick_rate")
    val actualPickRate = float("actual_pick_rate")
    val timeStart = datetime("time_start")
    val timeEnd = datetime("time_end")

    override val primaryKey = PrimaryKey(id)
}


object PickItem : Table("pick_item") {
    val id = integer("id").autoIncrement()
    val productId = reference("product_id", Product.id)
    val picklistId = reference("picklist_id", Picklist.id)
    val orderId = reference("order_id", Order.id)
    val crateId = reference("crate_id", Crate.id)
    val substituted = bool("substituted")
    val quantity = integer("quantity").nullable()
    val weight = float("weight").nullable()

    override val primaryKey = PrimaryKey(id)
}


object SubstituteItem : Table("substitute") {
    val id = integer("id").autoIncrement()
    val orderId = reference("order_id", Order.id)
    val originalProductId = reference("original_product_id", Product.id)
    val newProductId = reference("new_product_id", Product.id)
    val originalPrice = float("original_price")
    val newPrice = float("new_price")
    val quantitySubstituted = integer("quantity_substituted").nullable()
    val weightSubstituted = float("weight_substituted").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Crate : Table("crate") {
    val id = integer("id").autoIncrement()
    val orderId = reference("order_id", Order.id).nullable()
    val routeId = reference("route_id", DeliveryRoute.id).nullable()

    override val primaryKey = PrimaryKey(id)

}

object DeliveryRoute : Table("route") {
    val id = integer("id").autoIncrement()
    val driverId = reference("driver_id", Users.id)
    val routeDate = date("route_date")

    override val primaryKey = PrimaryKey(id)
}

object ProductSubstituteMap : Table("product_substitute_map") {
    val originalProductId = reference("original_product_id", Product.id)
    val substituteProductId = reference("substitute_product_id", Product.id)

    override val primaryKey = PrimaryKey(originalProductId, substituteProductId)
}

object Address : Table("address") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val line1 = varchar("line_1", 255)
    val line2 = varchar("line_2", 255).nullable()
    val city = varchar("city", 50)
    val postcode = varchar("postcode", 50)

    override val primaryKey = PrimaryKey(id)
}