package com.frequency.android

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.frequency.android.nostr.RelayDirectory
import com.frequency.android.ui.theme.ThemePreferenceManager
import com.frequency.android.net.ArtiTorManager

/**
 * Main application class for frequency Android
 */
class FrequencyApplication : Application() {
    private var defaultNetwork: Network? = null

    override fun onCreate() {
        super.onCreate()

        // Initialize Tor first so any early network goes over Tor
        try {
            val torProvider = ArtiTorManager.getInstance()
            torProvider.init(this)
        } catch (_: Exception){}

        registerDefaultNetworkCallback()

        // Initialize relay directory (loads assets/nostr_relays.csv)
        RelayDirectory.initialize(this)

        // Initialize LocationNotesManager dependencies early so sheet subscriptions can start immediately
        try { com.frequency.android.nostr.LocationNotesInitializer.initialize(this) } catch (_: Exception) { }

        // Initialize favorites persistence early so MessageRouter/NostrTransport can use it on startup
        try {
            com.frequency.android.favorites.FavoritesPersistenceService.initialize(this)
        } catch (_: Exception) { }

        // Warm up Nostr identity to ensure npub is available for favorite notifications
        try {
            com.frequency.android.nostr.NostrIdentityBridge.getCurrentNostrIdentity(this)
        } catch (_: Exception) { }

        // Initialize theme preference
        ThemePreferenceManager.init(this)

        // Initialize debug preference manager (persists debug toggles)
        try { com.frequency.android.ui.debug.DebugPreferenceManager.init(this) } catch (_: Exception) { }

        // Initialize Geohash Registries for persistence
        try {
            com.frequency.android.nostr.GeohashAliasRegistry.initialize(this)
            com.frequency.android.nostr.GeohashConversationRegistry.initialize(this)
        } catch (_: Exception) { }

        // Initialize mesh service preferences
        try { com.frequency.android.service.MeshServicePreferences.init(this) } catch (_: Exception) { }

        // Proactively start the foreground service to keep mesh alive
        try { com.frequency.android.service.MeshForegroundService.start(this) } catch (_: Exception) { }

        // TorManager already initialized above
    }

    private fun registerDefaultNetworkCallback() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        defaultNetwork = connectivityManager.activeNetwork?.takeIf { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        updateDefaultNetwork(network)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                            updateDefaultNetwork(network)
                        }
                    }

                    override fun onLost(network: Network) {
                        if (defaultNetwork == network) defaultNetwork = null
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("FrequencyApplication", "Unable to monitor default network changes", e)
        }
    }

    private fun updateDefaultNetwork(network: Network) {
        if (defaultNetwork == network) return
        defaultNetwork = network
        Log.i("FrequencyApplication", "Default internet network changed; reconnecting relays")
        com.frequency.android.nostr.NostrRelayManager.shared.onDefaultNetworkChanged()
    }
}
