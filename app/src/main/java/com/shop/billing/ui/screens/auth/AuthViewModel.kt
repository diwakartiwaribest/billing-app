package com.shop.billing.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.SupabaseClient
import com.shop.billing.util.Constants
import com.shop.billing.util.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _authChecked = MutableStateFlow(false)
    val authChecked: StateFlow<Boolean> = _authChecked

    private fun syncUserIdToSp(userId: String?) {
        context.getSharedPreferences("billing_prefs", Context.MODE_PRIVATE)
            .edit().putString("user_id", userId).apply()
    }

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val userId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]
            var supabaseUrl = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
            var supabaseKey = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""

            if (supabaseUrl.isBlank() || supabaseKey.isBlank()) {
                context.dataStore.edit { store ->
                    store[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = Constants.HARDCODED_SUPABASE_URL
                    store[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = Constants.HARDCODED_SUPABASE_KEY
                }
                supabaseUrl = Constants.HARDCODED_SUPABASE_URL
                supabaseKey = Constants.HARDCODED_SUPABASE_KEY
            }

            val hasSupabase = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()
            _isLoggedIn.value = !userId.isNullOrBlank()
            _authChecked.value = true
            if (_isLoggedIn.value || !hasSupabase) {
                _authState.value = AuthState.Authenticated
            }
        }
    }

    private suspend fun getSupabaseConfig(): Pair<String, String> {
        val prefs = context.dataStore.data.first()
        val url = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] ?: ""
        val key = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] ?: ""
        return Pair(url, key)
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val (url, key) = getSupabaseConfig()
                if (url.isBlank() || key.isBlank()) {
                    _authState.value = AuthState.Error("Configure Supabase URL and API key in Settings first")
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    supabaseClient.signUp(url, key, email, password)
                }

                Log.d("AuthVM", "signUp result: $result")

                val userId = result?.let { json ->
                    json.optJSONObject("user")?.optString("id")
                        ?: json.optString("id")
                        ?: ""
                } ?: ""

                Log.d("AuthVM", "Parsed userId: $userId")

                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = userId
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
                }
                syncUserIdToSp(userId)

                _isLoggedIn.value = true
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                Log.e("AuthVM", "Sign up failed", e)
                val msg = e.message ?: "Sign up failed"
                _authState.value = AuthState.Error(
                    when {
                        msg.contains("already registered", ignoreCase = true) -> "Email already registered. Try logging in."
                        msg.contains("invalid", ignoreCase = true) && msg.contains("email", ignoreCase = true) -> "Invalid email format"
                        else -> msg.take(100)
                    }
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val (url, key) = getSupabaseConfig()
                if (url.isBlank() || key.isBlank()) {
                    _authState.value = AuthState.Error("Configure Supabase URL and API key in Settings first")
                    return@launch
                }

                val result = withContext(Dispatchers.IO) {
                    supabaseClient.signIn(url, key, email, password)
                }

                Log.d("AuthVM", "signIn result: $result")

                val userId = result?.let { json ->
                    json.optJSONObject("user")?.optString("id")
                        ?: json.optString("id")
                        ?: ""
                } ?: ""

                Log.d("AuthVM", "Parsed userId: $userId")

                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = userId
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
                }
                syncUserIdToSp(userId)

                _isLoggedIn.value = true
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                Log.e("AuthVM", "Sign in failed", e)
                val msg = e.message ?: "Sign in failed"
                _authState.value = AuthState.Error(
                    when {
                        msg.contains("Invalid login credentials", ignoreCase = true) -> "Invalid email or password"
                        msg.contains("Email not confirmed", ignoreCase = true) -> "Email not confirmed. Check your inbox."
                        else -> msg.take(100)
                    }
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID))
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL))
                prefs.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
            }
            syncUserIdToSp(null)
            _isLoggedIn.value = false
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun joinViaQr(email: String, password: String, shopCode: String, shopSecret: String, supabaseUrl: String, supabaseKey: String, pat: String = "", projectRef: String = "") {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                // Store Supabase config from QR
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_URL)] = supabaseUrl
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SUPABASE_KEY)] = supabaseKey
                }

                // Sign in first (existing user), or sign up (new user)
                var userId = ""
                try {
                    val signInResult = withContext(Dispatchers.IO) {
                        supabaseClient.signIn(supabaseUrl, supabaseKey, email, password)
                    }
                    userId = signInResult?.let { json ->
                        json.optJSONObject("user")?.optString("id")
                            ?: json.optString("id")
                            ?: ""
                    } ?: ""
                } catch (e: Exception) {
                    Log.d("AuthVM", "signIn failed (user may not exist): ${e.message}")
                }

                if (userId.isBlank()) {
                    val signUpResult = withContext(Dispatchers.IO) {
                        supabaseClient.signUp(supabaseUrl, supabaseKey, email, password)
                    }
                    userId = signUpResult?.let { json ->
                        json.optJSONObject("user")?.optString("id")
                            ?: json.optString("id")
                            ?: ""
                    } ?: ""

                    if (userId.isBlank()) {
                        _authState.value = AuthState.Error("Could not create or sign in to account")
                        return@launch
                    }
                }

                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = userId
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
                }
                syncUserIdToSp(userId)
                _isLoggedIn.value = true

                // Register as member in the shop (upsert prevents duplicates)
                withContext(Dispatchers.IO) {
                    supabaseClient.registerUserShop(supabaseUrl, supabaseKey, userId, shopCode, "member", email = email)
                }
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = shopCode
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = shopSecret
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "member"
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                    if (pat.isNotBlank()) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PAT)] = pat
                    if (projectRef.isNotBlank()) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_PROJECT_REF)] = projectRef
                }
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                Log.e("AuthVM", "joinViaQr failed", e)
                _authState.value = AuthState.Error(e.message?.take(100) ?: "Failed to join shop")
            }
        }
    }
}
