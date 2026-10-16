package com.example.taller_2.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Envuelve el Geocoder de Android en funciones suspend (corrutinas).
 *  - searchByName:   texto  -> posición  (geocodificación directa)   -> punto 8 del taller
 *  - getAddressText: posición -> texto   (geocodificación inversa)  -> punto 9 del taller
 *
 * Desde Android 13 (API 33) el Geocoder tiene una versión asíncrona con callback;
 * en versiones anteriores solo existe la versión bloqueante, que se ejecuta en Dispatchers.IO
 * para no congelar la interfaz.
 */
class GeocoderHelper(context: Context) {

    private val geocoder = Geocoder(context, Locale.getDefault())

    fun isAvailable(): Boolean = Geocoder.isPresent()

    /** Busca una dirección escrita por el usuario. Devuelve null si no hay resultados. */
    suspend fun searchByName(query: String): Address? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(
                    query,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                }
            }
        }
    }

    /** Obtiene la dirección (texto) de una posición del mapa. Devuelve null si no hay resultados. */
    suspend fun getAddressText(latLng: LatLng): String? {
        val address: Address? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            cont.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            cont.resume(null)
                        }
                    },
                )
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?.firstOrNull()
                } catch (e: IOException) {
                    null
                }
            }
        }
        // getAddressLine(0) trae la dirección completa en una sola línea
        return address?.getAddressLine(0)
    }
}
