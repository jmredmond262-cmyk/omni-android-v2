package com.example

import android.os.Bundle
import android.content.Intent
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
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
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WbSunny
import com.example.ui.screens.OmniAiScreen
import com.example.ui.screens.OmniExperienceScreen
import com.example.ui.screens.MorningBriefingScreen
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
import com.example.data.auth.AuthRepository
import com.example.data.analytics.UsageAnalyticsRepository
import com.example.data.wallet.DemoWalletRepository
import com.example.ui.screens.AuthScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.DisposableEffect
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
import com.example.ui.screens.RoutineModeScreen
import com.example.ui.screens.JobBoardScreen
import com.example.ui.screens.EventsScreen
import com.example.ui.screens.OrderCenterScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var pendingStoreDeepLink by mutableStateOf<String?>(null)

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid -> UsageAnalyticsRepository.resumeSession(uid) }
    }

    override fun onStop() {
        UsageAnalyticsRepository.endSession()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingStoreDeepLink = intent.data?.takeIf {
            it.scheme == "omni" && it.host == "tienda"
        }?.lastPathSegment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) DebugAppCheckProviderFactory.getInstance()
            else PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        pendingStoreDeepLink = intent?.data?.takeIf {
            it.scheme == "omni" && it.host == "tienda"
        }?.lastPathSegment
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val deepLinkStoreId = pendingStoreDeepLink
            LaunchedEffect(deepLinkStoreId) {
                deepLinkStoreId?.let { storeId ->
                    viewModel.openStoreById(storeId)
                    pendingStoreDeepLink = null
                }
            }
            val themeMode by viewModel.themeMode.collectAsState()
            var showSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(2600)
                showSplash = false
                val briefingPrefs = getSharedPreferences("omni_session", android.content.Context.MODE_PRIVATE)
                if (
                    !briefingPrefs.getBoolean("morning_briefing_seen", false) &&
                    viewModel.currentScreen.value !is Screen.StoreDetail
                ) {
                    viewModel.navigateTo(Screen.MorningBriefing)
                }
            }

            MyApplicationTheme(themeMode = themeMode) {
                Crossfade(targetState = showSplash, label = "omni-launch-transition") { splash ->
                    if (splash) OmniSplashScreen() else AuthGate(activity = this@MainActivity, viewModel = viewModel)
                }
            }
        }
    }
}


@Composable
private fun AuthGate(activity: ComponentActivity, viewModel: MainViewModel) {
    val auth = remember { FirebaseAuth.getInstance() }
    val demoWalletRepository = remember { DemoWalletRepository() }
    val authRepository = remember { AuthRepository() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var authReady by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            authReady = true
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(currentUser?.uid) {
        currentUser?.let { user ->
            authRepository.syncCurrentProfile()
            UsageAnalyticsRepository.startSession(user.uid)
            viewModel.applyAuthenticatedIdentity(
                uid = user.uid,
                name = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                phone = user.phoneNumber.orEmpty(),
                photoUrl = user.photoUrl?.toString().orEmpty()
            )
            // La cuenta DEMO se provisiona al iniciar sesión, no al abrir la billetera.
            demoWalletRepository.ensureWallet { result ->
                result.onSuccess { account ->
                    viewModel.applyRemoteDemoWallet(
                        account.balance, 0.0, emptyList(), account.alias, account.cvu
                    )
                }
            }
        }
    }

    when {
        !authReady -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        currentUser != null -> OmniApp(viewModel = viewModel)
        else -> AuthScreen(activity = activity, repository = remember { AuthRepository() })
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
    val displayLocality = currentCity.ifBlank { "Tu zona" }
    val currentTemp = liveWeather?.let { "${it.temperatureC.toInt()}°C" } ?: "--°C"
    val currentWeatherSymbol = liveWeather?.let { weather ->
        when {
            weather.weatherCode in listOf(95, 96, 99) -> "⛈"
            weather.weatherCode in listOf(71, 73, 75, 77, 85, 86) -> "❄️"
            weather.weatherCode in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ||
                weather.precipitationMm >= 0.1 -> "🌧"
            weather.weatherCode in listOf(45, 48) -> "🌫"
            weather.weatherCode == 3 -> "☁"
            weather.weatherCode in listOf(1, 2) -> "🌤"
            weather.weatherCode == 0 && weather.isDay -> "☀"
            weather.weatherCode == 0 -> "🌙"
            weather.isDay -> "☀"
            else -> "🌙"
        }
    } ?: "⛅"

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
                currentScreen is Screen.OmniExperience ||
                currentScreen is Screen.UserProfile ||
                currentScreen is Screen.Weather ||
                currentScreen is Screen.MorningBriefing ||
                currentScreen is Screen.Routine ||
                currentScreen is Screen.JobBoard ||
                currentScreen is Screen.Events ||
                currentScreen is Screen.OrderCenter ||
                currentScreen is Screen.MerchantOrders
    ) {
        when (currentScreen) {
            is Screen.InteractiveRoute -> viewModel.navigateTo(Screen.StoreDetail)
            is Screen.StoreDetail -> viewModel.navigateTo(Screen.StoresList)
            is Screen.Comparison -> viewModel.navigateTo(Screen.SmartList)
            is Screen.MerchantHub -> viewModel.navigateTo(Screen.UserProfile)
            is Screen.Wallet -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Contacts -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.OmniAi -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.OmniExperience -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Weather -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.MorningBriefing -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Routine -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.JobBoard -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.Events -> viewModel.navigateTo(Screen.DailyHub)
            is Screen.OrderCenter -> viewModel.navigateTo(Screen.StoreDetail)
            is Screen.MerchantOrders -> viewModel.navigateTo(Screen.MerchantHub)
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
                                    val firstName = userProfile.fullName.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Hola" }
                                    Text(
                                        text = "Hola, $firstName",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "📍 $displayLocality",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = "$currentWeatherSymbol $currentTemp · ${liveWeather?.weatherDescription ?: "Clima local"}",
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
                                    text = { Text("🌅 OMNI Buenos Días · Ver presentación") },
                                    onClick = {
                                        viewModel.navigateTo(Screen.MorningBriefing)
                                        overflowMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🏪 Portal de Comerciante") },
                                    onClick = {
                                        viewModel.navigateTo(Screen.MerchantHub)
                                        overflowMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("↪ Cerrar sesión") },
                                    onClick = {
                                        FirebaseAuth.getInstance().signOut()
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
                    Screen.OmniExperience -> OmniExperienceScreen(viewModel = viewModel, onBack = { viewModel.navigateTo(Screen.DailyHub) })
                    Screen.Weather -> WeatherForecastScreen(viewModel = viewModel)
                    Screen.OmniAi -> OmniAiScreen(viewModel = viewModel)
                    Screen.MorningBriefing -> MorningBriefingScreen(
                        viewModel = viewModel,
                        onContinue = {
                            context.getSharedPreferences("omni_session", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("morning_briefing_seen", true).apply()
                            viewModel.navigateTo(Screen.DailyHub)
                        }
                    )
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
                    Screen.Routine -> RoutineModeScreen(onBack = { viewModel.navigateTo(Screen.DailyHub) })
                    Screen.JobBoard -> JobBoardScreen(onBack = { viewModel.navigateTo(Screen.DailyHub) })
                    Screen.Events -> EventsScreen(onBack = { viewModel.navigateTo(Screen.DailyHub) })
                    Screen.OrderCenter -> OrderCenterScreen(viewModel = viewModel, merchantMode = false, onBack = { viewModel.navigateTo(Screen.StoreDetail) })
                    Screen.MerchantOrders -> OrderCenterScreen(viewModel = viewModel, merchantMode = true, onBack = { viewModel.navigateTo(Screen.MerchantHub) })
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

                // 2. Contactos: segundo acceso del bloque izquierdo
                CyberDockItem(
                    icon = Icons.Default.People,
                    label = "Contactos",
                    selected = currentScreen is Screen.Contacts,
                    onClick = { onNavigate(Screen.Contacts) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_contacts"
                )

                // Orbe central: acceso único a Omni AI (evita duplicar Chat + Orbe)
                Spacer(modifier = Modifier.weight(1.3f))

                // 3. Billetera (OmniPay)
                CyberDockItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = "Billetera",
                    selected = currentScreen is Screen.Wallet,
                    onClick = { onNavigate(Screen.Wallet) },
                    modifier = Modifier.weight(1f),
                    testTag = "nav_wallet"
                )

                // 4. Perfil (Perfil / Omni Business)
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
