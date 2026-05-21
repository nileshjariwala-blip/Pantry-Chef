package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.AppThemePreset
import java.io.File

sealed class Screen(val route: String, val title: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    object Inventory : Screen("inventory", "Inventory", Icons.Filled.Home, Icons.Outlined.Home)
    object Culinary : Screen("culinary", "Culinary", Icons.Filled.Star, Icons.Outlined.Star)
    object Recipe : Screen("recipe", "Recipes", Icons.Filled.Favorite, Icons.Outlined.Favorite)
    object Grocery : Screen("grocery", "Groceries", Icons.AutoMirrored.Filled.List, Icons.Outlined.List)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation(viewModel: PantryViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Inventory) }

    // State collections
    val pantryItems by viewModel.pantryItems.collectAsStateWithLifecycle()
    val culinaryDishes by viewModel.culinaryDishes.collectAsStateWithLifecycle()
    val topDishes by viewModel.topCulinaryDishes.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val groceryItems by viewModel.groceryItems.collectAsStateWithLifecycle()

    // Loading streams
    val isScanningPantry by viewModel.isScanningPantry.collectAsStateWithLifecycle()
    val isSuggestingRecipe by viewModel.isSuggestingRecipe.collectAsStateWithLifecycle()
    val isGeneratingGrocery by viewModel.isGeneratingGrocery.collectAsStateWithLifecycle()

    // Notification states
    val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMsg by viewModel.successMessage.collectAsStateWithLifecycle()
    val activeThemePreset by viewModel.selectedTheme.collectAsStateWithLifecycle()

    // Capture Toast Alerts
    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrors()
        }
    }

    LaunchedEffect(successMsg) {
        successMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccess()
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }

    // Modern M3 slate styling & backgrounds
    val brandGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pantry Chef",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.testTag("palette_theme_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = "Switch theme palette",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                listOf(Screen.Inventory, Screen.Culinary, Screen.Recipe, Screen.Grocery).forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brandGradient)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Inventory -> InventoryScreen(
                        pantryItems = pantryItems,
                        isScanning = isScanningPantry,
                        onAddItem = { name, qty, cat -> viewModel.addManualPantryItem(name, qty, cat) },
                        onDeleteItem = { viewModel.deletePantryItem(it) },
                        onClearAll = { viewModel.clearAllPantry() },
                        onImageSelected = { uri -> viewModel.scanPantryImage(context, uri) }
                    )
                    Screen.Culinary -> CulinaryScreen(
                        dishes = culinaryDishes,
                        onAddDish = { title, uri ->
                            coroutineScope.launch {
                                val localPath = viewModel.saveImageToInternalStorage(context, uri, "culinary")
                                if (localPath != null) {
                                    viewModel.addNewCulinaryDish(title, localPath)
                                } else {
                                    Toast.makeText(context, "Failed to store culinary photo.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onToggleTop = { viewModel.toggleTopDishStatus(it) },
                        onDeleteDish = { viewModel.deleteCulinaryDish(it) }
                    )
                    Screen.Recipe -> RecipeScreen(
                        savedRecipes = savedRecipes,
                        pantryCount = pantryItems.size,
                        isSuggesting = isSuggestingRecipe,
                        onGenerateRecipe = { viewModel.generateRecipeSuggestion() },
                        onDeleteRecipe = { viewModel.deleteSavedRecipe(it) }
                    )
                    Screen.Grocery -> GroceryScreen(
                        groceryItems = groceryItems,
                        topDishesCount = topDishes.size,
                        isGenerating = isGeneratingGrocery,
                        onGenerateList = { viewModel.generateGroceryListFromCulinaryDishes() },
                        onTogglePurchased = { viewModel.toggleGroceryItemPurchased(it) },
                        onDeleteGroceryItem = { viewModel.deleteGroceryItem(it) },
                        onAddManualItem = { name, qty -> viewModel.addManualGroceryItem(name, qty) },
                        onClearAll = { viewModel.clearAllGrocery() }
                    )
                }
            }

            if (showThemeDialog) {
                Dialog(onDismissRequest = { showThemeDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Visual Theme Studio",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select from 5 curated food & kitchen design aesthetics to custom style your workspace.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (preset in AppThemePreset.values()) {
                                    val isSelected = activeThemePreset == preset
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            )
                                            .clickable { viewModel.selectTheme(preset) }
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .testTag("theme_preset_${preset.name}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy((-4).dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(preset.primary, CircleShape)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(preset.secondary, CircleShape)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .background(preset.background, CircleShape)
                                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = preset.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showThemeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Apply & Close")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. INVENTORY SCREEN (Fridge & Pantry)
// ==========================================

@Composable
fun InventoryScreen(
    pantryItems: List<PantryItem>,
    isScanning: Boolean,
    onAddItem: (String, String, String) -> Unit,
    onDeleteItem: (PantryItem) -> Unit,
    onClearAll: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedCategoryTab by remember { mutableStateOf("All") }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { onImageSelected(it) } }
    )

    // Camera Capture File Flow
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempCameraUri?.let { onImageSelected(it) }
            }
        }
    )

    fun startCameraCapture() {
        try {
            val file = File(context.cacheDir, "camera_pantry_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error starting camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredItems = when (selectedCategoryTab) {
        "Fridge" -> pantryItems.filter { it.category == "Fridge" }
        "Pantry" -> pantryItems.filter { it.category == "Pantry" }
        else -> pantryItems
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App Title Section with elegant asymmetric layout
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "My Food Inventory",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${pantryItems.size} items tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick reset button
            if (pantryItems.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear inventory",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Feature: Scan buttons card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Scan Fridge or Pantry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Snap a picture or upload from your device to automatically detect items utilizing Gemini AI.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isScanning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini is analyzing food ingredients...", fontSize = 13.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { startCameraCapture() },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_snap_fridge"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Snap Photo", fontSize = 13.sp)
                        }

                        ElevatedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_upload_fridge"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Gallery", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Tab Categories
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Fridge", "Pantry").forEach { tab ->
                val isSelected = selectedCategoryTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryTab = tab },
                    label = { Text(tab, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.testTag("tab_inv_$tab")
                )
            }
        }

        // List representation
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Dining,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your food list is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Add manually or scan a photo to populate local inventory.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Category Tag Indicator
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (item.category == "Fridge") Color(0xFF64B5F6) else Color(0xFFFFB74D)
                                        )
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.quantity.isNotEmpty()) {
                                        Text(
                                            text = "Qty: ${item.quantity}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(item.category, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                                }

                                IconButton(
                                    onClick = { onDeleteItem(item) },
                                    modifier = Modifier.testTag("delete_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Floating Action Button for manual entries
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
                .testTag("fab_add_item")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Item Manually")
        }
    }

    // Manual Entry Dialog
    if (showDialog) {
        var addName by remember { mutableStateOf("") }
        var addQty by remember { mutableStateOf("") }
        var addCategory by remember { mutableStateOf("Fridge") }

        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Food Item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Food Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_item_name"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = addQty,
                        onValueChange = { addQty = it },
                        label = { Text("Quantity (e.g. 2 bags, 1 lb)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_item_qty"),
                        singleLine = true
                    )

                    // Simple custom radio tab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Fridge", "Pantry").forEach { cat ->
                            val isSel = addCategory == cat
                            ElevatedCard(
                                onClick = { addCategory = cat },
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("select_cat_$cat")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        cat,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (addName.isNotBlank()) {
                                    onAddItem(addName, addQty, addCategory)
                                    showDialog = false
                                }
                            },
                            modifier = Modifier.testTag("btn_save_manual_item")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CULINARY GALLERY SCREEN
// ==========================================

@Composable
fun CulinaryScreen(
    dishes: List<CulinaryDish>,
    onAddDish: (String, Uri) -> Unit,
    onToggleTop: (CulinaryDish) -> Unit,
    onDeleteDish: (CulinaryDish) -> Unit
) {
    val context = LocalContext.current
    var titleInput by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Uri selection launchers
    var tempUrl by remember { mutableStateOf<Uri?>(null) }
    val selectUriLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selUri ->
                tempUrl = selUri
            }
        }
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                // Keep the URI saved in tempUrl
            }
        }
    )

    fun startCamera() {
        try {
            val file = File(context.cacheDir, "camera_dish_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempUrl = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error starting camera: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Culinary Cooked Dishes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${dishes.size} experiences logged",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active selection banner: Top 5 dishes
        val checkedCount = dishes.filter { it.isTopDish }.size
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (checkedCount == 5) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (checkedCount >= 5) Icons.Filled.Stars else Icons.Outlined.Stars,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Selected Dishes: $checkedCount / 5",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Check off up to 5 culinary dishes you want to cook. These are analyzed to compile missing ingredients in your groceries list!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Gallery List/Grid representation
        if (dishes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No culinary dishes logged",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Take photos of dishes you cook or love. Star your top 5 to generate automatic missing grocery stock!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dishes, key = { it.id }) { dish ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .testTag("recipe_photo_item_${dish.id}"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Local Photograph
                            AsyncImage(
                                model = File(dish.imagePath),
                                contentDescription = dish.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Glassmorphic title backdrop
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                                    .padding(8.dp)
                             ) {
                                Column {
                                    Text(
                                        text = dish.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Cooked dish experience",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Quick controls overlay (Star and Trash)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(6.dp)
                                    .align(Alignment.TopCenter),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Star Top Dish Toggle Button
                                IconButton(
                                    onClick = {
                                        if (!dish.isTopDish && checkedCount >= 5) {
                                            Toast.makeText(context, "Select maximum of 5 dishes for the Missing Ingredients algorithm.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onToggleTop(dish)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .testTag("toggle_top_dish_${dish.id}")
                                ) {
                                    Icon(
                                        imageVector = if (dish.isTopDish) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Star top culinary dish",
                                        tint = if (dish.isTopDish) Color(0xFFFFEB3B) else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete
                                IconButton(
                                    onClick = { onDeleteDish(dish) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .testTag("delete_dish_${dish.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete dish",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                titleInput = ""
                tempUrl = null
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
                .testTag("fab_add_dish")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Cooked Dish Log")
        }
    }

    if (showAddDialog) {
        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Log Cooked Culinary Dish",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Dish Title (e.g., Avocado Salmon Salad)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_dish_title")
                    )

                    // Image picker representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tempUrl != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = tempUrl,
                                    contentDescription = "Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                TextButton(
                                    onClick = { tempUrl = null },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                    colors = ButtonDefaults.textButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Text("Change", color = Color.White, fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Attach photograph of dish", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { startCamera() }, modifier = Modifier.testTag("btn_log_camera")) {
                                        Text("Camera", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { selectUriLauncher.launch("image/*") }, modifier = Modifier.testTag("btn_log_gallery")) {
                                        Text("Gallery", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val uri = tempUrl
                                if (titleInput.isNotBlank() && uri != null) {
                                    onAddDish(titleInput, uri)
                                    showAddDialog = false
                                } else {
                                    Toast.makeText(context, "Please enter a dish title and supply a picture.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("btn_save_dish_log")
                        ) {
                            Text("Log Dish")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. HEALTHY RECIPES SUGGESTER SCREEN
// ==========================================

@Composable
fun RecipeScreen(
    savedRecipes: List<SavedRecipe>,
    pantryCount: Int,
    isSuggesting: Boolean,
    onGenerateRecipe: () -> Unit,
    onDeleteRecipe: (SavedRecipe) -> Unit
) {
    var expandedRecipeId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = "Healthy Recipe Advisor",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Find smart healthy cuisines matching your fridge stock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Call-to-action bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Smart Recipe Cooker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Combines available food items in your inventory with your logged culinary dish history to output healthy recipe cards with detailed guide instructions.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isSuggesting) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Gemini is curating customized nutritious recipe...", fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onGenerateRecipe,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_get_recipe"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Draft Healthy Recipe", fontSize = 14.sp)
                    }
                }
            }
        }

        // Suggestions List
        Text(
            text = "Suggested Recipes History",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (savedRecipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No custom recipe cards yet",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Make sure your food inventory has items registered, then tap the Draft button above!",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedRecipes, key = { it.id }) { recipe ->
                    val isExpanded = expandedRecipeId == recipe.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedRecipeId = if (isExpanded) null else recipe.id
                            }
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = recipe.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (recipe.suggestedBasedOn.isNotEmpty()) {
                                            Text(
                                                text = "Based on: ${recipe.suggestedBasedOn}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onDeleteRecipe(recipe) },
                                        modifier = Modifier.testTag("delete_recipe_${recipe.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete recipe card",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isExpanded) {
                                Divider(modifier = Modifier.padding(vertical = 12.dp))

                                Text(
                                    text = "Required Ingredients",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = recipe.ingredients,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Text(
                                    text = "Preparation Guide",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    text = recipe.instructions,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. SMART GROCERY LIST SCREEN
// ==========================================

@Composable
fun GroceryScreen(
    groceryItems: List<GroceryItem>,
    topDishesCount: Int,
    isGenerating: Boolean,
    onGenerateList: () -> Unit,
    onTogglePurchased: (GroceryItem) -> Unit,
    onDeleteGroceryItem: (GroceryItem) -> Unit,
    onAddManualItem: (String, String) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var manualName by remember { mutableStateOf("") }
    var manualQty by remember { mutableStateOf("") }

    // Export Intent Share
    fun exportToNotes() {
        if (groceryItems.isEmpty()) {
            Toast.makeText(context, "Grocery planner list is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val textBody = "My Smart Grocery List:\n" + groceryItems.joinToString("\n") {
            val checkmark = if (it.isPurchased) "[x]" else "[ ]"
            "$checkmark ${it.name} ${if (it.quantity.isNotEmpty()) "(${it.quantity})" else ""}"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Pantry Chef - My Grocery Shopping List")
            putExtra(Intent.EXTRA_TEXT, textBody)
        }
        context.startActivity(Intent.createChooser(intent, "Export via Share Link / Message"))
    }

    // Direct Text SMS Intent Share
    fun sendSMS() {
        if (groceryItems.isEmpty()) {
            Toast.makeText(context, "Grocery list is empty.", Toast.LENGTH_SHORT).show()
            return
        }
        val textBody = "Pantry Chef Smart grocery list:\n" + groceryItems.joinToString("\n") {
            "- ${it.name} (${it.quantity})"
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("sms:")
                putExtra("sms_body", textBody)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to start standard messaging app. Exporting instead...", Toast.LENGTH_SHORT).show()
            exportToNotes()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Smart Grocery List",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${groceryItems.size} checklist items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (groceryItems.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear all groceries",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Smart Generator Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Missing Ingredient Estimator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = "Based on your top dishes choice ($topDishesCount starred) and current pantry inventory, Gemini will calculate standard recipe ingredients and populate a missing grocery list.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    lineHeight = 15.sp
                )

                if (isGenerating) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calculating recipe missing stocks...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                } else {
                    Button(
                        onClick = onGenerateList,
                        enabled = topDishesCount > 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_build_grocery"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Compile Missing Ingredients", fontSize = 13.sp)
                    }
                    if (topDishesCount == 0) {
                        Text(
                            text = "⚠ Mark at least one Cooked Dish under the Culinary tab first.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Quick Manual Add
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualName,
                onValueChange = { manualName = it },
                placeholder = { Text("Quick add item...", fontSize = 13.sp) },
                modifier = Modifier.weight(2f).height(50.dp).testTag("grocery_quick_name"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = manualQty,
                onValueChange = { manualQty = it },
                placeholder = { Text("Qty", fontSize = 13.sp) },
                modifier = Modifier.weight(1f).height(50.dp).testTag("grocery_quick_qty"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            IconButton(
                onClick = {
                    if (manualName.isNotBlank()) {
                        onAddManualItem(manualName, manualQty)
                        manualName = ""
                        manualQty = ""
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("btn_quick_add_grocery")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Grocery Item",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Grocery list
        if (groceryItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your grocery list is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Compile missing items from starred culinary dishes, or add quick items manually above.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groceryItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.isPurchased,
                                    onCheckedChange = { onTogglePurchased(item) },
                                    modifier = Modifier.testTag("check_grocery_${item.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (item.isPurchased) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                        style = if (item.isPurchased) MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.bodyMedium
                                    )
                                    if (item.quantity.isNotEmpty()) {
                                        Text(
                                            text = "Quantity: ${item.quantity}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDeleteGroceryItem(item) },
                                modifier = Modifier.testTag("delete_grocery_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Export Actions Card
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { exportToNotes() },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_export_notes"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export/Share List", fontSize = 12.sp)
                }

                Button(
                    onClick = { sendSMS() },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_send_sms"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send to Mobile", fontSize = 12.sp)
                }
            }
        }
    }
}
