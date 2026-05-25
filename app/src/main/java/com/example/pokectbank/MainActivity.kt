package com.example.pokectbank

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PocketBankApp() }
    }
}

const val BASE_URL = "https://unmotionable-omar-overtruly.ngrok-free.dev"

@Composable
fun PocketBankApp() {

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("bank_app", Context.MODE_PRIVATE)
    }

    var loggedIn by remember {
        mutableStateOf(prefs.getBoolean("loggedIn", false))
    }

    if (loggedIn) {
        HomeScreen(
            prefs = prefs,
            onLogout = {
                prefs.edit().clear().apply()
                loggedIn = false
            }
        )
    } else {
        LoginUI(
            prefs = prefs,
            onLoginSuccess = { loggedIn = true }
        )
    }
}

@Composable
fun LoginUI(
    prefs: SharedPreferences,
    onLoginSuccess: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf("") }
    var lastLogin by remember { mutableStateOf("") }

    var otpSent by remember { mutableStateOf(false) }

    var showPopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("POCKET BANK LOGIN", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = account,
            onValueChange = { account = it.filter { c -> c.isDigit() }.take(6) },
            label = { Text("Last 6 Digit Account") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                checkUser(account) { success, n, p, e, img, login ->
                    scope.launch {
                        if (success) {
                            name = n
                            phone = p
                            email = e
                            pic = img
                            lastLogin = login
                            showPopup = true
                        } else {
                            Toast.makeText(context, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Account")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {},
            enabled = false,
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                sendOtp(account, phone) { success, msg ->
                    scope.launch {
                        otpSent = success
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send OTP")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = otp,
            onValueChange = { otp = it },
            enabled = otpSent,
            label = { Text("OTP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                verifyOtp(account, otp) { success, msg ->
                    scope.launch {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

                        if (success) {
                            prefs.edit()
                                .putBoolean("loggedIn", true)
                                .putString("name", name)
                                .putString("phone", phone)
                                .putString("email", email)
                                .putString("pic", pic)
                                .putString("account", account)
                                .putString("lastLogin", lastLogin)
                                .apply()

                            onLoginSuccess()
                        }
                    }
                }
            },
            enabled = otpSent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify OTP")
        }
    }

    if (showPopup) {
        Dialog(onDismissRequest = { showPopup = false }) {
            Card {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = pic,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.height(10.dp))

                    Text("Name: $name")
                    Text("Email: $email")
                    Text("Phone: $phone")

                    Button(onClick = { showPopup = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: SharedPreferences,
    onLogout: () -> Unit
) {

    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val name = prefs.getString("name", "") ?: ""
    val phone = prefs.getString("phone", "") ?: ""
    val email = prefs.getString("email", "") ?: ""
    val pic = prefs.getString("pic", "") ?: ""
    val account = prefs.getString("account", "") ?: ""
    val lastLogin = prefs.getString("lastLogin", "") ?: ""

    var showProfile by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {

                Spacer(Modifier.height(20.dp))

                Text("MENU", modifier = Modifier.padding(16.dp))

                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = { onLogout() }
                )

                Button(
                    onClick = {
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                ) {
                    Text("Close Menu")
                }
            }
        }
    ) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pocket Bank") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showProfile = true }) {
                            Icon(Icons.Default.Person, null)
                        }
                    }
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text("Welcome $name")
                AsyncImage(
                    model = pic,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }

    if (showProfile) {
        Dialog(onDismissRequest = { showProfile = false }) {
            Card {
                Column(Modifier.padding(20.dp)) {

                    AsyncImage(
                        model = pic,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Text("Name: $name")
                    Text("Phone: $phone")
                    Text("Email: $email")
                    Text("Account: $account")
                    Text("Last Login: $lastLogin")

                    Button(onClick = { showProfile = false }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/* ================= API ================= */

fun checkUser(account: String, cb: (Boolean, String, String, String, String, String) -> Unit) {
    val client = OkHttpClient()

    val json = JSONObject().apply { put("accountLast6", account) }

    val req = Request.Builder()
        .url("$BASE_URL/check-user")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(req).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            cb(false, "", "", "", "", "")
        }

        override fun onResponse(call: Call, response: Response) {
            val res = JSONObject(response.body?.string() ?: "{}")

            cb(
                res.optBoolean("success"),
                res.optString("name"),
                res.optString("number"),
                res.optString("email"),
                res.optString("pic"),
                res.optString("lastLogin")
            )
        }
    })
}

fun sendOtp(account: String, phone: String, cb: (Boolean, String) -> Unit) {
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("accountLast6", account)
        put("number", phone)
    }

    val req = Request.Builder()
        .url("$BASE_URL/login-send-otp")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(req).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            cb(false, "Error")
        }

        override fun onResponse(call: Call, response: Response) {
            cb(true, "OTP Sent")
        }
    })
}

fun verifyOtp(account: String, otp: String, cb: (Boolean, String) -> Unit) {
    val client = OkHttpClient()

    val json = JSONObject().apply {
        put("accountLast6", account)
        put("otp", otp)
    }

    val req = Request.Builder()
        .url("$BASE_URL/login-verify-otp")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(req).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            cb(false, "Error")
        }

        override fun onResponse(call: Call, response: Response) {
            val res = JSONObject(response.body?.string() ?: "{}")
            cb(res.optBoolean("success"), res.optString("message", "Done"))
        }
    })
}