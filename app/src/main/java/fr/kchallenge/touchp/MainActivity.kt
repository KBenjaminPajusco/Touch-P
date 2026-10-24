package fr.kchallenge.touchp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.kchallenge.touchp.ui.CameraScreen
import fr.kchallenge.touchp.ui.MainViewModel

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // `adb shell am start -n ... --ez debug true` : démarre directement en mode debug (CI / tests).
        val startInDebug = intent.getBooleanExtra(EXTRA_DEBUG, false)
        if (startInDebug) vm.setDebug(true)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var granted by remember {
                    mutableStateOf(
                        startInDebug || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED,
                    )
                }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
                LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

                if (granted) {
                    CameraScreen(vm)
                } else {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("La caméra est nécessaire pour suivre ta main.")
                        Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Autoriser") }
                        // Le mode debug fonctionne sans caméra.
                        Button(onClick = { granted = true; vm.setDebug(true) }) { Text("Continuer en mode debug") }
                    }
                }
            }
        }
    }

    private companion object {
        const val EXTRA_DEBUG = "debug"
    }
}
