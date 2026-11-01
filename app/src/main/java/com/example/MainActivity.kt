package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import com.example.data.api.RealLocationHelper
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbSunny
import com.example.ui.screens.OmniAiScreen
import com.example.ui.screens.OmniSplashScreen
import com.example.ui.theme.CyberGlowAmber
import com.example.ui.theme.CyberGlowCyan
import com.example.ui.theme.CyberOrbGradient
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DailyHubScreen
import com.example.ui.screens.InteractiveRouteScreen
import com.example.ui.screens.MerchantHubScreen
import com.example.ui.screens.SmartListScreen
import com.example.ui.screens.StoreDetailScreen
import com.example.ui.screens.StoresListScreen
import com.example.ui.screens.TripPlannerScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.WeatherForecastScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(2600)
                showSplash = false
            }

            MyApplicationTheme(themeMode = themeMode) {
                Crossfade(targetState = showSplash, label = "omni-launch-transition") { splash ->
                    if (splash) OmniSplashScreen() else OmniApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLiveMode by viewModel.isLiveRealTimeMode.collectAsState()
    val activeTheme by viewModel.themeMode.collectAsState()
    val liveWeather by viewModel.liveWeatherData.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    val currentCity by viewModel.currentCity.collectAsState()
    val displayLocality = currentCity.ifBlank { "Córdoba" }
    val currentTemp = liveWeather?.let { "${it.temperatureC.toInt()}°C" } ?: "--°C"
    val currentWeatherSymbol = liveWeather?.let { weather ->
        when {
            weather.precipitationMm >= 0.1 -> "🌧"
            weather.weatherCode in listOf(95, 96, 99) -> "⛈"
            weather.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> "🌧"
            weather.weatherCode in listOf(45, 48) -> "🌫"
            weather.weatherCode == 3 -> "☁"
            weather.weatherCode in listOf(1, 2) -> "🌤"
            weather.isDay -> "☀"
            else -> "🌙"
        }
    } ?: "🌡"

    var showLocationDialog by remember { mutableStateOf(false) }
    var customCityInput by remember(displayLocality) { mutableStateOf(displayLocality) }

    // Solicitud automática del diálogo nativo de permisos GPS en el primer inicio
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) {
            viewModel.refreshLiveGpsAndPlaces(context)
        }
    }

    LaunchedEffect(Unit) {
        val restoredSession = viewModel.restoreSavedLocation(context)
        if (!RealLocationHelper.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!restoredSession) {
            viewModel.refreshLiveGpsAndPlaces(context)
        }
    }

    // Manejo de navegación hacia atrás en subpantallas
    BackHandler(
        enabled = currentScreen is Screen.StoreDetail ||
                currentScreen is Screen.InteractiveRoute ||
                currentScreen is Screen.Comparison ||
                currentScreen is Screen.MerchantHub ||
                currentScreen is Screen.Wallet ||
                currentScreen is Screen.Contacts ||
                currentScreen is Screen.OmniAi ||
                currentScreen is Screen.UserProfile ||
                currentScreen is Screen.Weather
    ) {
        when (currentScreen) {
            is Screen.InteractiveRoute -> viewModel.navigateTo(Screen.StoreDetail)
            is Screen.StoreDetail -> viewModel.navigateTo(Screen.StoresList)
            is Screen.Comparison -> viewModel.navigateTo(Screen.SmartList)
            is Screen.MerchantHub -> viewModel.navigateTo(Screen.UserProfile)
            is Screen.Wallet -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Contacts -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.OmniAi -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Weather -> viewModel.navigateTo(Screen.DailyHub)
            else -> viewModel.navigateTo(Screen.DailyHub)
        }
    }

    val isFullScreenSubpage = currentScreen is Screen.StoreDetail ||
            currentScreen is Screen.InteractiveRoute ||
            currentScreen is Screen.MerchantHub

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = {
                Text(text = "📍 Configurar Ubicación", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Escribe tu localidad manualmente o detecta automáticamente si te trasladaste.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customCityInput,
                        onValueChange = { customCityInput = it },
                        label = { Text("Tu ciudad o barrio") },
                        placeholder = { Text("Ej: Río Cuarto, Carlos Paz, Rosario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Ciudades Frecuentes:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Córdoba", "Buenos Aires", "Rosario", "Mendoza").forEach { quickCity ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        customCityInput = quickCity
                                        viewModel.setManualLocation(quickCity, context)
                                        showLocationDialog = false
                                    }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                    Text(quickCity.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            viewModel.refreshLiveGpsAndPlaces(context)
                            showLocationDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🛰️ Detectar mi GPS en Vivo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customCityInput.isNotBlank()) {
                            viewModel.setManualLocation(customCityInput, context)
                        }
                        showLocationDialog = false
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Fijar Ubicación", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("OMNI BETA", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Text("Elegí tu experiencia", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    listOf(
                        AppThemeMode.MIDNIGHT_PURPLE,
                        AppThemeMode.CLEAN_MINIMAL,
                        AppThemeMode.SOLAR_EMBER,
                        AppThemeMode.NEON_EMERALD
                    ).forEach { option ->
                        val selected = activeTheme == option
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(13.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                viewModel.setThemeMode(option)
                                showThemeDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(13.dp).background(option.previewPrimary, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(option.title, fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                    Text(option.subtitle, fontSize = 10.sp,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cerrar") } }
        )
    }

    Scaffold(
        topBar = {
            if (!isFullScreenSubpage) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(13.dp),
                                modifier = Modifier.size(39.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "O",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 25.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(9.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        customCityInput = displayLocality
                                        showLocationDialog = true
                                    },
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "OMNI",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp,
                                        letterSpacing = 1.4.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text(
                                            text = "BETA",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "📍 $displayLocality",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "$currentWeatherSymbol $currentTemp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        maxLines = 1,
                                        modifier = Modifier.clickable { viewModel.navigateTo(Screen.Weather) }
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        // Comparador Inteligente de Canasta
                        IconButton(onClick = { viewModel.navigateTo(Screen.Comparison) }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Comparador de Canasta",
                                tint = if (currentScreen is Screen.Comparison) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Acceso rápido a Billetera Virtual OmniPay
                        IconButton(onClick = { viewModel.navigateTo(Screen.Wallet) }) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Billetera Virtual",
                                tint = if (currentScreen is Screen.Wallet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Menú camuflado para Modo En Vivo / Simulación y Selección de Tema
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Más Opciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (isLiveMode) "📡 Modo: En Vivo (APIs Reales)" else "🧪 Modo: Simulación")
                                    },
                                    onClick = {
                                        viewModel.toggleLiveRealTimeMode()
                                        overflowMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎨 Elegir apariencia · ${activeTheme.title}") },
                                    onClick = {
                                        showThemeDialog = true
                                        overflowMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text("📞 Directorio Telefónico Barrial")
                                    },
                                    onClick = {
                                        viewModel.navigateTo(Screen.Contacts)
                                        overflowMenuExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            if (!isFullScreenSubpage) {
                CyberBottomDock(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                when (screen) {
                    Screen.DailyHub -> DailyHubScreen(viewModel = viewModel)
                    Screen.Weather -> WeatherForecastScreen(viewModel = viewModel)
                    Screen.OmniAi -> OmniAiScreen(viewModel = viewModel)
                    Screen.SmartList -> SmartListScreen(viewModel = viewModel)
                    Screen.StoresList -> StoresListScreen(viewModel = viewModel)
                    Screen.Comparison -> ComparisonScreen(viewModel = viewModel)
                    Screen.StoreDetail -> StoreDetailScreen(viewModel = viewModel)
                    Screen.InteractiveRoute -> InteractiveRouteScreen(viewModel = viewModel)
                    Screen.Wallet -> WalletScreen(viewModel = viewModel)
                    Screen.Contacts -> ContactsScreen(viewModel = viewModel)
                    Screen.UserProfile -> UserProfileScreen(viewModel = viewModel)
                    Screen.MerchantHub -> MerchantHubScreen(viewModel = viewModel)
                    Screen.TripPlanner -> TripPlannerScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CyberBottomDock(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Contenedor principal de la barra tipo Dock Neón
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0D1627),
            border = androidx.compose.foundation.BorderStroke(1.4.dp, CyberGlowCyan.copy(alpha = 0.72f)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Inicio
                CyberDockItem(
                    icon = Icons.Default.Home,
                    label = "Inicio",
                    selected = currentScreen is Screen.DailyHub,
                    onClick = { onNavigate(Screen.DailyHub) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_daily_hub"
                )

                // 2. Chat (Omni AI)
                CyberDockItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    label = "Chat",
                    selected = currentScreen is Screen.OmniAi,
                    onClick = { onNavigate(Screen.OmniAi) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_chat"
                )

                // Espacio reservado para el Orbe Central 3D "Ai"
                Spacer(modifier = Modifier.weight(1.3f))

                // 4. Billetera (OmniPay)
                CyberDockItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Billetera",
                    selected = currentScreen is Screen.Wallet,
                    onClick = { onNavigate(Screen.Wallet) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_wallet"
                )

                // 5. Perfil (Perfil / Omni Business)
                CyberDockItem(
                    icon = Icons.Default.Person,
                    label = "Perfil",
                    selected = currentScreen is Screen.UserProfile || currentScreen is Screen.MerchantHub,
                    onClick = { onNavigate(Screen.UserProfile) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_profile"
                )
            }
        }

        // Orbe 3D Central Luminoso "Ai" (Réplica exacta de la captura de pantalla)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(0xFF080E1C))
                .border(2.dp, CyberGlowCyan.copy(alpha = 0.95f), CircleShape)
                .clickable { onNavigate(Screen.OmniAi) }
                .testTag("nav_central_ai_orb"),
            contentAlignment = Alignment.Center
        ) {
            // Esfera interna con gradiente radial ámbar / naranja y resplandor
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF6AF0FF), Color(0xFF087BFF), Color(0xFF082A78)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ai",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun CyberDockItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) CyberGlowCyan else Color(0xFF8298B8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) CyberGlowCyan else Color(0xFF8298B8)
        )
    }
}
