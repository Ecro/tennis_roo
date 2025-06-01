package com.example.tennis_roo.watch_app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.material.*
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.tennis_roo.data_store.entity.StrokeType
import com.example.tennis_roo.watch_app.R
import com.example.tennis_roo.watch_app.presentation.theme.TennisRooTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: MainViewModel
    private val TAG = "MainActivity"
    
    // Required permissions
    private val requiredPermissions = arrayOf(
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.ACTIVITY_RECOGNITION,
        Manifest.permission.HIGH_SAMPLING_RATE_SENSORS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "All permissions granted")
        } else {
            Log.d(TAG, "Some permissions denied: ${permissions.filter { !it.value }.keys}")
            // Show a message to the user that the app may not work properly without permissions
            viewModel.updateDebugInfo("Some permissions denied. App may not work properly.")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Request permissions
        requestPermissions()
        
        // Set up content
        setContent {
            TennisRooApp(viewModel)
        }
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            Log.d(TAG, "All permissions already granted")
        }
    }
}

@Composable
fun TennisRooApp(viewModel: MainViewModel) {
    TennisRooTheme {
        val serviceRunningState = viewModel.serviceRunning.collectAsState()
        val testModeState = viewModel.testModeEnabled.collectAsState()
        val lastPointEventState = viewModel.lastPointEvent.collectAsState()
        val debugInfoState = viewModel.debugInfo.collectAsState()
        val player1ScoreState = viewModel.player1Score.collectAsState()
        val player2ScoreState = viewModel.player2Score.collectAsState()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // Time text at the top
            TimeText()
            
            // Scrollable content
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 32.dp,
                    bottom = 8.dp
                )
            ) {
                // Service controls
                item {
                    Card(
                        onClick = { viewModel.toggleService(testModeState.value) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (serviceRunningState.value) 
                                stringResource(R.string.stop_service) 
                            else 
                                stringResource(R.string.start_service),
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                // Test mode toggle
                item {
                    Card(
                        onClick = { viewModel.toggleTestMode(!testModeState.value) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (testModeState.value) 
                                stringResource(R.string.test_mode_on) 
                            else 
                                stringResource(R.string.test_mode_off),
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Score display
                item {
                    Card(
                        onClick = { /* No action needed */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "P1: ${player1ScoreState.value}",
                                style = MaterialTheme.typography.title2
                            )
                            Text(
                                text = "P2: ${player2ScoreState.value}",
                                style = MaterialTheme.typography.title2
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Stroke simulation buttons (only visible in test mode)
                if (testModeState.value) {
                    item {
                        Text(
                            text = stringResource(R.string.simulate_strokes),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.title3
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Player 1 strokes
                    item {
                        Text(
                            text = stringResource(R.string.player_1),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StrokeButton(
                                text = stringResource(R.string.serve),
                                strokeType = StrokeType.SERVE,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_1,
                                viewModel = viewModel
                            )
                            StrokeButton(
                                text = stringResource(R.string.forehand),
                                strokeType = StrokeType.FOREHAND,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_1,
                                viewModel = viewModel
                            )
                            StrokeButton(
                                text = stringResource(R.string.backhand),
                                strokeType = StrokeType.BACKHAND,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_1,
                                viewModel = viewModel
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Player 2 strokes
                    item {
                        Text(
                            text = stringResource(R.string.player_2),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StrokeButton(
                                text = stringResource(R.string.serve),
                                strokeType = StrokeType.SERVE,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_2,
                                viewModel = viewModel
                            )
                            StrokeButton(
                                text = stringResource(R.string.forehand),
                                strokeType = StrokeType.FOREHAND,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_2,
                                viewModel = viewModel
                            )
                            StrokeButton(
                                text = stringResource(R.string.backhand),
                                strokeType = StrokeType.BACKHAND,
                                player = com.example.tennis_roo.ml_gesture.Player.PLAYER_2,
                                viewModel = viewModel
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // Last point info
                item {
                    Card(
                        onClick = { /* No action needed */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.last_point),
                                style = MaterialTheme.typography.title3,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val pointEvent = lastPointEventState.value
                            if (pointEvent != null) {
                                Text(
                                    text = "Type: ${pointEvent.type}",
                                    style = MaterialTheme.typography.body2
                                )
                                Text(
                                    text = "Player: ${pointEvent.player}",
                                    style = MaterialTheme.typography.body2
                                )
                                Text(
                                    text = "Confidence: ${pointEvent.confidence}",
                                    style = MaterialTheme.typography.body2
                                )
                                
                                // Button to record this point
                                Button(
                                    onClick = { viewModel.recordPoint(pointEvent) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.record_point))
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_point_detected),
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Debug info
                if (testModeState.value) {
                    item {
                        Card(
                            onClick = { /* No action needed */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.debug_info),
                                    style = MaterialTheme.typography.title3,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = debugInfoState.value,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StrokeButton(
    text: String,
    strokeType: StrokeType,
    player: com.example.tennis_roo.ml_gesture.Player,
    viewModel: MainViewModel
) {
    Button(
        onClick = { viewModel.simulateStroke(strokeType, player) },
        modifier = Modifier.size(width = 60.dp, height = 40.dp)
    ) {
        Text(text, style = MaterialTheme.typography.button)
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TennisRooTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = "Tennis Roo"
            )
        }
    }
}
