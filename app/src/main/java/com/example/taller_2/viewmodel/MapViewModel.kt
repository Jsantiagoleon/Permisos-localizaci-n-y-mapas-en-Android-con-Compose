package com.example.taller_2.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taller_2.R
import com.example.taller_2.data.GeocoderHelper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/** Un marcador creado por el usuario (búsqueda por texto o LongClick). */
data class MapMarker(
    val id: Long,
    val position: LatLng,
    val title: String,
)

/** Mensaje para mostrar en un Snackbar. Se guarda el id del string (strings.xml) y sus argumentos. */
data class UiMessage(
    @param:StringRes val resId: Int,
    val args: List<Any> = emptyList(),
)

/** Orden para mover la cámara. zoom = null significa "conservar el zoom actual". */
data class CameraTarget(
    val position: LatLng,
    val zoom: Float? = null,
)

/** Todo lo que la pantalla del mapa necesita para dibujarse. */
data class MapUiState(
    val hasLocationPermission: Boolean = false,
    val currentLocation: LatLng? = null,
    val routePoints: List<LatLng> = emptyList(),
    val markers: List<MapMarker> = emptyList(),
    val isDarkMap: Boolean = false,
    val followUser: Boolean = true,
    val searchQuery: String = "",
    val message: UiMessage? = null,
)

class MapViewModel(application: Application) :
    AndroidViewModel(application),
    SensorEventListener {

    // ---------- Estado ----------
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    // Eventos de "una sola vez" para mover la cámara (no son estado, por eso un Channel)
    private val _cameraEvents = Channel<CameraTarget>(Channel.BUFFERED)
    val cameraEvents: Flow<CameraTarget> = _cameraEvents.receiveAsFlow()

    // ---------- Dependencias ----------
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val geocoderHelper = GeocoderHelper(application)

    private var isTrackingLocation = false
    private var nextMarkerId = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                onNewLocation(LatLng(location.latitude, location.longitude))
            }
        }
    }

    // =====================================================================
    // PERMISOS
    // =====================================================================

    /** Revisa si ya tenemos permiso (fino o grueso) y lo guarda en el estado. */
    fun checkLocationPermission(): Boolean {
        val granted = hasFineLocation() || hasCoarseLocation()
        _uiState.update { it.copy(hasLocationPermission = granted) }
        return granted
    }

    /** Resultado del diálogo de permisos. Basta con que se conceda uno de los dos. */
    fun onPermissionResult(results: Map<String, Boolean>) {
        val granted = results.values.any { it }
        _uiState.update { it.copy(hasLocationPermission = granted) }
    }

    private fun hasFineLocation() = ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasCoarseLocation() = ContextCompat.checkSelfPermission(
        getApplication(),
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

    // =====================================================================
    // LOCALIZACIÓN (puntos 5 y 7)
    // =====================================================================

    @SuppressLint("MissingPermission") // El permiso se verifica justo abajo
    fun startLocationUpdates() {
        if (isTrackingLocation) return
        if (!hasFineLocation() && !hasCoarseLocation()) return

        // Con permiso fino pedimos GPS (alta precisión); con solo el grueso, precisión balanceada
        val priority = if (hasFineLocation()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(priority, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()

        // Última ubicación conocida: así el mapa no arranca vacío mientras llega la primera lectura
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { onNewLocation(LatLng(it.latitude, it.longitude)) }
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        isTrackingLocation = true
    }

    fun stopLocationUpdates() {
        if (!isTrackingLocation) return
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isTrackingLocation = false
    }

    /** "On location change": actualiza la posición y agrega el punto a la ruta (polyline). */
    private fun onNewLocation(newPosition: LatLng) {
        val state = _uiState.value
        val isFirstLocation = state.currentLocation == null
        val lastPoint = state.routePoints.lastOrNull()

        // Solo se agrega el punto si se movió lo suficiente, para que la línea no tenga "ruido"
        val newRoute = if (lastPoint == null || distanceInMeters(lastPoint, newPosition) >= MIN_DISTANCE_METERS) {
            state.routePoints + newPosition
        } else {
            state.routePoints
        }

        _uiState.update { it.copy(currentLocation = newPosition, routePoints = newRoute) }

        when {
            isFirstLocation -> _cameraEvents.trySend(CameraTarget(newPosition, DEFAULT_ZOOM))
            state.followUser -> _cameraEvents.trySend(CameraTarget(newPosition))
        }
    }

    fun onFollowUserChange(enabled: Boolean) {
        _uiState.update { it.copy(followUser = enabled) }
        val current = _uiState.value.currentLocation
        if (enabled && current != null) {
            _cameraEvents.trySend(CameraTarget(current, DEFAULT_ZOOM))
        }
    }

    private fun distanceInMeters(a: LatLng, b: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    // =====================================================================
    // SENSOR DE LUMINOSIDAD (punto 6)
    // =====================================================================

    fun startLightSensor() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stopLightSensor() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        val lux = event.values[0]
        val isDarkNow = _uiState.value.isDarkMap

        // Histéresis: dos umbrales distintos para que el mapa no "parpadee"
        // cuando la luz está justo en el límite.
        val shouldBeDark = if (isDarkNow) lux < LIGHT_ABOVE_LUX else lux < DARK_BELOW_LUX

        if (shouldBeDark != isDarkNow) {
            _uiState.update { it.copy(isDarkMap = shouldBeDark) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // =====================================================================
    // BÚSQUEDA POR TEXTO CON GEOCODER (punto 8)
    // =====================================================================

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchAddress() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return

        if (!geocoderHelper.isAvailable()) {
            showMessage(UiMessage(R.string.msg_geocoder_unavailable))
            return
        }

        viewModelScope.launch {
            val address = geocoderHelper.searchByName(query)
            if (address == null) {
                showMessage(UiMessage(R.string.msg_address_not_found, listOf(query)))
                return@launch
            }

            val position = LatLng(address.latitude, address.longitude)
            val title = address.getAddressLine(0) ?: query
            addMarker(position, title)

            // Muestra un mensaje con la distancia desde la ubicación actual
            val current = _uiState.value.currentLocation
            if (current != null) {
                val distance = distanceInMeters(current, position)
                val distanceText = if (distance >= 1000) {
                    String.format(Locale.getDefault(), "%.2f km", distance / 1000)
                } else {
                    String.format(Locale.getDefault(), "%.0f m", distance)
                }
                showMessage(UiMessage(R.string.msg_distance_to_location, listOf(title, distanceText)))
            }

            // Dejamos de seguir al usuario para que la cámara se quede en el punto encontrado
            _uiState.update { it.copy(followUser = false) }
            _cameraEvents.send(CameraTarget(position, SEARCH_ZOOM))
        }
    }

    // =====================================================================
    // LONG CLICK + GEOCODER INVERSO (punto 9)
    // =====================================================================

    fun onMapLongClick(position: LatLng) {
        viewModelScope.launch {
            // Si el Geocoder no encuentra dirección, el título serán las coordenadas
            val address: String? = if (geocoderHelper.isAvailable()) {
                geocoderHelper.getAddressText(position)
            } else {
                null
            }
            val title = address ?: getApplication<Application>().getString(
                R.string.marker_coordinates,
                position.latitude,
                position.longitude,
            )
            addMarker(position, title)

            // Muestra un mensaje con la distancia desde la ubicación actual del usuario
            val current = _uiState.value.currentLocation
            if (current != null) {
                val distance = distanceInMeters(current, position)
                val distanceText = if (distance >= 1000) {
                    String.format(Locale.getDefault(), "%.2f km", distance / 1000)
                } else {
                    String.format(Locale.getDefault(), "%.0f m", distance)
                }
                showMessage(UiMessage(R.string.msg_distance_to_location, listOf(title, distanceText)))
            }
        }
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private fun addMarker(position: LatLng, title: String) {
        val marker = MapMarker(id = nextMarkerId++, position = position, title = title)
        _uiState.update { it.copy(markers = it.markers + marker) }
    }

    private fun showMessage(message: UiMessage) {
        _uiState.update { it.copy(message = message) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    /** Se llama cuando la pantalla sale de la pila de navegación: liberamos GPS y sensor. */
    override fun onCleared() {
        stopLocationUpdates()
        stopLightSensor()
        super.onCleared()
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 5_000L
        private const val FASTEST_INTERVAL_MS = 2_000L
        private const val MIN_DISTANCE_METERS = 5f
        const val DEFAULT_ZOOM = 16f
        private const val SEARCH_ZOOM = 16f

        // Umbrales del sensor de luz (en lux). Ajustables según el dispositivo.
        private const val DARK_BELOW_LUX = 40f  // por debajo de esto -> mapa oscuro
        private const val LIGHT_ABOVE_LUX = 60f // por encima de esto -> mapa claro
    }
}
