package com.example.data

import androidx.room.*

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: String = "",
    val category: String = "Fridge", // "Fridge" or "Pantry"
    val addedDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "culinary_dishes")
data class CulinaryDish(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imagePath: String, // Local saved file path
    val addedDate: Long = System.currentTimeMillis(),
    val isTopDish: Boolean = false // Track user's top culinary dishes
)

@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val ingredients: String, // Multiline or formatted text
    val instructions: String, // Multiline or formatted text
    val suggestedBasedOn: String = "", // Pantry items used as basis
    val addedDate: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: String = "",
    val isPurchased: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)
