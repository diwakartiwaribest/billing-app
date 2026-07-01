package com.shop.billing.ui.screens.auth

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shop.billing.data.remote.FirebaseAuthManager
import com.shop.billing.data.remote.FirebaseClient
import com.shop.billing.data.sync.SyncEngine
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

sealed class ShopSetupState {
    data object Idle : ShopSetupState()
    data object Loading : ShopSetupState()
    data class Error(val message: String) : ShopSetupState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val firebaseClient: FirebaseClient,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _authChecked = MutableStateFlow(false)
    val authChecked: StateFlow<Boolean> = _authChecked

    private val _shopSetupState = MutableStateFlow<ShopSetupState>(ShopSetupState.Idle)
    val shopSetupState: StateFlow<ShopSetupState> = _shopSetupState

    private val _needsShopSetup = MutableStateFlow(false)
    val needsShopSetup: StateFlow<Boolean> = _needsShopSetup

    private val _needsPasswordSetup = MutableStateFlow(false)
    val needsPasswordSetup: StateFlow<Boolean> = _needsPasswordSetup

    private fun syncUserIdToSp(userId: String?) {
        context.getSharedPreferences("billing_prefs", Context.MODE_PRIVATE)
            .edit().putString("user_id", userId).apply()
    }

    init {
        viewModelScope.launch {
            val prefs = context.dataStore.data.first()
            val userId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]
            _isLoggedIn.value = !userId.isNullOrBlank()
            _authChecked.value = true
        }
    }

    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    firebaseAuthManager.signInWithEmail(email.trim(), password)
                }
                result.fold(
                    onSuccess = { uid ->
                        onEmailAuthSuccess(uid, email.trim())
                    },
                    onFailure = { e ->
                        _authState.value = AuthState.Error(
                            e.message?.let {
                                when {
                                    it.contains("INVALID_LOGIN_CREDENTIALS") -> "Invalid email or password"
                                    it.contains("USER_DISABLED") -> "Account has been disabled"
                                    it.contains("TOO_MANY_ATTEMPTS_TRY_LATER") -> "Too many attempts. Try again later"
                                    else -> it.take(100)
                                }
                            } ?: "Sign-in failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Email sign-in failed", e)
                _authState.value = AuthState.Error(e.message?.take(100) ?: "Sign-in failed")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
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
                val result = withContext(Dispatchers.IO) {
                    firebaseAuthManager.createAccount(email.trim(), password)
                }
                result.fold(
                    onSuccess = { uid ->
                        onEmailAuthSuccess(uid, email.trim())
                    },
                    onFailure = { e ->
                        _authState.value = AuthState.Error(
                            e.message?.let {
                                when {
                                    it.contains("EMAIL_EXISTS") -> "An account with this email already exists"
                                    it.contains("WEAK_PASSWORD") -> "Password is too weak"
                                    it.contains("INVALID_EMAIL") -> "Invalid email address"
                                    else -> it.take(100)
                                }
                            } ?: "Sign-up failed"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Email sign-up failed", e)
                _authState.value = AuthState.Error(e.message?.take(100) ?: "Sign-up failed")
            }
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) return
        firebaseAuthManager.sendPasswordReset(email.trim())
        _authState.value = AuthState.Error("Password reset email sent")
    }

    fun createFirebaseShop(code: String, name: String) {
        if (code.isBlank()) {
            _shopSetupState.value = ShopSetupState.Error("Shop code is required")
            return
        }
        _shopSetupState.value = ShopSetupState.Loading
        viewModelScope.launch {
            try {
                val uid = firebaseAuthManager.currentUserId ?: run {
                    _shopSetupState.value = ShopSetupState.Error("Not signed in")
                    return@launch
                }
                val secret = withContext(Dispatchers.IO) {
                    java.util.UUID.randomUUID().toString().take(8).uppercase()
                }
                val email = firebaseAuthManager.currentUserEmail ?: ""
                val created = withContext(Dispatchers.IO) {
                    firebaseClient.createShop(code, uid, name, secret, email)
                }
                if (created) {
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = code
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = name
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "owner"
                        prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = uid
                        prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] =
                            firebaseAuthManager.currentUserEmail ?: ""
                    }
                    syncEngine.exportAllToFirebase(code)
                    context.dataStore.edit { prefs ->
                        prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_FIREBASE_EXPORT_DONE)] = true
                    }
                    _isLoggedIn.value = true
                    _needsShopSetup.value = false
                    _shopSetupState.value = ShopSetupState.Idle
                    _authState.value = AuthState.Authenticated
                } else {
                    _shopSetupState.value = ShopSetupState.Error("Shop code already exists")
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "create shop failed", e)
                _shopSetupState.value = ShopSetupState.Error(e.message?.take(100) ?: "Failed to create shop")
            }
        }
    }

    fun joinFirebaseShop(code: String, secret: String) {
        if (code.isBlank() || secret.isBlank()) {
            _shopSetupState.value = ShopSetupState.Error("Code and secret are required")
            return
        }
        _shopSetupState.value = ShopSetupState.Loading
        viewModelScope.launch {
            try {
                val valid = withContext(Dispatchers.IO) {
                    firebaseClient.verifyShopSecret(code, secret)
                }
                if (!valid) {
                    _shopSetupState.value = ShopSetupState.Error("Invalid shop code or secret")
                    return@launch
                }
                val uid = firebaseAuthManager.currentUserId ?: run {
                    _shopSetupState.value = ShopSetupState.Error("Not signed in")
                    return@launch
                }
                val email = firebaseAuthManager.currentUserEmail ?: ""
                withContext(Dispatchers.IO) {
                    firebaseClient.addUserToShop(code, uid, "member", email)
                }
                val shopInfo = withContext(Dispatchers.IO) {
                    firebaseClient.getShopInfo(code)
                }
                val shopName = shopInfo["name"]?.toString() ?: code
                val shopAddress = shopInfo["address"]?.toString() ?: ""
                val shopPhone = shopInfo["phone"]?.toString() ?: ""
                val shopLogo = shopInfo["logo"]?.toString() ?: ""
                val shopInvoiceMessage = shopInfo["invoiceMessage"]?.toString() ?: ""
                context.dataStore.edit { prefs ->
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = code
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = shopName
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = shopAddress
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = shopPhone
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = secret
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = "member"
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = uid
                    prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
                    if (shopLogo.isNotBlank()) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = shopLogo
                    if (shopInvoiceMessage.isNotBlank()) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = shopInvoiceMessage
                }
                syncEngine.exportAllToFirebase(code)
                context.dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_FIREBASE_EXPORT_DONE)] = true
                }
                _isLoggedIn.value = true
                _needsShopSetup.value = false
                _shopSetupState.value = ShopSetupState.Idle
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                Log.e("AuthVM", "join shop failed", e)
                _shopSetupState.value = ShopSetupState.Error(e.message?.take(100) ?: "Failed to join shop")
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    firebaseAuthManager.signInWithGoogle(idToken)
                }
                result.fold(
                    onSuccess = { uid ->
                        val email = firebaseAuthManager.currentUserEmail ?: ""

                        val prefs = context.dataStore.data.first()
                        val savedUserId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]
                        val savedShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)]

                        // New user on this device — clear any stale shop data from prev user
                        if (savedUserId != null && savedUserId != uid) {
                            context.dataStore.edit { p ->
                                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE))
                                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME))
                                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET))
                                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
                                p.remove(booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED))
                            }
                        }

                        // Don't save userId to prefs yet — only after setup is complete
                        _isLoggedIn.value = true

                        val needsPassword = !firebaseAuthManager.hasPasswordProvider()
                        if (needsPassword) {
                            _needsPasswordSetup.value = true
                            _authState.value = AuthState.Idle
                        } else {
                            val hasShopCode = savedShopCode?.isNotBlank() == true && savedUserId == uid
                            if (!hasShopCode) {
                                if (tryAutoConfigureFromFirestore(uid, email)) return@fold
                                _needsShopSetup.value = true
                                _authState.value = AuthState.Idle
                            } else {
                                completeSetup(uid, email)
                            }
                        }
                    },
                    onFailure = { e ->
                        _authState.value = AuthState.Error(e.message?.take(100) ?: "Google sign-in failed")
                    }
                )
            } catch (e: Exception) {
                Log.e("AuthVM", "Google sign-in failed", e)
                _authState.value = AuthState.Error(e.message?.take(100) ?: "Google sign-in failed")
            }
        }
    }

    fun setPassword(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    firebaseAuthManager.updatePassword(password)
                }
                result.fold(
                    onSuccess = {
                        _needsPasswordSetup.value = false
                        val prefs = context.dataStore.data.first()
                        val savedShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)]
                        val savedUserId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]
                        val uid = firebaseAuthManager.currentUserId ?: ""
                        val email = firebaseAuthManager.currentUserEmail ?: ""
                        val hasShopCode = savedShopCode?.isNotBlank() == true && savedUserId == uid
                        if (!hasShopCode) {
                            if (tryAutoConfigureFromFirestore(uid, email)) return@fold
                            _needsShopSetup.value = true
                            _authState.value = AuthState.Idle
                        } else {
                            completeSetup(uid, email)
                        }
                    },
                    onFailure = { e ->
                        _authState.value = AuthState.Error(e.message?.take(100) ?: "Failed to set password")
                    }
                )
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message?.take(100) ?: "Failed to set password")
            }
        }
    }

    private fun completeSetup(uid: String, email: String) {
        viewModelScope.launch {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = uid
                prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
            }
            syncUserIdToSp(uid)
            _isLoggedIn.value = true
            _needsPasswordSetup.value = false
            _needsShopSetup.value = false
            _authState.value = AuthState.Authenticated
        }
    }

    private suspend fun tryAutoConfigureFromFirestore(uid: String, email: String): Boolean {
        val shops = withContext(Dispatchers.IO) {
            firebaseClient.getUserShops(uid)
        }
        if (shops.isEmpty()) return false
        val (shopCode, role) = shops.first()
        val info = withContext(Dispatchers.IO) {
            firebaseClient.getShopInfo(shopCode)
        }
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)] = shopCode
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME)] = info["name"]?.toString() ?: shopCode
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_ADDRESS)] = info["address"]?.toString() ?: ""
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_PHONE)] = info["phone"]?.toString() ?: ""
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET)] = info["secret"]?.toString() ?: ""
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE)] = role
            prefs[booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED)] = true
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)] = uid
            prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_EMAIL)] = email
            val logo = info["logo"]?.toString()
            if (logo?.isNotBlank() == true) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_LOGO)] = logo
            val invoiceMsg = info["invoiceMessage"]?.toString()
            if (invoiceMsg?.isNotBlank() == true) prefs[stringPreferencesKey(Constants.SETTINGS_KEY_INVOICE_MESSAGE)] = invoiceMsg
        }
        completeSetup(uid, email)
        return true
    }

    private suspend fun onEmailAuthSuccess(uid: String, email: String) {
        val prefs = context.dataStore.data.first()
        val savedShopCode = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE)]
        val savedUserId = prefs[stringPreferencesKey(Constants.SETTINGS_KEY_USER_ID)]

        if (savedUserId != null && savedUserId != uid) {
            context.dataStore.edit { p ->
                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_CODE))
                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_NAME))
                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_SHOP_SECRET))
                p.remove(stringPreferencesKey(Constants.SETTINGS_KEY_USER_ROLE))
                p.remove(booleanPreferencesKey(Constants.SETTINGS_KEY_SYNC_ENABLED))
            }
        }

        _isLoggedIn.value = true
        val hasShopCode = savedShopCode?.isNotBlank() == true && savedUserId == uid
        if (!hasShopCode) {
            if (tryAutoConfigureFromFirestore(uid, email)) return
            _needsShopSetup.value = true
            _authState.value = AuthState.Idle
        } else {
            completeSetup(uid, email)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                firebaseAuthManager.signOut()
            } catch (_: Exception) { }
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
}
