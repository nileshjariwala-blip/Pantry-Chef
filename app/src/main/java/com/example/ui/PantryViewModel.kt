package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.theme.AppThemePreset
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PantryViewModel(private val repository: PantryRepository) : ViewModel() {

    // --- Interactive Theme Configuration ---
    private val _selectedTheme = MutableStateFlow(AppThemePreset.BOTANICAL_JADE)
    val selectedTheme: StateFlow<AppThemePreset> = _selectedTheme.asStateFlow()

    fun selectTheme(themePreset: AppThemePreset) {
        _selectedTheme.value = themePreset
    }

    // --- State Streams ---
    val pantryItems: StateFlow<List<PantryItem>> = repository.allPantryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val culinaryDishes: StateFlow<List<CulinaryDish>> = repository.allCulinaryDishes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCulinaryDishes: StateFlow<List<CulinaryDish>> = repository.topCulinaryDishes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRecipes: StateFlow<List<SavedRecipe>> = repository.allSavedRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groceryItems: StateFlow<List<GroceryItem>> = repository.allGroceryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Loading & Notification States ---
    private val _isScanningPantry = MutableStateFlow(false)
    val isScanningPantry: StateFlow<Boolean> = _isScanningPantry.asStateFlow()

    private val _isSuggestingRecipe = MutableStateFlow(false)
    val isSuggestingRecipe: StateFlow<Boolean> = _isSuggestingRecipe.asStateFlow()

    private val _isGeneratingGrocery = MutableStateFlow(false)
    val isGeneratingGrocery: StateFlow<Boolean> = _isGeneratingGrocery.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // --- Operations ---

    fun clearErrors() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun addManualPantryItem(name: String, quantity: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPantryItem(
                PantryItem(name = name.trim(), quantity = quantity.trim(), category = category)
            )
        }
    }

    fun deletePantryItem(item: PantryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePantryItem(item)
        }
    }

    fun clearAllPantry() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllPantryItems()
        }
    }

    /**
     * Copy an image from external content Uri to app's private filesDir for persistence.
     */
    suspend fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String = "img"): String? =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
                    val file = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(file)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    file.absolutePath
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Scan image of pantry/fridge via Gemini Vision.
     */
    fun scanPantryImage(context: Context, uri: Uri) {
        _isScanningPantry.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Save image locally to hold reference
                val localPath = saveImageToInternalStorage(context, uri, "fridge")
                if (localPath == null) {
                    _errorMessage.value = "Failed to copy image to local. Make sure file exists."
                    _isScanningPantry.value = false
                    return@launch
                }

                // Load and resize bitmap
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Safe scale down first
                }
                var rawBitmap = BitmapFactory.decodeFile(localPath, options)
                if (rawBitmap == null) {
                    _errorMessage.value = "Failed to decode image file."
                    _isScanningPantry.value = false
                    return@launch
                }

                rawBitmap = resizeBitmap(rawBitmap, maxDimension = 1024)

                // Call Gemini Vision Client
                val detected = GeminiClient.identifyPantryItems(rawBitmap)
                if (detected.isNotEmpty()) {
                    val entityItems = detected.map {
                        PantryItem(
                            name = it.name,
                            quantity = it.quantity,
                            category = it.category
                        )
                    }
                    repository.insertPantryItems(entityItems)
                    _successMessage.value = "Identified ${entityItems.size} items in image! Added to inventory."
                } else {
                    _errorMessage.value = "Gemini identified no items. Please provide a clearer picture or configure Gemini API key."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Scan failed: ${e.localizedMessage}"
            } finally {
                _isScanningPantry.value = false
            }
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (ratio > 1) {
            Pair(maxDimension, (maxDimension / ratio).toInt())
        } else {
            Pair((maxDimension * ratio).toInt(), maxDimension)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // --- Culinary Dish Log ---

    fun addNewCulinaryDish(title: String, localPath: String) {
        if (title.isBlank() || localPath.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCulinaryDish(
                CulinaryDish(
                    title = title.trim(),
                    imagePath = localPath
                )
            )
        }
    }

    fun toggleTopDishStatus(dish: CulinaryDish) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = dish.copy(isTopDish = !dish.isTopDish)
            repository.updateCulinaryDish(updated)
        }
    }

    fun deleteCulinaryDish(dish: CulinaryDish) {
        viewModelScope.launch(Dispatchers.IO) {
            // Delete actual image file if helpful
            try {
                val file = File(dish.imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteCulinaryDish(dish)
        }
    }

    // --- Suggest Recipe ---

    fun generateRecipeSuggestion() {
        _isSuggestingRecipe.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val activePantry = pantryItems.value
                val experiences = culinaryDishes.value

                if (activePantry.isEmpty()) {
                    _errorMessage.value = "Pantry is empty. Please add or scan ingredients first!"
                    _isSuggestingRecipe.value = false
                    return@launch
                }

                val recipeData = GeminiClient.suggestHealthyRecipe(activePantry, experiences)
                if (recipeData != null) {
                    val entity = SavedRecipe(
                        title = recipeData.title,
                        ingredients = recipeData.ingredients.joinToString("\n• ", prefix = "• "),
                        instructions = recipeData.instructions.joinToString("\n"),
                        suggestedBasedOn = activePantry.take(5).joinToString(", ") { it.name }
                    )
                    repository.insertSavedRecipe(entity)
                    _successMessage.value = "Healthy recipe suggestion generated successfully!"
                } else {
                    _errorMessage.value = "Failed to suggest recipe. Make sure Gemini API Key is configured."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Recipe generation failed: ${e.localizedMessage}"
            } finally {
                _isSuggestingRecipe.value = false
            }
        }
    }

    fun deleteSavedRecipe(recipe: SavedRecipe) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSavedRecipe(recipe)
        }
    }

    // --- Smart Grocery List Generation (Feature 2) ---

    fun generateGroceryListFromCulinaryDishes() {
        _isGeneratingGrocery.value = true
        _errorMessage.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val topDishes = topCulinaryDishes.value
                val pantry = pantryItems.value

                if (topDishes.isEmpty()) {
                    _errorMessage.value = "No culinary dishes are selected as 'Top Dishes'. Please mark dishes in Culinary gallery."
                    _isGeneratingGrocery.value = false
                    return@launch
                }

                val missing = GeminiClient.generateMissingGroceryList(topDishes, pantry)
                if (missing.isNotEmpty()) {
                    val list = missing.map {
                        GroceryItem(
                            name = it.name,
                            quantity = it.quantity,
                            isPurchased = false
                        )
                    }
                    repository.insertGroceryItems(list)
                    _successMessage.value = "Generated grocery list with ${list.size} missing items."
                } else {
                    _errorMessage.value = "All ingredients already present in your fridge/pantry, or API key error."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Grocery generator failed: ${e.localizedMessage}"
            } finally {
                _isGeneratingGrocery.value = false
            }
        }
    }

    fun addManualGroceryItem(name: String, quantity: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGroceryItem(
                GroceryItem(name = name.trim(), quantity = quantity.trim())
            )
        }
    }

    fun toggleGroceryItemPurchased(item: GroceryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = item.copy(isPurchased = !item.isPurchased)
            repository.updateGroceryItem(updated)
        }
    }

    fun deleteGroceryItem(item: GroceryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroceryItem(item)
        }
    }

    fun clearAllGrocery() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllGroceryItems()
        }
    }
}

class PantryViewModelFactory(private val repository: PantryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PantryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
