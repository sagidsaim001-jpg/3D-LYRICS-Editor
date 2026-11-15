package com.saji.threedlyrics

import android.net.Uri
import android.graphics.Typeface
import android.graphics.Paint
import java.io.File
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Night = Color(0xFF090A12)
private val Panel = Color(0xFF151725)
private val Pink = Color(0xFFFF3DCE)
private val Violet = Color(0xFF8B5CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 10, 18)
        window.navigationBarColor = android.graphics.Color.rgb(9, 10, 18)
        setContent { EditorScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    val customFonts = remember { mutableStateListOf<String>().apply {
        val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
        dir.listFiles()?.filter { it.extension.lowercase() in listOf("ttf", "otf") }?.forEach { add(it.absolutePath) }
    } }
    var selectedFontPath by remember { mutableStateOf<String?>(null) }
    var fontMessage by remember { mutableStateOf("TTF / OTF supported") }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val dir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
            val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "CustomFont.ttf"
            require(name.substringAfterLast('.', "").lowercase() in listOf("ttf", "otf")) { "Only .TTF and .OTF files are supported" }
            val target = File(dir, "${System.currentTimeMillis()}_${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}")
            context.contentResolver.openInputStream(uri).use { input -> requireNotNull(input).use { source -> target.outputStream().use { source.copyTo(it) } } }
            Typeface.createFromFile(target)
            customFonts.add(target.absolutePath); selectedFontPath = target.absolutePath; fontMessage = "Imported: $name"
        }.onFailure { fontMessage = it.message ?: "Import failed" }
    }
    val layers = remember { mutableStateListOf("TERI YAADON MEIN", "SAJI STUDIO") }
    var selectedLayer by remember { mutableIntStateOf(0) }
    val lyric = layers.getOrElse(selectedLayer) { "" }
    var rotateX by remember { mutableFloatStateOf(18f) }
    var rotateY by remember { mutableFloatStateOf(-12f) }
    var depth by remember { mutableFloatStateOf(8f) }
    var textSize by remember { mutableFloatStateOf(34f) }
    var glow by remember { mutableFloatStateOf(18f) }
    var positionX by remember { mutableFloatStateOf(0f) }
    var positionY by remember { mutableFloatStateOf(0f) }
    var italic by remember { mutableStateOf(false) }
    var bold by remember { mutableStateOf(true) }
    var topLeft by remember { mutableFloatStateOf(0f) }
    var topRight by remember { mutableFloatStateOf(0f) }
    var bottomLeft by remember { mutableFloatStateOf(0f) }
    var bottomRight by remember { mutableFloatStateOf(0f) }
    var playhead by remember { mutableFloatStateOf(0f) }
    val keyframes = remember { mutableStateListOf<Float>() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            videoUri = uri
        }
    }

    val player = remember(videoUri) {
        videoUri?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(it))
                prepare()
                playWhenReady = false
            }
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Pink,
        secondary = Violet, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Night) { padding ->
            var activeTool by remember { mutableStateOf("TEXT") }
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SAJI STUDIO", color = Pink, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("3D LYRICS EDITOR", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(onClick = { picker.launch(arrayOf("video/*")) }, shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(5.dp))
                        Text(if (videoUri == null) "IMPORT VIDEO" else "CHANGE VIDEO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp, bottom = 7.dp)
                    .shadow(14.dp, RoundedCornerShape(16.dp))
                    .background(Color(0xFF101321), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF343047), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    if (player != null) {
                        AndroidView(factory = { ctx -> PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } }, modifier = Modifier.fillMaxSize())
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(Icons.Default.Movie, null, tint = Violet, modifier = Modifier.size(38.dp))
                            Text("Video preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Import a video to start editing", color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                    PerspectiveText(text = lyric.ifBlank { "YOUR LYRICS" }, modifier = Modifier.fillMaxSize(),
                        sizeSp = textSize, cornerOffsets = listOf(topLeft, topRight, bottomRight, bottomLeft),
                        glow = glow, bold = bold, italic = italic, fontPath = selectedFontPath,
                        rotationX = rotateX, rotationY = rotateY, translationX = positionX, translationY = positionY)
                }
                Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("TIMELINE", color = Pink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${playhead.toInt()}%  •  ${keyframes.size} keyframes", color = Color.LightGray, fontSize = 10.sp)
                        }
                        Slider(value = playhead, onValueChange = { playhead = it }, valueRange = 0f..100f,
                            modifier = Modifier.height(25.dp), colors = SliderDefaults.colors(thumbColor = Pink, activeTrackColor = Pink, inactiveTrackColor = Color(0xFF3B3A4C)))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFF171927), RoundedCornerShape(13.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("TEXT", "FONT", "3D", "KEYFRAMES").forEach { tool ->
                        val active = activeTool == tool
                        Surface(color = if (active) Color(0xFF452044) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f).clickable { activeTool = tool }) {
                            Column(Modifier.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(when(tool) { "TEXT" -> "T"; "FONT" -> "Aa"; "3D" -> "◇"; else -> "◆" },
                                    color = if (active) Pink else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(tool, color = if (active) Pink else Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().weight(0.92f).padding(top = 6.dp).verticalScroll(rememberScrollState())) {
                    when(activeTool) {
                        "TEXT" -> {
                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TextFields, null, tint = Pink)
                                Spacer(Modifier.width(8.dp))
                                Text("TEXT LAYERS", fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = {
                                layers.add("NEW LYRIC")
                                selectedLayer = layers.lastIndex
                            }) { Text("+ ADD LAYER", color = Pink) }
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            layers.forEachIndexed { index, item ->
                                Surface(
                                    color = if (selectedLayer == index) Color(0xFF452044) else Color(0xFF25283A),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { selectedLayer = index }
                                ) {
                                    Text("LAYER ${index + 1}", color = if (selectedLayer == index) Pink else Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = lyric, onValueChange = { layers[selectedLayer] = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Edit selected layer") },
                            minLines = 1, maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Pink, focusedLabelColor = Pink,
                                unfocusedBorderColor = Color(0xFF414357)
                            )
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(selected = bold, onClick = { bold = !bold }, label = { Text("Bold") })
                            FilterChip(selected = italic, onClick = { italic = !italic }, label = { Text("Italic") })
                            TextButton(enabled = layers.size > 1, onClick = {
                                layers.removeAt(selectedLayer)
                                selectedLayer = (selectedLayer - 1).coerceAtLeast(0)
                            }) { Text("DELETE LAYER", color = Color(0xFFFF7A8A)) }
                        }
                        Text("CUSTOM FONTS", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                                colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) { Text("+ IMPORT FONT") }
                            Text(fontMessage, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = if (selectedFontPath == null) Color(0xFF452044) else Color(0xFF25283A), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { selectedFontPath = null }) {
                                Text("DEFAULT", color = if (selectedFontPath == null) Pink else Color.LightGray, modifier = Modifier.padding(12.dp), fontSize = 11.sp)
                            }
                            customFonts.forEach { path ->
                                val label = File(path).name.substringAfter('_').substringBeforeLast('.')
                                Surface(color = if (selectedFontPath == path) Color(0xFF452044) else Color(0xFF25283A), shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { selectedFontPath = path }) {
                                    Text(label, color = if (selectedFontPath == path) Pink else Color.LightGray, modifier = Modifier.padding(12.dp), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                        }
                        "FONT" -> {
                            Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("CUSTOM FONT LIBRARY", color = Pink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Button(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) {
                                        Text("+ IMPORT TTF / OTF FONT")
                                    }
                                    Text(fontMessage, color = Color.LightGray, fontSize = 11.sp)
                                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(color = if (selectedFontPath == null) Color(0xFF452044) else Color(0xFF25283A),
                                            shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { selectedFontPath = null }) {
                                            Text("DEFAULT", color = if (selectedFontPath == null) Pink else Color.LightGray, modifier = Modifier.padding(12.dp), fontSize = 11.sp)
                                        }
                                        customFonts.forEach { path ->
                                            val label = File(path).name.substringAfter('_').substringBeforeLast('.')
                                            Surface(color = if (selectedFontPath == path) Color(0xFF452044) else Color(0xFF25283A),
                                                shape = RoundedCornerShape(10.dp), modifier = Modifier.clickable { selectedFontPath = path }) {
                                                Text(label, color = if (selectedFontPath == path) Pink else Color.LightGray, modifier = Modifier.padding(12.dp), fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "3D" -> {
                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = Pink)
                                Spacer(Modifier.width(8.dp))
                                Text("3D TRANSFORM & STYLE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            TextButton(onClick = {
                                rotateX = 18f; rotateY = -12f; depth = 8f; textSize = 34f; glow = 18f
                                positionX = 0f; positionY = 0f; italic = false; bold = true
                            }) { Text("RESET", color = Pink) }
                        }
                        SliderControl("Top-left corner", topLeft, -90f, 90f, " px") { topLeft = it }
                        SliderControl("Top-right corner", topRight, -90f, 90f, " px") { topRight = it }
                        SliderControl("Bottom-left corner", bottomLeft, -90f, 90f, " px") { bottomLeft = it }
                        SliderControl("Bottom-right corner", bottomRight, -90f, 90f, " px") { bottomRight = it }
                        SliderControl("Position X", positionX, -120f, 120f, " px") { positionX = it }
                        SliderControl("Position Y", positionY, -100f, 100f, " px") { positionY = it }
                        SliderControl("Rotate X", rotateX, -70f, 70f, "°") { rotateX = it }
                        SliderControl("Rotate Y", rotateY, -70f, 70f, "°") { rotateY = it }
                        SliderControl("3D Depth / Shadow", depth, 0f, 30f, "") { depth = it }
                        SliderControl("Text Size", textSize, 16f, 58f, "sp") { textSize = it }
                        SliderControl("Neon Glow", glow, 0f, 40f, "") { glow = it }
                    }
                }
                        }
                        "KEYFRAMES" -> {
                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("KEYFRAME TIMELINE", color = Pink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        SliderControl("Playhead", playhead, 0f, 100f, "%") { playhead = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { keyframes.add(playhead) }, colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) { Text("+ ADD KEYFRAME") }
                            Text("${keyframes.size} keyframes", color = Color.LightGray, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            keyframes.forEachIndexed { index, frame ->
                                Surface(color = Color(0xFF452044), shape = RoundedCornerShape(8.dp), modifier = Modifier.clickable { playhead = frame }) {
                                    Text("◆ ${frame.toInt()}%", color = Pink, modifier = Modifier.padding(9.dp), fontSize = 11.sp)
                                }
                            }
                        }
                        Text("Keyframes currently save timeline markers. Automatic motion interpolation is a later upgrade.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PerspectiveText(text: String, modifier: Modifier = Modifier, sizeSp: Float, cornerOffsets: List<Float>, glow: Float, bold: Boolean, italic: Boolean, rotationX: Float, rotationY: Float, translationX: Float, translationY: Float, fontPath: String?) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val typeface = remember(fontPath, bold, italic) {
        val style = when { bold && italic -> Typeface.BOLD_ITALIC; bold -> Typeface.BOLD; italic -> Typeface.ITALIC; else -> Typeface.NORMAL }
        runCatching { Typeface.create(fontPath?.let { Typeface.createFromFile(it) } ?: Typeface.DEFAULT, style) }.getOrElse { Typeface.create(Typeface.DEFAULT, style) }
    }
    Canvas(modifier.graphicsLayer {
        this.rotationX = rotationX; this.rotationY = rotationY
        translationX = translationX; translationY = translationY
        cameraDistance = 18f * density.density
    }) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; textSize = sizeSp * density.density
            textAlign = Paint.Align.CENTER; this.typeface = typeface
            setShadowLayer(glow.coerceAtLeast(0.1f), 0f, 0f, android.graphics.Color.rgb(255, 61, 206))
        }
        val lines = text.split("\\n")
        val lineHeight = paint.textSize * 1.18f
        val startY = size.height / 2f - (lines.size - 1) * lineHeight / 2f
        lines.forEachIndexed { index, line -> drawContext.canvas.nativeCanvas.drawText(line, size.width / 2f, startY + index * lineHeight, paint) }
    }
}

@Composable
private fun SliderControl(label: String, value: Float, min: Float, max: Float, suffix: String, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = Color(0xFFD7D5E0), fontSize = 13.sp)
            Text(value.toInt().toString() + suffix, color = Pink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValue, valueRange = min..max, colors = SliderDefaults.colors(
            thumbColor = Pink, activeTrackColor = Pink, inactiveTrackColor = Color(0xFF3B3A4C)
        ))
    }
}
