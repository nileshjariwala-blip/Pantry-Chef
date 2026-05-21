package com.example.data

import kotlinx.coroutines.flow.Flow

class PantryRepository(private val database: AppDatabase) {
    val pantryDao = database.pantryDao()
    val culinaryDishDao = database.culinaryDishDao()
    val savedRecipeDao = database.savedRecipeDao()
    val groceryDao = database.groceryDao()

    // Pantry Items
    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()
    suspend fun insertPantryItem(item: PantryItem) = pantryDao.insertPantryItem(item)
    suspend fun insertPantryItems(items: List<PantryItem>) = pantryDao.insertPantryItems(items)
    suspend fun deletePantryItem(item: PantryItem) = pantryDao.deletePantryItem(item)
    suspend fun clearAllPantryItems() = pantryDao.clearAllPantryItems()

    // Culinary Dishes
    val allCulinaryDishes: Flow<List<CulinaryDish>> = culinaryDishDao.getAllCulinaryDishes()
    val topCulinaryDishes: Flow<List<CulinaryDish>> = culinaryDishDao.getTopCulinaryDishes()
    suspend fun insertCulinaryDish(dish: CulinaryDish) = culinaryDishDao.insertCulinaryDish(dish)
    suspend fun deleteCulinaryDish(dish: CulinaryDish) = culinaryDishDao.deleteCulinaryDish(dish)
    suspend fun updateCulinaryDish(dish: CulinaryDish) = culinaryDishDao.updateCulinaryDish(dish)

    // Saved Recipes
    val allSavedRecipes: Flow<List<SavedRecipe>> = savedRecipeDao.getAllSavedRecipes()
    suspend fun insertSavedRecipe(recipe: SavedRecipe) = savedRecipeDao.insertSavedRecipe(recipe)
    suspend fun deleteSavedRecipe(recipe: SavedRecipe) = savedRecipeDao.deleteSavedRecipe(recipe)

    // Grocery Items
    val allGroceryItems: Flow<List<GroceryItem>> = groceryDao.getAllGroceryItems()
    suspend fun insertGroceryItem(item: GroceryItem) = groceryDao.insertGroceryItem(item)
    suspend fun insertGroceryItems(items: List<GroceryItem>) = groceryDao.insertGroceryItems(items)
    suspend fun updateGroceryItem(item: GroceryItem) = groceryDao.updateGroceryItem(item)
    suspend fun deleteGroceryItem(item: GroceryItem) = groceryDao.deleteGroceryItem(item)
    suspend fun clearAllGroceryItems() = groceryDao.clearAllGroceryItems()
}
