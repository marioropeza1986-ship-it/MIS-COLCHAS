package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOut
import com.example.data.ColchasRepository
import com.example.data.PreferencesRepository
import com.example.data.ProductoConStock
import com.example.data.UiState
import com.example.data.Venta
import com.example.ui.ColchasViewModel
import com.example.ui.DashboardData
import com.example.ui.screens.CobrosScreen
import com.example.ui.screens.ColchasScreen
import com.example.ui.screens.HistorialScreen
import com.example.ui.screens.InicioScreen
import com.example.ui.screens.NuevaVentaSheet
import com.example.ui.screens.ProductoFormSheet
import com.example.ui.screens.ReabastecerSheet
import com.example.ui.screens.RegistrarAbonoSheet
import com.example.ui.theme.AppVisualStyle
import com.example.ui.theme.MisColchasTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
) {
    INICIO("Inicio", Icons.Rounded.Home, Icons.Outlined.Home, "nav_inicio"),
    COLCHAS("Colchas", Icons.Rounded.Bed, Icons.Outlined.Bed, "nav_colchas"),
    COBROS("Cobros", Icons.Rounded.Payments, Icons.Outlined.Payments, "nav_cobros"),
    HISTORIAL("Historial", Icons.Rounded.History, Icons.Outlined.History, "nav_historial"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val largeText by prefsRepo.largeTextEnabled.collectAsStateWithLifecycle(initialValue = false)
    val themeMode by prefsRepo.themeMode.collectAsStateWithLifecycle(initialValue = "light")
    val visualStyleKey by prefsRepo.visualStyle.collectAsStateWithLifecycle(initialValue = "terracota")
    val appVisualStyle = AppVisualStyle.fromKey(visualStyleKey)
    val useDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    MisColchasTheme(visualStyle = appVisualStyle, darkTheme = useDarkTheme, largeText = largeText) {
        val user = currentUser
        if (user == null) {
            AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
        } else {
            key(user.uid) {
                MisColchasMainContent(
                    currentUserId = user.uid,
                    userName = user.displayName,
                    userEmail = user.email,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MisColchasMainContent(
    currentUserId: String,
    userName: String?,
    userEmail: String?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: ColchasViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ColchasViewModel(
                    repository = ColchasRepository(db),
                    preferencesRepository = PreferencesRepository(app),
                    currentUserId = currentUserId,
                )
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val largeText by viewModel.largeTextEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val visualStyleKey by viewModel.visualStyle.collectAsStateWithLifecycle()
    val visualStyle = AppVisualStyle.fromKey(visualStyleKey)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var activeTab by remember { mutableStateOf(MainTab.INICIO) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }
    var showProductoSheet by remember { mutableStateOf(false) }
    var editingProducto by remember { mutableStateOf<ProductoConStock?>(null) }
    var showReabastecerSheet by remember { mutableStateOf(false) }
    var preselectedReabastecer by remember { mutableStateOf<ProductoConStock?>(null) }
    var showVentaSheet by remember { mutableStateOf(false) }
    var preselectedVenta by remember { mutableStateOf<ProductoConStock?>(null) }
    var abonoVentaTarget by remember { mutableStateOf<Venta?>(null) }

    if (activeTab != MainTab.INICIO) {
        BackHandler { activeTab = MainTab.INICIO }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Mis Colchas",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    },
                    actions = {
                        val successData = (uiState as? UiState.Success<DashboardData>)?.data
                        if (successData != null && successData.productosConStock.any { it.disponibles > 0L }) {
                            Button(
                                onClick = {
                                    preselectedVenta = null
                                    showVentaSheet = true
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .height(44.dp)
                                    .testTag("topbar_sell_button"),
                                shape = RoundedCornerShape(if (visualStyle == AppVisualStyle.APPLE) 999.dp else 14.dp),
                            ) {
                                Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(" Vender", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        Spacer(Modifier.height(12.dp))
                        MainTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                    ) {
                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = "Cargando tus colchas...",
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "No se pudo conectar con tus datos",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                        }
                                    }
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                when (activeTab) {
                                    MainTab.INICIO -> InicioScreen(
                                        data = data,
                                        userName = userName,
                                        visualStyle = visualStyle,
                                        onSelectVisualStyle = { viewModel.setVisualStyle(it.key) },
                                        onVenderClick = {
                                            if (data.productosConStock.none { it.disponibles > 0L }) {
                                                editingProducto = null
                                                showProductoSheet = true
                                            } else {
                                                preselectedVenta = null
                                                showVentaSheet = true
                                            }
                                        },
                                        onIrACobrosClick = { activeTab = MainTab.COBROS },
                                        onAgregarColchaClick = {
                                            if (data.productosConStock.isEmpty()) {
                                                editingProducto = null
                                                showProductoSheet = true
                                            } else {
                                                showAddChoiceDialog = true
                                            }
                                        },
                                        onEditarProducto = {
                                            editingProducto = it
                                            showProductoSheet = true
                                        },
                                        onVenderProducto = {
                                            preselectedVenta = it
                                            showVentaSheet = true
                                        },
                                        onReabastecerProducto = {
                                            preselectedReabastecer = it
                                            showReabastecerSheet = true
                                        },
                                        onAbonarVenta = { abonoVentaTarget = it },
                                    )

                                    MainTab.COLCHAS -> ColchasScreen(
                                        productos = data.productosConStock,
                                        onAgregarNueva = {
                                            editingProducto = null
                                            showProductoSheet = true
                                        },
                                        onEditarProducto = {
                                            editingProducto = it
                                            showProductoSheet = true
                                        },
                                        onReabastecer = {
                                            preselectedReabastecer = it
                                            showReabastecerSheet = true
                                        },
                                        onVenderProducto = {
                                            preselectedVenta = it
                                            showVentaSheet = true
                                        },
                                        onCambiarUbicacion = { prod, loc ->
                                            viewModel.updateUbicacion(prod, loc)
                                        },
                                    )

                                    MainTab.COBROS -> CobrosScreen(
                                        ventas = data.ventas,
                                        onAbonarVenta = { abonoVentaTarget = it },
                                    )

                                    MainTab.HISTORIAL -> HistorialScreen(
                                        data = data,
                                        userEmail = userEmail,
                                        largeTextEnabled = largeText,
                                        themeMode = themeMode,
                                        visualStyle = visualStyle,
                                        onToggleLargeText = viewModel::setLargeTextEnabled,
                                        onChangeThemeMode = viewModel::setThemeMode,
                                        onChangeVisualStyle = { viewModel.setVisualStyle(it.key) },
                                        onDeleteVenta = viewModel::deleteVenta,
                                        onSignOut = {
                                            signOut(credentialManager = credentialManager, scope = scope)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Choice dialog: Add brand-new model vs restock existing
    if (showAddChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showAddChoiceDialog = false },
            title = { Text("¿Qué quieres agregar?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showAddChoiceDialog = false
                            editingProducto = null
                            showProductoSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Rounded.Bed, contentDescription = null)
                        Text("  Modelo nuevo de colcha")
                    }
                    Button(
                        onClick = {
                            showAddChoiceDialog = false
                            preselectedReabastecer = null
                            showReabastecerSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                        ),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("  Surtir piezas a uno existente")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddChoiceDialog = false }) { Text("Cancelar") }
            },
        )
    }

    val currentSuccess = (uiState as? UiState.Success<DashboardData>)?.data

    if (showProductoSheet) {
        ProductoFormSheet(
            existingProducto = editingProducto,
            isBusy = isBusy,
            onDismiss = { showProductoSheet = false },
            onSave = { sku, modelo, tamano, color, ubicacion, foto, stockIni, costo, precio, min ->
                val existing = editingProducto?.producto
                if (existing == null) {
                    viewModel.addProducto(
                        sku = sku,
                        modelo = modelo,
                        tamano = tamano,
                        color = color,
                        ubicacion = ubicacion,
                        fotoBase64 = foto,
                        stockInicial = stockIni,
                        costo = costo,
                        precio = precio,
                        stockMinimo = min,
                        onSuccess = { showProductoSheet = false },
                    )
                } else {
                    viewModel.updateProducto(
                        productoId = existing.id,
                        sku = sku,
                        modelo = modelo,
                        tamano = tamano,
                        color = color,
                        ubicacion = ubicacion,
                        fotoBase64 = foto,
                        stockInicial = stockIni,
                        costo = costo,
                        precio = precio,
                        stockMinimo = min,
                        onSuccess = { showProductoSheet = false },
                    )
                }
            },
            onDelete = { prod ->
                viewModel.deleteProducto(prod) { showProductoSheet = false }
            },
        )
    }

    if (showReabastecerSheet && currentSuccess != null) {
        ReabastecerSheet(
            productos = currentSuccess.productosConStock,
            preselected = preselectedReabastecer,
            isBusy = isBusy,
            onDismiss = { showReabastecerSheet = false },
            onSaveEntrada = { fecha, prod, cant, costo, prov ->
                viewModel.addEntrada(
                    fecha = fecha,
                    producto = prod,
                    cantidad = cant,
                    costoUnitario = costo,
                    proveedor = prov,
                    onSuccess = { showReabastecerSheet = false },
                )
            },
        )
    }

    if (showVentaSheet && currentSuccess != null) {
        NuevaVentaSheet(
            productosConStock = currentSuccess.productosConStock,
            clientasSugeridas = currentSuccess.clientasSugeridas,
            preselected = preselectedVenta,
            isBusy = isBusy,
            onDismiss = { showVentaSheet = false },
            onSaveVenta = { fecha, prod, cant, precio, modalidad, enganche, proximo, cli, tel, nota ->
                viewModel.addVenta(
                    fecha = fecha,
                    producto = prod,
                    cantidad = cant,
                    precioUnitario = precio,
                    modalidad = modalidad,
                    enganche = enganche,
                    proximoCobro = proximo,
                    clienteNombre = cli,
                    clienteTelefono = tel,
                    nota = nota,
                    onSuccess = { showVentaSheet = false },
                )
            },
        )
    }

    abonoVentaTarget?.let { targetVenta ->
        RegistrarAbonoSheet(
            venta = targetVenta,
            isBusy = isBusy,
            onDismiss = { abonoVentaTarget = null },
            onSaveAbono = { monto, nuevaFecha ->
                viewModel.registrarAbono(
                    venta = targetVenta,
                    montoAbono = monto,
                    nuevaFechaCobro = nuevaFecha,
                    onSuccess = { abonoVentaTarget = null },
                )
            },
        )
    }
}
