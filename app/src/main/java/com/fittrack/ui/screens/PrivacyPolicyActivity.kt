package com.fittrack.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fittrack.ui.theme.FitTrackTheme

/**
 * Privacy policy activity required by Health Connect
 */
class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitTrackTheme {
                PrivacyPolicyScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "FitTrack Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Last updated: ${java.time.LocalDate.now()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PolicySection(
                title = "Health Data",
                content = """
                    FitTrack uses Health Connect to read and write health and fitness data. This includes:
                    
                    • Steps and distance walked
                    • Calories burned (active and total)
                    • Heart rate measurements
                    • Exercise sessions
                    • Sleep data
                    • Weight records
                    • Nutrition information
                    
                    This data is stored locally on your device and is used solely to provide you with fitness tracking functionality within the app.
                """.trimIndent()
            )
            
            PolicySection(
                title = "Data Storage",
                content = """
                    All health data accessed through Health Connect remains on your device. FitTrack does not transmit your health data to any external servers.
                    
                    Nutrition data from food searches may be retrieved from the Open Food Facts database, but your personal meal logs remain on your device.
                """.trimIndent()
            )
            
            PolicySection(
                title = "Data Sharing",
                content = """
                    FitTrack does not share your personal health or fitness data with third parties. Your data belongs to you.
                """.trimIndent()
            )
            
            PolicySection(
                title = "Data Deletion",
                content = """
                    You can delete your data at any time by:
                    
                    • Clearing app data through Android settings
                    • Uninstalling the app
                    • Revoking Health Connect permissions
                    
                    When you revoke permissions or uninstall the app, FitTrack can no longer access your Health Connect data.
                """.trimIndent()
            )
            
            PolicySection(
                title = "Contact",
                content = """
                    If you have questions about this privacy policy or how your data is handled, please contact us through the app's feedback feature.
                """.trimIndent()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
