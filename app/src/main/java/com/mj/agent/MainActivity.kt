package com.mj.agent

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        MJBrain.refreshSetup(applicationContext)
    }

    private val modelPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) MJBrain.importModel(uri)
    }

    private fun askPermissions() {
        permLauncher.launch(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MJBrain.requestPermissions = { askPermissions() }
        MJBrain.loadModelAsync()
        setContent {
            MJTheme {
                MainScreen(
                    onPickModel = { modelPicker.launch(arrayOf("*/*")) },
                    onPerms = { askPermissions() },
                    onA11y = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onAppInfo = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                        )
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        MJBrain.refreshSetup(applicationContext)
    }

    override fun onDestroy() {
        MJBrain.requestPermissions = null
        super.onDestroy()
    }
}
