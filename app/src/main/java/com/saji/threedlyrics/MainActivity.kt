package com.saji.threedlyrics

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    var lyric by remember { mutableStateOf("TERI YAADON MEIN") }
    var rotateX by remember { mutableFloatStateOf(18f) }
    var rotateY by remember { mutableFloatStateOf(-12f) }
    var depth by remember { mutableFloatStateOf(8f) }
    var textSize by remember { mutableFloatStateOf(34f) }
    var glow by remember { mutableFloatStateOf(18f) }
    var showControls by remember { mutableStateOf(true) }

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
                        Text("BETA 01", color = Pink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Text(
                        text = lyric.ifBlank { "YOUR LYRICS" },
                        color = Pink,
                        fontSize = textSize.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        maxLines = 2,
                        modifier = Modifier.align(Alignment.Center)
                            .graphicsLayer {
                                rotationX = rotateX
                                rotationY = rotateY
                                cameraDistance = 18 * density
                                shadowElevation = depth
                            }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TextFields, null, tint = Pink)
                            Spacer(Modifier.width(8.dp))
                            Text("LYRICS TEXT", fontWeight = FontWeight.Bold)
                        }
                        OutlinedTextField(
                            value = lyric, onValueChange = { lyric = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter your lyrics") },
                            minLines = 1, maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Pink, focusedLabelColor = Pink,
                                unfocusedBorderColor = Color(0xFF414357)
                            )
                        )
                    }
                }

                Surface(color = Panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, null, tint = Pink)
                                Spacer(Modifier.width(8.dp))
                                Text("3D PERSPECTIVE CONTROLS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            TextButton(onClick = {
                                rotateX = 18f; rotateY = -12f; depth = 8f; textSize = 34f; glow = 18f
                            }) { Text("RESET", color = Pink) }
                        }
                        SliderControl("Rotate X", rotateX, -70f, 70f, "°") { rotateX = it }
                        SliderControl("Rotate Y", rotateY, -70f, 70f, "°") { rotateY = it }
                        SliderControl("3D Depth", depth, 0f, 30f, "") { depth = it }
                        SliderControl("Text Size", textSize, 16f, 58f, "sp") { textSize = it }
                        SliderControl("Neon Glow", glow, 0f, 40f, "") { glow = it }
                    }
                }

                Surface(color = Color(0xFF21152A), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("FIRST BUILD • EDITOR FOUNDATION", color = Pink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(5.dp))
                        Text("Video import, live lyric overlay and adjustable 3D rotation are included. Perspective corner pinning, motion tracking and final MP4 rendering are planned for the next development phases.", color = Color(0xFFD6D1E3), fontSize = 12.sp, lineHeight = 18.sp)
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
