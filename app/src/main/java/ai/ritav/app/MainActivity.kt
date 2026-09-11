package ai.ritav.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var safeMode by mutableStateOf(false)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Ritav", style = MaterialTheme.typography.displaySmall)
                    Text(
                        if (safeMode) "Safe Mode active" else "Privacy-first local AI foundation",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Button(
                        onClick = { safeMode = !safeMode },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text(if (safeMode) "Disable Safe Mode" else "Emergency Stop")
                    }
                }
            }
        }
    }
}
