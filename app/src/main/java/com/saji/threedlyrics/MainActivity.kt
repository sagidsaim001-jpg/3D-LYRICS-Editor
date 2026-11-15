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
import androidx.compose.animation.core.*
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
private data class EditorKeyframe(
 val time: Float, val positionX: Float, val positionY: Float, val positionZ: Float,
 val rotateX: Float, val rotateY: Float, val rotateZ: Float, val scaleX: Float, val scaleY: Float,
 val textSize: Float, val textColor: Int, val textAlpha: Float, val glow: Float, val outlineWidth: Float, val animationStyle: String
)

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
    var rotateZ by remember { mutableFloatStateOf(0f) }
    var depth by remember { mutableFloatStateOf(8f) }
    var textSize by remember { mutableFloatStateOf(34f) }
    var glow by remember { mutableFloatStateOf(18f) }
    var positionX by remember { mutableFloatStateOf(0f) }
    var positionY by remember { mutableFloatStateOf(0f) }
    var positionZ by remember { mutableFloatStateOf(0f) }
    var scaleXValue by remember { mutableFloatStateOf(1f) }
    var scaleYValue by remember { mutableFloatStateOf(1f) }
    var textAlpha by remember { mutableFloatStateOf(1f) }
    var letterSpacing by remember { mutableFloatStateOf(0f) }
    var lineSpacing by remember { mutableFloatStateOf(1.18f) }
    var outlineWidth by remember { mutableFloatStateOf(0f) }
    var backgroundEnabled by remember { mutableStateOf(false) }
    var textAlignMode by remember { mutableStateOf("CENTER") }
    var animationSpeed by remember { mutableFloatStateOf(1f) }
    var italic by remember { mutableStateOf(false) }
    var bold by remember { mutableStateOf(true) }
    var textColor by remember { mutableStateOf(0xFFFFFFFF.toInt()) }
    var animationStyle by remember { mutableStateOf("NONE") }
    var topLeft by remember { mutableFloatStateOf(0f) }
    var topRight by remember { mutableFloatStateOf(0f) }
    var bottomLeft by remember { mutableFloatStateOf(0f) }
    var bottomRight by remember { mutableFloatStateOf(0f) }
    var playhead by remember { mutableFloatStateOf(0f) }
    val keyframes = remember { mutableStateListOf<EditorKeyframe>() }

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
                        glow = glow, bold = bold, italic = italic, fontPath = selectedFontPath, perspectiveDepth = depth, textColor = textColor, rotationZ = rotateZ, animationStyle = animationStyle,
                         positionZ = positionZ, scaleXValue = scaleXValue, scaleYValue = scaleYValue, textAlpha = textAlpha,
                         letterSpacing = letterSpacing, lineSpacing = lineSpacing, outlineWidth = outlineWidth,
                         backgroundEnabled = backgroundEnabled, textAlignMode = textAlignMode, animationSpeed = animationSpeed,
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
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .background(Color(0xFF171927), RoundedCornerShape(13.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    listOf("TEXT" to "T", "FONT" to "Aa", "STYLE" to "✦", "MOVE" to "↔", "3D" to "◇", "GLOW" to "☼", "COLOR" to "●", "ANIM" to "✧", "KEYFRAMES" to "◆").forEach { (tool, symbol) ->
                        val active = activeTool == tool
                        Surface(color = if (active) Color(0xFF452044) else Color.Transparent,
                            shape = RoundedCornerShape(9.dp), modifier = Modifier.width(65.dp).clickable { activeTool = tool }) {
                            Column(Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(symbol, color = if (active) Pink else Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(tool, color = if (active) Pink else Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().weight(0.92f).padding(top = 5.dp).verticalScroll(rememberScrollState())) {
                    when (activeTool) {
                        "TEXT" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TEXT LAYERS", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    TextButton(onClick = { layers.add("NEW LYRIC"); selectedLayer = layers.lastIndex },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("+ ADD", color = Pink, fontSize = 11.sp) }
                                }
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    layers.forEachIndexed { index, _ ->
                                        Surface(color = if (selectedLayer == index) Color(0xFF452044) else Color(0xFF25283A),
                                            shape = RoundedCornerShape(9.dp), modifier = Modifier.clickable { selectedLayer = index }) {
                                            Text("LAYER " + (index + 1), color = if (selectedLayer == index) Pink else Color.LightGray,
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                OutlinedTextField(value = lyric, onValueChange = { if (layers.isNotEmpty()) layers[selectedLayer] = it },
                                    modifier = Modifier.fillMaxWidth(), label = { Text("Selected lyric text") }, minLines = 1, maxLines = 3,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Pink, focusedLabelColor = Pink,
                                        unfocusedBorderColor = Color(0xFF414357)))
                                TextButton(enabled = layers.size > 1, onClick = {
                                    layers.removeAt(selectedLayer); selectedLayer = (selectedLayer - 1).coerceAtLeast(0)
                                }, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp)) {
                                    Text("DELETE SELECTED LAYER", color = Color(0xFFFF7A8A), fontSize = 10.sp)
                                }
                            }
                        }
                        "FONT" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("FONT LIBRARY", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) {
                                        Text("+ IMPORT TTF / OTF", fontSize = 10.sp)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(fontMessage, color = Color.LightGray, fontSize = 9.sp, modifier = Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Surface(color = if (selectedFontPath == null) Color(0xFF452044) else Color(0xFF25283A),
                                        shape = RoundedCornerShape(9.dp), modifier = Modifier.clickable { selectedFontPath = null }) {
                                        Text("DEFAULT", color = if (selectedFontPath == null) Pink else Color.LightGray,
                                            modifier = Modifier.padding(10.dp), fontSize = 10.sp)
                                    }
                                    customFonts.forEach { path ->
                                        val label = File(path).name.substringAfter('_').substringBeforeLast('.')
                                        Surface(color = if (selectedFontPath == path) Color(0xFF452044) else Color(0xFF25283A),
                                            shape = RoundedCornerShape(9.dp), modifier = Modifier.clickable { selectedFontPath = path }) {
                                            Text(label, color = if (selectedFontPath == path) Pink else Color.LightGray,
                                                modifier = Modifier.padding(10.dp), fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                        "STYLE" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("TEXT STYLE", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = bold, onClick = { bold = !bold }, label = { Text("Bold", fontSize = 11.sp) })
                                    FilterChip(selected = italic, onClick = { italic = !italic }, label = { Text("Italic", fontSize = 11.sp) })
                                }
                                SliderControl("Text size", textSize, 8f, 180f, " sp") { textSize = it }
                                SliderControl("Text opacity", textAlpha, 0f, 1f, "") { textAlpha = it }
                                SliderControl("Letter spacing", letterSpacing, -5f, 30f, "") { letterSpacing = it }
                                SliderControl("Line spacing", lineSpacing, 0.7f, 2.5f, "x") { lineSpacing = it }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("LEFT", "CENTER", "RIGHT").forEach { align ->
                                        FilterChip(selected = textAlignMode == align, onClick = { textAlignMode = align }, label = { Text(align, fontSize = 10.sp) })
                                    }
                                }
                                FilterChip(selected = backgroundEnabled, onClick = { backgroundEnabled = !backgroundEnabled }, label = { Text("Text background box") })
                            }
                        }
                        "MOVE" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("POSITION", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SliderControl("Horizontal", positionX, -1000000f, 1000000f, " px") { positionX = it }
                                SliderControl("Vertical", positionY, -1000000f, 1000000f, " px") { positionY = it }
                                SliderControl("Z position", positionZ, -100000f, 100000f, " px") { positionZ = it }
                                TextButton(onClick = { positionX = 0f; positionY = 0f },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("RESET POSITION", color = Pink, fontSize = 10.sp) }
                            }
                        }
                        "3D" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("3D PERSPECTIVE", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SliderControl("Rotate X", rotateX, -3600f, 3600f, "°") { rotateX = it }
                                SliderControl("Rotate Y", rotateY, -3600f, 3600f, "°") { rotateY = it }
                                SliderControl("Rotate Z", rotateZ, -3600f, 3600f, "°") { rotateZ = it }
                                SliderControl("Depth / Camera", depth, 0f, 10000f, "") { depth = it }
                                SliderControl("Scale X", scaleXValue, 0.05f, 8f, "x") { scaleXValue = it }
                                SliderControl("Scale Y", scaleYValue, 0.05f, 8f, "x") { scaleYValue = it }
                                TextButton(onClick = {
                                    rotateX = 18f; rotateY = -12f; rotateZ = 0f; depth = 8f
                                    positionX = 0f; positionY = 0f; positionZ = 0f
                                    scaleXValue = 1f; scaleYValue = 1f
                                },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) { Text("RESET 3D", color = Pink, fontSize = 10.sp) }
                            }
                        }
                        "GLOW" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("NEON GLOW", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SliderControl("Glow intensity", glow, 0f, 80f, "") { glow = it }
                                SliderControl("Outline width", outlineWidth, 0f, 20f, " px") { outlineWidth = it }
                                Text("Outline color follows the selected text color", color = Color.LightGray, fontSize = 9.sp)
                            }
                        }
                        "COLOR" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("TEXT COLOR", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                val palette = listOf(0xFFFFFFFF.toInt(), 0xFFFF3DCE.toInt(), 0xFF00E5FF.toInt(), 0xFFFFD740.toInt(), 0xFF7CFF6B.toInt(), 0xFFFF7043.toInt(), 0xFFB388FF.toInt(), 0xFF111111.toInt())
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    palette.forEach { colorValue ->
                                        Surface(color = Color(colorValue), shape = RoundedCornerShape(50),
                                            modifier = Modifier.size(34.dp).border(if (textColor == colorValue) 3.dp else 1.dp, if (textColor == colorValue) Pink else Color.Gray, RoundedCornerShape(50)).clickable { textColor = colorValue }) {}
                                    }
                                }
                                val red = (textColor shr 16) and 0xFF
                                val green = (textColor shr 8) and 0xFF
                                val blue = textColor and 0xFF
                                SliderControl("Red", red.toFloat(), 0f, 255f, "") { textColor = android.graphics.Color.rgb(it.toInt(), green, blue) }
                                SliderControl("Green", green.toFloat(), 0f, 255f, "") { textColor = android.graphics.Color.rgb(red, it.toInt(), blue) }
                                SliderControl("Blue", blue.toFloat(), 0f, 255f, "") { textColor = android.graphics.Color.rgb(red, green, it.toInt()) }
                            }
                        }
                        "ANIM" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("TEXT ANIMATION", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                listOf("NONE", "FADE", "ZOOM", "BOUNCE", "FLOAT", "SHAKE", "PULSE", "ROTATE", "SLIDE", "TYPEWRITER").chunked(3).forEach { presets ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        presets.forEach { preset ->
                                            FilterChip(selected = animationStyle == preset, onClick = { animationStyle = preset },
                                                label = { Text(preset, fontSize = 10.sp) })
                                        }
                                    }
                                }
                                SliderControl("Animation speed", animationSpeed, 0.2f, 4f, "x") { animationSpeed = it }
                                Text("Live preview presets: fade, zoom, bounce, float, shake, pulse, rotate, slide and typewriter.", color = Color.LightGray, fontSize = 9.sp)
                            }
                        }
                        "KEYFRAMES" -> Surface(color = Panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text("KEYFRAME MARKERS", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                SliderControl("Playhead", playhead, 0f, 100f, "%") { playhead = it }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        keyframes.removeAll { kotlin.math.abs(it.time - playhead) < 0.5f }
                                        keyframes.add(EditorKeyframe(playhead, positionX, positionY, positionZ, rotateX, rotateY, rotateZ,
                                            scaleXValue, scaleYValue, textSize, textColor, textAlpha, glow, outlineWidth, animationStyle))
                                        keyframes.sortBy { it.time }
                                    },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)) {
                                        Text("+ SAVE SNAPSHOT", fontSize = 10.sp)
                                    }
                                    Text(keyframes.size.toString() + " markers", color = Color.LightGray, fontSize = 10.sp)
                                }
                                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    keyframes.forEach { frame ->
                                        Surface(color = Color(0xFF452044), shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.clickable {
                                                playhead = frame.time
                                                positionX = frame.positionX; positionY = frame.positionY; positionZ = frame.positionZ
                                                rotateX = frame.rotateX; rotateY = frame.rotateY; rotateZ = frame.rotateZ
                                                scaleXValue = frame.scaleX; scaleYValue = frame.scaleY
                                                textSize = frame.textSize; textColor = frame.textColor; textAlpha = frame.textAlpha
                                                glow = frame.glow; outlineWidth = frame.outlineWidth; animationStyle = frame.animationStyle
                                            }) {
                                            Text("◆ " + frame.time.toInt() + "%", color = Pink, modifier = Modifier.padding(8.dp), fontSize = 10.sp)
                                        }
                                    }
                                }
                                Text("Markers only. Animated keyframe interpolation is not implemented yet.", color = Color.LightGray, fontSize = 9.sp)
                            }
                        }
                    }
                }
                    Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PerspectiveText(text: String, modifier: Modifier = Modifier, sizeSp: Float, cornerOffsets: List<Float>, glow: Float, bold: Boolean, italic: Boolean, rotationX: Float, rotationY: Float, translationX: Float, translationY: Float, fontPath: String?, perspectiveDepth: Float, textColor: Int, rotationZ: Float, animationStyle: String,
    positionZ: Float, scaleXValue: Float, scaleYValue: Float, textAlpha: Float, letterSpacing: Float,
    lineSpacing: Float, outlineWidth: Float, backgroundEnabled: Boolean, textAlignMode: String, animationSpeed: Float) {
    val transition = rememberInfiniteTransition(label = "lyricAnimation")
    val motion by transition.animateFloat(initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween((1800f / animationSpeed.coerceIn(0.2f, 4f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse), label = "motion")
    val animAlpha = textAlpha * (if (animationStyle == "FADE") 0.15f + motion * 0.85f else 1f)
    val animScale = when (animationStyle) {
        "ZOOM" -> 0.65f + motion * 0.5f
        "BOUNCE" -> 0.9f + motion * 0.2f
        "PULSE" -> 0.9f + motion * 0.22f
        "SLIDE" -> 0.95f + motion * 0.05f
        else -> 1f
    }
    val animY = when (animationStyle) {
        "FLOAT" -> (motion - 0.5f) * 70f
        "BOUNCE" -> -kotlin.math.abs(motion - 0.5f) * 55f
        "SLIDE" -> (0.5f - motion) * 220f
        else -> 0f
    }
    val animX = when (animationStyle) {
        "SHAKE" -> kotlin.math.sin(motion * 35f) * 12f
        "SLIDE" -> (motion - 0.5f) * 220f
        else -> 0f
    }
    val animatedRotation = rotationZ + if (animationStyle == "ROTATE") motion * 360f else 0f
    val density = androidx.compose.ui.platform.LocalDensity.current
    val typeface = remember(fontPath, bold, italic) {
        val style = when { bold && italic -> Typeface.BOLD_ITALIC; bold -> Typeface.BOLD; italic -> Typeface.ITALIC; else -> Typeface.NORMAL }
        runCatching { Typeface.create(fontPath?.let { Typeface.createFromFile(it) } ?: Typeface.DEFAULT, style) }.getOrElse { Typeface.create(Typeface.DEFAULT, style) }
    }
    Canvas(modifier.graphicsLayer {
        this.rotationX = rotationX; this.rotationY = rotationY; this.rotationZ = animatedRotation
        this.translationX = translationX + animX; this.translationY = translationY + animY
        this.translationZ = positionZ
        this.alpha = animAlpha; this.scaleX = scaleXValue * animScale; this.scaleY = scaleYValue * animScale
        cameraDistance = (8f + perspectiveDepth) * density.density
    }) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor; textSize = sizeSp * density.density
            textAlign = Paint.Align.CENTER; this.typeface = typeface
            setShadowLayer(glow.coerceAtLeast(0.1f), 0f, 0f, android.graphics.Color.rgb(255, 61, 206))
        }
        val displayText = if (animationStyle == "TYPEWRITER") {
            text.take((text.length * motion).toInt().coerceIn(0, text.length))
        } else text
        val lines = displayText.split("\n")
        val lineHeight = paint.textSize * lineSpacing
        val startY = size.height / 2f - (lines.size - 1) * lineHeight / 2f
        val maxWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val x = when (textAlignMode) {
            "LEFT" -> size.width * 0.08f
            "RIGHT" -> size.width * 0.92f
            else -> size.width / 2f
        }
        paint.textAlign = when (textAlignMode) {
            "LEFT" -> Paint.Align.LEFT
            "RIGHT" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        paint.letterSpacing = letterSpacing / 100f
        paint.alpha = (animAlpha.coerceIn(0f, 1f) * 255f).toInt()
        if (backgroundEnabled && maxWidth > 0f) {
            val left = when (textAlignMode) {
                "LEFT" -> x - 14f
                "RIGHT" -> x - maxWidth - 14f
                else -> x - maxWidth / 2f - 14f
            }
            val rect = android.graphics.RectF(left, startY - paint.textSize,
                left + maxWidth + 28f, startY + (lines.size - 1) * lineHeight + paint.textSize * 0.45f)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(190, 12, 12, 20)
            }
            drawContext.canvas.nativeCanvas.drawRoundRect(rect, 18f, 18f, bgPaint)
        }
        lines.forEachIndexed { index, line ->
            val y = startY + index * lineHeight
            if (outlineWidth > 0f) {
                val outline = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = outlineWidth
                    color = android.graphics.Color.BLACK
                }
                drawContext.canvas.nativeCanvas.drawText(line, x, y, outline)
            }
            drawContext.canvas.nativeCanvas.drawText(line, x, y, paint)
        }
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
