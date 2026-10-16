package com.example.taller_2.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taller_2.R
import com.example.taller_2.components.AddressSearchField
import com.example.taller_2.components.LabeledSwitch
import com.example.taller_2.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch

// Punto inicial mientras llega la primera ubicación (Pontificia Universidad Javeriana, Bogotá)
private val DEFAULT_LOCATION = LatLng(4.6286, -74.0645)

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    // viewModel() crea (o recupera) el ViewModel asociado a esta entrada de Navigation 3
    viewModel: MapViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ---------- 1. Permisos de localización (fina y gruesa) ----------
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results -> viewModel.onPermissionResult(results) }

    LaunchedEffect(Unit) {
        if (!viewModel.checkLocationPermission()) {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // ---------- 2. Encender GPS y sensor solo mientras la pantalla está visible ----------
    LifecycleStartEffect(uiState.hasLocationPermission) {
        viewModel.startLightSensor()
        if (uiState.hasLocationPermission) viewModel.startLocationUpdates()
        onStopOrDispose {
            viewModel.stopLocationUpdates()
            viewModel.stopLightSensor()
        }
    }

    // ---------- 3. Cámara del mapa ----------
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, 12f)
    }
    var isMapLoaded by remember { mutableStateOf(false) }

    // Escucha las órdenes de "mover cámara" del ViewModel (solo cuando el mapa ya cargó)
    LaunchedEffect(isMapLoaded) {
        if (!isMapLoaded) return@LaunchedEffect
        viewModel.cameraEvents.collect { target ->
            val update = if (target.zoom != null) {
                CameraUpdateFactory.newLatLngZoom(target.position, target.zoom)
            } else {
                CameraUpdateFactory.newLatLng(target.position)
            }
            // Cada animación en su propia corrutina: si el usuario toca el mapa y la
            // interrumpe, solo se cancela esa animación y no la escucha de eventos.
            launch { cameraPositionState.animate(update, 800) }
        }
    }

    // ---------- 4. Mensajes (Snackbar) ----------
    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            context.getString(message.resId, *message.args.toTypedArray()),
        )
        viewModel.onMessageShown()
    }

    // ---------- 5. Estilo del mapa según el sensor de luz ----------
    val darkMapStyle = remember {
        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
    }
    val mapProperties = MapProperties(
        isMyLocationEnabled = uiState.hasLocationPermission,
        mapStyleOptions = if (uiState.isDarkMap) darkMapStyle else null,
    )
    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = true,
        myLocationButtonEnabled = uiState.hasLocationPermission,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.title_map)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.btn_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LabeledSwitch(
                    label = stringResource(id = R.string.label_follow_user),
                    checked = uiState.followUser,
                    onCheckedChange = viewModel::onFollowUserChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                AddressSearchField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    onSearch = viewModel::searchAddress,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!uiState.hasLocationPermission) {
                    PermissionCard(onRequest = { permissionLauncher.launch(LOCATION_PERMISSIONS) })
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings,
                    mapColorScheme = if (uiState.isDarkMap) {
                        ComposeMapColorScheme.DARK
                    } else {
                        ComposeMapColorScheme.LIGHT
                    },
                    onMapLoaded = { isMapLoaded = true },
                    onMapLongClick = viewModel::onMapLongClick,
                ) {
                    // Ruta recorrida por el usuario (punto 7)
                    if (uiState.routePoints.size >= 2) {
                        Polyline(
                            points = uiState.routePoints,
                            color = MaterialTheme.colorScheme.primary,
                            width = 14f,
                            jointType = JointType.ROUND,
                            startCap = RoundCap(),
                            endCap = RoundCap(),
                        )
                    }

                    // Marcador de la posición actual (punto 5)
                    uiState.currentLocation?.let { position ->
                        Marker(
                            state = rememberUpdatedMarkerState(position = position),
                            title = stringResource(id = R.string.marker_my_location),
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE,
                            ),
                        )
                    }

                    // Marcadores de búsqueda y LongClick (puntos 8 y 9)
                    uiState.markers.forEach { marker ->
                        key(marker.id) {
                            Marker(
                                state = rememberUpdatedMarkerState(position = marker.position),
                                title = marker.title,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(id = R.string.msg_location_permission_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequest) {
                Text(text = stringResource(id = R.string.btn_grant_permission))
            }
        }
    }
}
