package com.fontpreview.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FontEntry(val name: String, val uri: Uri)

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("font_viewer_prefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme {
                FontViewerScreen(
                    savedFolderUri = prefs.getString("folder_uri", null),
                    onFolderSaved = { uri ->
                        prefs.edit().putString("folder_uri", uri.toString()).apply()
                    },
                    savedFavorites = prefs.getStringSet("favorites", emptySet()) ?: emptySet(),
                    onFavoritesSaved = { favs ->
                        prefs.edit().putStringSet("favorites", favs).apply()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontViewerScreen(
    savedFolderUri: String?,
    onFolderSaved: (Uri) -> Unit,
    savedFavorites: Set<String>,
    onFavoritesSaved: (Set<String>) -> Unit
) {
    val context = LocalContext.current

    var folderUri by remember { mutableStateOf(savedFolderUri?.let { Uri.parse(it) }) }
    var fonts by remember { mutableStateOf(listOf<FontEntry>()) }
    var selectedFont by remember { mutableStateOf<FontEntry?>(null) }
    var typeface by remember { mutableStateOf<Typeface?>(null) }
    var previewText by remember {
        mutableStateOf("El veloz murciélago hindú comía feliz cardillo y kiwi. 0123456789")
    }
    var previewFontSize by remember { mutableStateOf(24f) }
    var favorites by remember { mutableStateOf(savedFavorites) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Lista que efectivamente se muestra y por la que navegan las flechas
    val displayedFonts = remember(fonts, favorites, showOnlyFavorites) {
        if (showOnlyFavorites) fonts.filter { favorites.contains(it.uri.toString()) } else fonts
    }

    fun toggleFavorite(font: FontEntry) {
        val key = font.uri.toString()
        val newFavorites = if (favorites.contains(key)) {
            favorites - key
        } else {
            favorites + key
        }
        favorites = newFavorites
        onFavoritesSaved(newFavorites)
    }

    fun selectByOffset(offset: Int) {
        if (displayedFonts.isEmpty()) return
        val currentIndex = displayedFonts.indexOf(selectedFont)
        val nextIndex = if (currentIndex == -1) {
            0
        } else {
            ((currentIndex + offset) % displayedFonts.size + displayedFonts.size) % displayedFonts.size
        }
        selectedFont = displayedFonts[nextIndex]
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedFont = null
            typeface = null
            folderUri = uri
            onFolderSaved(uri)
        }
    }

    // Escanea la carpeta elegida en busca de archivos .ttf / .otf
    LaunchedEffect(folderUri) {
        val uri = folderUri ?: return@LaunchedEffect
        loading = true
        errorMsg = null
        fonts = withContext(Dispatchers.IO) {
            try {
                val tree = DocumentFile.fromTreeUri(context, uri)
                tree?.listFiles()
                    ?.filter { doc ->
                        doc.isFile && (
                            doc.name?.endsWith(".ttf", ignoreCase = true) == true ||
                                doc.name?.endsWith(".otf", ignoreCase = true) == true
                            )
                    }
                    ?.map { FontEntry(it.name ?: "sin nombre", it.uri) }
                    ?.sortedBy { it.name.lowercase() }
                    ?: emptyList()
            } catch (e: Exception) {
                errorMsg = "No se pudo leer la carpeta: ${e.message}"
                emptyList()
            }
        }
        loading = false
    }

    // Carga el Typeface de la fuente seleccionada
    LaunchedEffect(selectedFont) {
        val font = selectedFont ?: return@LaunchedEffect
        typeface = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(font.uri, "r")?.use { pfd ->
                    Typeface.Builder(pfd.fileDescriptor).build()
                }
            } catch (e: Exception) {
                errorMsg = "No se pudo cargar la fuente: ${e.message}"
                null
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Visor de Tipografías") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Button(onClick = { folderPicker.launch(null) }) {
                Text(if (folderUri == null) "Elegir carpeta de fuentes" else "Cambiar carpeta")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = previewText,
                onValueChange = { previewText = it },
                label = { Text("Texto de prueba") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Control de tamaño de letra de la vista previa
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tamaño: ${previewFontSize.toInt()}sp", modifier = Modifier.width(90.dp))
                Slider(
                    value = previewFontSize,
                    onValueChange = { previewFontSize = it },
                    valueRange = 10f..60f,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filtro de favoritos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Solo favoritos", modifier = Modifier.weight(1f))
                Switch(checked = showOnlyFavorites, onCheckedChange = { showOnlyFavorites = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            errorMsg?.let {
                Text(it, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

            selectedFont?.let { font ->
                val isFavorite = favorites.contains(font.uri.toString())
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                font.name,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { toggleFavorite(font) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Marcar como favorita",
                                    tint = if (isFavorite) Color(0xFFE53935) else Color.Gray
                                )
                            }
                            IconButton(onClick = { selectByOffset(-1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Fuente anterior")
                            }
                            IconButton(onClick = { selectByOffset(1) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Fuente siguiente")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = previewText,
                            fontSize = previewFontSize.sp,
                            fontFamily = typeface?.let { FontFamily(it) } ?: FontFamily.Default
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (loading) {
                Text("Cargando fuentes...")
            }

            if (!loading && folderUri != null && displayedFonts.isEmpty()) {
                Text(
                    if (showOnlyFavorites) "Todavía no marcaste ninguna fuente como favorita."
                    else "No se encontraron archivos .ttf u .otf en esta carpeta."
                )
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(displayedFonts) { font ->
                    val isFavorite = favorites.contains(font.uri.toString())
                    ListItem(
                        headlineContent = { Text(font.name) },
                        trailingContent = {
                            IconButton(onClick = { toggleFavorite(font) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Marcar como favorita",
                                    tint = if (isFavorite) Color(0xFFE53935) else Color.Gray
                                )
                            }
                        },
                        modifier = Modifier.clickable { selectedFont = font }
                    )
                    Divider()
                }
            }
        }
    }
}
