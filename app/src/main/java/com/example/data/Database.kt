package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PantryDao {
    @Query("SELECT * FROM pantry_items ORDER BY addedDate DESC")
    fun getAllPantryItems(): Flow<List<PantryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryItem)

    @Delete
    suspend fun deletePantryItem(item: PantryItem)

    @Query("DELETE FROM pantry_items")
    suspend fun clearAllPantryItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItems(items: List<PantryItem>)
}

@Dao
interface CulinaryDishDao {
    @Query("SELECT * FROM culinary_dishes ORDER BY addedDate DESC")
    fun getAllCulinaryDishes(): Flow<List<CulinaryDish>>

    @Query("SELECT * FROM culinary_dishes WHERE isTopDish = 1 ORDER BY addedDate DESC")
    fun getTopCulinaryDishes(): Flow<List<CulinaryDish>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCulinaryDish(dish: CulinaryDish)

    @Delete
    suspend fun deleteCulinaryDish(dish: CulinaryDish)

    @Update
    suspend fun updateCulinaryDish(dish: CulinaryDish)
}

@Dao
interface SavedRecipeDao {
    @Query("SELECT * FROM saved_recipes ORDER BY addedDate DESC")
    fun getAllSavedRecipes(): Flow<List<SavedRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipe)

    @Delete
    suspend fun deleteSavedRecipe(recipe: SavedRecipe)
}

@Dao
interface GroceryDao {
    @Query("SELECT * FROM grocery_items ORDER BY addedDate DESC")
    fun getAllGroceryItems(): Flow<List<GroceryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItem(item: GroceryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroceryItems(items: List<GroceryItem>)

    @Update
    suspend fun updateGroceryItem(item: GroceryItem)

    @Delete
    suspend fun deleteGroceryItem(item: GroceryItem)

    @Query("DELETE FROM grocery_items")
    suspend fun clearAllGroceryItems()
}

@Database(
    entities = [PantryItem::class, CulinaryDish::class, SavedRecipe::class, GroceryItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pantryDao(): PantryDao
    abstract fun culinaryDishDao(): CulinaryDishDao
    abstract fun savedRecipeDao(): SavedRecipeDao
    abstract fun groceryDao(): GroceryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pantry_chef_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
