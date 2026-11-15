package com.saji.threedlyrics

import android.net.Uri
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
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("SAJI STUDIO", color = Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("3D LYRICS Editor", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(color = Color(0xFF25152B), shape = RoundedCornerShape(50)) {
                        Text("BETA 02", color = Pink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                        .shadow(18.dp, RoundedCornerShape(22.dp))
                        .background(Color(0xFF101321), RoundedCornerShape(22.dp))
                        .border(1.dp, Color(0xFF343047), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (player != null) {
                        AndroidView(
                            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Movie, null, tint = Violet, modifier = Modifier.size(42.dp))
                            Text("Your video preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Import a clip to start creating", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                    PerspectiveText(
                        text = lyric.ifBlank { "YOUR LYRICS" },
                        modifier = Modifier.fillMaxSize(),
                        sizeSp = textSize,
                        cornerOffsets = listOf(topLeft, topRight, bottomRight, bottomLeft),
                        glow = glow,
                        bold = bold,
                        italic = italic,
                        rotationX = rotateX,
                        rotationY = rotateY,
                        translationX = positionX,
                        translationY = positionY
                    )
                }

                Button(
                    onClick = { picker.launch(arrayOf("video/*")) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Pink, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (videoUri == null) "IMPORT VIDEO" else "CHANGE VIDEO", fontWeight = FontWeight.ExtraBold)
                }

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
                    }
                }

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

                Surface(color = Color(0xFF21152A), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("BETA 03 • CORNER PERSPECTIVE & KEYFRAME MARKERS", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        Text("Added editable corner pinning controls, multi-layer lyrics, styling and timeline keyframe markers. Keyframe interpolation, motion tracking and MP4 export are still pending.", color = Color(0xFFD6D1E3), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
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
