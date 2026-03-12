package com.supermarket
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseCreation {
    fun init() {
        // Creates a file named supermarket.db
        val db = Database.connect("jdbc:sqlite:/supermarket.db", "org.sqlite.JDBC")

        println("Database imported successfully!")
    }
}