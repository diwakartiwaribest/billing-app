package com.shop.billing.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.shop.billing.R
import com.shop.billing.ui.components.DialogCancelButton
import com.shop.billing.ui.components.DialogConfirmButton
import com.shop.billing.ui.components.DialogOverlay
import androidx.compose.material3.MaterialTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val needsShopSetup by viewModel.needsShopSetup.collectAsState()
    val needsPasswordSetup by viewModel.needsPasswordSetup.collectAsState()
    val shopSetupState by viewModel.shopSetupState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account?.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                }
            } catch (e: Exception) {
                viewModel.signInWithGoogle("")
            }
        }
    }

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    var newPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var setupModeCreate by remember { mutableStateOf(true) }
    var setupShopCode by remember { mutableStateOf("") }
    var setupShopName by remember { mutableStateOf("") }
    var setupShopSecret by remember { mutableStateOf("") }
    var setupError by remember { mutableStateOf("") }

    val qrScanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val decoded = try { java.net.URLDecoder.decode(result.contents.trim(), "UTF-8") } catch (_: Exception) { result.contents.trim() }
            val parts = decoded.split(":")
            if (parts.size >= 3 && parts[0].uppercase() == "BILLING") {
                setupShopCode = parts[1].uppercase()
                setupShopSecret = parts.drop(2).joinToString(":")
            } else {
                setupShopCode = decoded.uppercase()
            }
            setupModeCreate = false
        }
    }

    LaunchedEffect(needsShopSetup) {
        if (needsShopSetup) {
            showSetupDialog = true
            setupError = ""
            setupShopCode = ""
            setupShopName = ""
            setupShopSecret = ""
        }
    }

    LaunchedEffect(shopSetupState) {
        when (val state = shopSetupState) {
            is ShopSetupState.Error -> setupError = state.message
            else -> {}
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rupee),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Billing App",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sign in to manage your shop",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { isSignUp = false; localError = "" },
                                modifier = Modifier
                                    .weight(1f)
                                        .background(
                                            if (!isSignUp) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Text(
                                        "Sign In",
                                        fontWeight = if (!isSignUp) FontWeight.Bold else FontWeight.Normal,
                                        color = if (!isSignUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { isSignUp = true; localError = "" },
                                modifier = Modifier
                                    .weight(1f)
                                        .background(
                                            if (isSignUp) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Text(
                                        "Sign Up",
                                        fontWeight = if (isSignUp) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSignUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; localError = "" },
                            label = { Text("Email") },
                            placeholder = { Text("Enter your email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; localError = "" },
                            label = { Text("Password") },
                            placeholder = { Text("Enter your password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (isSignUp) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    if (isSignUp) focusManager.moveFocus(FocusDirection.Down)
                                    else {
                                        focusManager.clearFocus()
                                        if (!isSignUp) viewModel.signInWithEmail(email, password)
                                    }
                                },
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.signInWithEmail(email, password)
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        if (isSignUp) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; localError = "" },
                                label = { Text("Confirm Password") },
                                placeholder = { Text("Re-enter password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (password == confirmPassword) {
                                            viewModel.signUpWithEmail(email, password)
                                        } else {
                                            localError = "Passwords do not match"
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "Hide password" else "Show password",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (!isSignUp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showResetDialog = true }) {
                                    Text("Forgot password?", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (localError.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = localError,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        when (val state = authState) {
                            is AuthState.Error -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.message,
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {}
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                localError = ""
                                if (isSignUp) {
                                    if (password != confirmPassword) {
                                        localError = "Passwords do not match"
                                    } else {
                                        viewModel.signUpWithEmail(email, password)
                                    }
                                } else {
                                    viewModel.signInWithEmail(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            enabled = authState !is AuthState.Loading &&
                                email.isNotBlank() && password.isNotBlank() &&
                                (!isSignUp || confirmPassword.isNotBlank())
                        ) {
                            if (authState is AuthState.Loading) {
                                Text(
                                    "Please wait...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    if (isSignUp) "Create Account" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Text("  or  ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    enabled = authState !is AuthState.Loading
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Sign in with Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

        // Overlay dialogs
        if (needsPasswordSetup) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Set Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("Set a password so you can also sign in with email.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        )
                        if (newPassword.isNotBlank() && newPassword.length < 6) {
                            Spacer(Modifier.height(4.dp))
                            Text("Minimum 6 characters", fontSize = 11.sp, color = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth())
                        }
                        if (authState is AuthState.Error) {
                            Spacer(Modifier.height(8.dp))
                            Text((authState as AuthState.Error).message, color = Color(0xFFDC2626), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.setPassword(newPassword) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = newPassword.length >= 6 && authState !is AuthState.Loading
                        ) {
                            Text(if (authState is AuthState.Loading) "Saving..." else "Set Password", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else if (showSetupDialog) {
            DialogOverlay(onDismiss = { showSetupDialog = false }) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = if (setupModeCreate) Icons.Default.Store else Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(if (setupModeCreate) "Create Shop" else "Join Shop", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text(if (setupModeCreate) "Set up a new shop for your business" else "Enter shop details to join an existing shop", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(
                                onClick = { setupModeCreate = true; setupError = "" },
                                modifier = Modifier.background(if (setupModeCreate) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 24.dp)
                            ) { Text("Create", fontWeight = FontWeight.Bold, color = if (setupModeCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { setupModeCreate = false; setupError = "" },
                                modifier = Modifier.background(if (!setupModeCreate) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 24.dp)
                            ) { Text("Join", fontWeight = FontWeight.Bold, color = if (!setupModeCreate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = setupShopCode,
                            onValueChange = { setupShopCode = it.uppercase() },
                            label = { Text("Shop Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(Modifier.height(10.dp))
                        if (setupModeCreate) {
                            OutlinedTextField(
                                value = setupShopName,
                                onValueChange = { setupShopName = it },
                                label = { Text("Shop Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            OutlinedTextField(
                                value = setupShopSecret,
                                onValueChange = { setupShopSecret = it.uppercase() },
                                label = { Text("Shop Secret") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val options = ScanOptions()
                                    options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    options.setPrompt("Scan shop QR code")
                                    options.setBeepEnabled(false)
                                    options.setOrientationLocked(true)
                                    qrScanLauncher.launch(options)
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scan QR Code", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                        if (setupError.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(setupError, color = Color(0xFFDC2626), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { setupError = ""
                                if (setupModeCreate) viewModel.createFirebaseShop(setupShopCode.trim(), setupShopName.trim())
                                else viewModel.joinFirebaseShop(setupShopCode.trim(), setupShopSecret.trim())
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = shopSetupState !is ShopSetupState.Loading && setupShopCode.isNotBlank() && (setupModeCreate || setupShopSecret.isNotBlank())
                        ) {
                            Text(if (shopSetupState is ShopSetupState.Loading) "Please wait..." else if (setupModeCreate) "Create Shop" else "Join Shop", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { showSetupDialog = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

    if (showResetDialog) {
        DialogOverlay(onDismiss = { showResetDialog = false }) {
            Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Enter your email to receive a password reset link.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = resetEmail,
                onValueChange = { resetEmail = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogCancelButton(onClick = { showResetDialog = false }, modifier = Modifier.weight(1f))
                DialogConfirmButton(
                    text = "Send",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        viewModel.sendPasswordReset(resetEmail)
                        showResetDialog = false
                        resetEmail = ""
                    }
                )
            }
        }
    }
}
