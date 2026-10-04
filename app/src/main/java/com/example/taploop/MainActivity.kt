package com.example.taploop

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager

private val Blue = Color(0xFF155EEF)
private val Canvas = Color(0xFFF1F4FA)
private val Ink = Color(0xFF17191F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { TapLoopApp(this) } }
    fun accessibilityEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}

@Composable
private fun TapLoopApp(activity: MainActivity) {
    var page by remember { mutableStateOf("Home") }
    var interval by remember { mutableFloatStateOf(500f) }
    var repeatLimit by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(activity.accessibilityEnabled()) }
    MaterialTheme(colorScheme = lightColorScheme(primary = Blue, background = Canvas, surface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Canvas) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "Home") { TextButton(onClick = { page = "Home" }) { Icon(Icons.Default.ArrowBack, null); Spacer(Modifier.width(6.dp)); Text("Back") } }
                    else { Text("TapLoop", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Ink) }
                    Spacer(Modifier.weight(1f)); Text("TAP LOOP", fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Bold)
                }
                when (page) {
                    "Home" -> HomeScreen(onStart = {
                        enabled = activity.accessibilityEnabled()
                        if (!enabled) { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return@HomeScreen }
                        if (!Settings.canDrawOverlays(activity)) activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}")))
                        else { OverlayService.intervalMs = interval.toLong(); OverlayService.repeatLimit = repeatLimit; ContextCompat.startForegroundService(activity, Intent(activity, OverlayService::class.java)) }
                    }, onPage = { page = it })
                    "Configuration" -> ConfigScreen(interval, { interval = it }, repeatLimit, { repeatLimit = it })
                    "Permission" -> PermissionScreen(enabled, { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, { activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${activity.packageName}"))) })
                    "Guide" -> GuideScreen()
                    "Skins" -> SkinsScreen()
                }
                Spacer(Modifier.weight(1f))
                NavigationBar(containerColor = Color.White) {
                    listOf("Home" to Icons.Default.Home, "Configuration" to Icons.Default.Tune, "Guide" to Icons.Default.MenuBook).forEach { (name, icon) ->
                        NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Icon(icon, null) }, label = { Text(name) })
                    }
                }
            }
        }
    }
}

@Composable private fun HomeScreen(onStart: () -> Unit, onPage: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            Button(onClick = onStart, modifier = Modifier.size(240.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("Start", fontSize = 36.sp, fontWeight = FontWeight.Bold) }
        }
        Text("GET STARTED", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard("Configuration", Icons.Default.Tune, Modifier.weight(1f).height(115.dp)) { onPage("Configuration") }
            HomeCard("Guide", Icons.Default.MenuBook, Modifier.weight(1f).height(115.dp)) { onPage("Guide") }
            HomeCard("Permissions", Icons.Default.Security, Modifier.weight(1f).height(115.dp)) { onPage("Permission") }
        }
        Text("MAKE IT YOURS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCard("Tap settings", Icons.Default.TouchApp, Modifier.weight(1f).height(115.dp)) { onPage("Configuration") }
            HomeCard("Skins", Icons.Default.Palette, Modifier.weight(1f).height(115.dp)) { onPage("Skins") }
            HomeCard("Safety", Icons.Default.Timer, Modifier.weight(1f).height(115.dp)) { onPage("Configuration") }
        }
        Text("TapLoop only taps while the floating control is running. Move the target circle over the spot you want to tap.", color = Color.Gray, modifier = Modifier.padding(vertical = 18.dp))
    }
}

@Composable private fun HomeCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, action: () -> Unit) {
    Card(onClick = action, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, tint = Ink, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontSize = 12.sp, color = Ink) }
    }
}

@Composable private fun ConfigScreen(interval: Float, onInterval: (Float) -> Unit, repeatLimit: Int, onLimit: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Tap configuration", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Choose a comfortable pace. You can stop at any time from the floating bar.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text("Tap interval", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("${interval.toLong()} ms between taps", color = Blue, modifier = Modifier.padding(top = 12.dp))
                Slider(value = interval, onValueChange = onInterval, valueRange = 100f..3000f, steps = 28)
                Text("Faster", color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("Stop after", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (repeatLimit == 0) "Keep running until you stop" else "$repeatLimit taps", color = Color.Gray, modifier = Modifier.padding(vertical = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 25, 100, 500).forEach { count -> FilterChip(selected = repeatLimit == count, onClick = { onLimit(count) }, label = { Text(if (count == 0) "Manual" else "$count") }) }
                }
            }
        }
        Text("Target position", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        Text("The target starts near the center. Drag the crosshair after you start the floating controls.", color = Color.Gray)
    }
}

@Composable private fun PermissionScreen(accessibility: Boolean, onAccessibility: () -> Unit, onOverlay: () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Permissions", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Android requires these permissions for a user-controlled tap overlay.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 20.dp))
        PermissionCard("Accessibility service", "Allows TapLoop to perform taps when you press Start. Screen content is not read.", accessibility, onAccessibility)
        Spacer(Modifier.height(16.dp))
        PermissionCard("Display over other apps", "Shows the movable start and stop control above your current app.", Settings.canDrawOverlays(androidx.compose.ui.platform.LocalContext.current), onOverlay)
        Text("You can revoke either permission in Android Settings at any time.", color = Color.Gray, modifier = Modifier.padding(top = 18.dp))
    }
}
@Composable private fun PermissionCard(title: String, body: String, granted: Boolean, action: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text(if (granted) "Ready" else "Needed", color = if (granted) Color(0xFF16803C) else Color(0xFFD9480F), fontWeight = FontWeight.Bold) }
        Text(body, color = Color.Gray, modifier = Modifier.padding(vertical = 14.dp))
        Button(onClick = action, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) { Text(if (granted) "Open settings" else "Enable in settings") }
    } }
}

@Composable private fun GuideScreen() {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Quick guide", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 20.dp))
        listOf("1. Enable the accessibility and overlay permissions.", "2. Tap Start. The floating control appears over your screen.", "3. Drag the crosshair onto the place you want tapped.", "4. Press ▶ to start. The control turns green while tapping.", "5. Press ■ or close the bar to stop immediately.", "Long press the bar to move it. Taps repeat at your chosen interval.").forEach { item -> Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { Text(item, modifier = Modifier.padding(18.dp), lineHeight = 24.sp) } }
    }
}

@Composable private fun SkinsScreen() {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Text("Choose a target", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Select a simple target style", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
        listOf("Blue crosshair", "Classic ring", "Minimal dot").forEachIndexed { i, title ->
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) { Text(listOf("⊕", "◎", "•")[i], fontSize = 38.sp, color = Blue); Spacer(Modifier.width(18.dp)); Text(title, fontWeight = FontWeight.SemiBold); Spacer(Modifier.weight(1f)); if (i == 0) Icon(Icons.Default.CheckCircle, null, tint = Blue) }
            }
        }
        Text("More target styles can be added as the app grows.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
    }
}
