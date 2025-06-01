package com.example.tennis_roo.watch_app.presentation

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tennis_roo.data_store.entity.MatchFormat
import com.example.tennis_roo.data_store.entity.Player
import com.example.tennis_roo.data_store.entity.StrokeType
import com.example.tennis_roo.data_store.repository.MatchRepository
import com.example.tennis_roo.ml_gesture.PointEvent
import com.example.tennis_roo.watch_app.service.SensorService
import com.example.tennis_roo.watch_app.service.SensorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen of the Tennis Roo app.
 * Handles communication with the SensorService and the data repository.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "MainViewModel"
    
    // Repository for match data
    private val matchRepository = MatchRepository(application)
    
    // Current match and game IDs
    private var currentMatchId: Long = -1
    private var currentGameId: Long = -1
    
    // Service state
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()
    
    private val _testModeEnabled = MutableStateFlow(false)
    val testModeEnabled: StateFlow<Boolean> = _testModeEnabled.asStateFlow()
    
    private val _lastPointEvent = MutableStateFlow<PointEvent?>(null)
    val lastPointEvent: StateFlow<PointEvent?> = _lastPointEvent.asStateFlow()
    
    private val _debugInfo = MutableStateFlow("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()
    
    /**
     * Updates the debug info text.
     *
     * @param info The debug info text
     */
    fun updateDebugInfo(info: String) {
        _debugInfo.value = info
    }
    
    // Match state
    private val _player1Score = MutableStateFlow(0)
    val player1Score: StateFlow<Int> = _player1Score.asStateFlow()
    
    private val _player2Score = MutableStateFlow(0)
    val player2Score: StateFlow<Int> = _player2Score.asStateFlow()
    
    // Service connection
    private var sensorService: SensorService? = null
    private var isBound = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected")
            val binder = service as SensorService.LocalBinder
            sensorService = binder.getService()
            isBound = true
            
            // Start collecting state from the service
            collectServiceState()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected")
            sensorService = null
            isBound = false
        }
    }
    
    init {
        // Initialize match data
        initializeMatchData()
        
        // Start collecting service state
        startServiceStateCollection()
    }
    
    /**
     * Starts collecting service state from the SensorService.
     */
    private fun startServiceStateCollection() {
        viewModelScope.launch {
            try {
                // Bind to the service
                val context = getApplication<Application>()
                val intent = Intent(context, SensorService::class.java)
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                
                Log.d(TAG, "Started service state collection")
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting service state", e)
            }
        }
    }
    
    /**
     * Collects state from the bound service.
     */
    private fun collectServiceState() {
        val service = sensorService ?: return
        
        viewModelScope.launch {
            try {
                // Collect sensor state
                service.sensorState.collectLatest { state ->
                    when (state) {
                        is SensorState.Running -> {
                            _serviceRunning.value = true
                        }
                        is SensorState.Idle -> {
                            _serviceRunning.value = false
                        }
                        is SensorState.Error -> {
                            _serviceRunning.value = false
                            _debugInfo.value = "Sensor error: ${state.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting sensor state", e)
            }
        }
        
        viewModelScope.launch {
            try {
                // Collect last point event
                service.lastPointEvent.collectLatest { event ->
                    if (event != null) {
                        _lastPointEvent.value = event
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting point events", e)
            }
        }
        
        viewModelScope.launch {
            try {
                // Collect debug info
                service.debugInfo.collectLatest { info ->
                    if (info.isNotEmpty()) {
                        _debugInfo.value = info
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting debug info", e)
            }
        }
    }
    
    /**
     * Initializes match data by creating a new match if none exists.
     */
    private fun initializeMatchData() {
        viewModelScope.launch {
            try {
                matchRepository.getCurrentMatch().collectLatest { match ->
                    if (match == null) {
                        // Create a new match
                        currentMatchId = matchRepository.createMatch(
                            player1Name = "Player 1",
                            player2Name = "Player 2",
                            format = MatchFormat.BEST_OF_3_SETS
                        )
                        
                        // Create first game
                        currentGameId = matchRepository.createGame(
                            matchId = currentMatchId,
                            server = Player.PLAYER_1
                        )
                        
                        Log.d(TAG, "Created new match: $currentMatchId, game: $currentGameId")
                    } else {
                        currentMatchId = match.id
                        
                        // Get current game
                        matchRepository.getCurrentGameForMatch(currentMatchId).collectLatest { game ->
                            if (game != null) {
                                currentGameId = game.id
                                
                                // Update scores
                                updateScores()
                            } else {
                                // Create a new game if none exists
                                currentGameId = matchRepository.createGame(
                                    matchId = currentMatchId,
                                    server = Player.PLAYER_1
                                )
                            }
                            Log.d(TAG, "Using existing match: $currentMatchId, game: $currentGameId")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing match data", e)
                _debugInfo.value = "Error initializing match data: ${e.message}"
            }
        }
    }
    
    /**
     * Updates the scores for the current game.
     */
    private fun updateScores() {
        if (currentGameId <= 0) return
        
        viewModelScope.launch {
            try {
                // Get point counts for each player
                matchRepository.getPointCountForPlayer(currentGameId, Player.PLAYER_1).collectLatest { count ->
                    _player1Score.value = count
                }
                
                matchRepository.getPointCountForPlayer(currentGameId, Player.PLAYER_2).collectLatest { count ->
                    _player2Score.value = count
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating scores", e)
            }
        }
    }
    
    /**
     * Toggles the sensor service on/off.
     *
     * @param testMode Whether to enable test mode
     */
    fun toggleService(testMode: Boolean) {
        val context = getApplication<Application>()
        
        if (_serviceRunning.value) {
            // Stop service
            val intent = Intent(context, SensorService::class.java).apply {
                action = SensorService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
            _serviceRunning.value = false
        } else {
            try {
                // Start service
                val intent = Intent(context, SensorService::class.java).apply {
                    action = SensorService.ACTION_START_SERVICE
                    putExtra(SensorService.EXTRA_TEST_MODE, testMode)
                }
                context.startService(intent)
                _serviceRunning.value = true
                _debugInfo.value = "Service started in ${if (testMode) "test" else "normal"} mode"
            } catch (e: Exception) {
                Log.e(TAG, "Error starting service", e)
                _debugInfo.value = "Error starting service: ${e.message}"
                _serviceRunning.value = false
            }
        }
    }
    
    /**
     * Toggles test mode on/off.
     *
     * @param enabled Whether to enable test mode
     */
    fun toggleTestMode(enabled: Boolean) {
        _testModeEnabled.value = enabled
        
        // Update service if it's running
        if (_serviceRunning.value) {
            val context = getApplication<Application>()
            val intent = Intent(context, SensorService::class.java).apply {
                action = SensorService.ACTION_TOGGLE_TEST_MODE
                putExtra(SensorService.EXTRA_TEST_MODE, enabled)
            }
            context.startService(intent)
        }
    }
    
    /**
     * Simulates a stroke event.
     *
     * @param strokeType The type of stroke
     * @param player The player who performed the stroke
     */
    fun simulateStroke(strokeType: StrokeType, player: com.example.tennis_roo.ml_gesture.Player) {
        if (!_serviceRunning.value || !_testModeEnabled.value) {
            return
        }
        
        val context = getApplication<Application>()
        val intent = Intent(context, SensorService::class.java).apply {
            action = SensorService.ACTION_SIMULATE_POINT
            putExtra(SensorService.EXTRA_POINT_TYPE, strokeType.name)
            putExtra(SensorService.EXTRA_PLAYER, player.name)
        }
        context.startService(intent)
        
        // Simulate receiving the point event
        val simulatedEvent = PointEvent(
            timestamp = System.currentTimeMillis(),
            type = when (strokeType) {
                StrokeType.SERVE -> com.example.tennis_roo.ml_gesture.PointType.SERVE
                StrokeType.FOREHAND -> com.example.tennis_roo.ml_gesture.PointType.FOREHAND
                StrokeType.BACKHAND -> com.example.tennis_roo.ml_gesture.PointType.BACKHAND
                StrokeType.VOLLEY -> com.example.tennis_roo.ml_gesture.PointType.VOLLEY
                StrokeType.SMASH -> com.example.tennis_roo.ml_gesture.PointType.SMASH
                else -> com.example.tennis_roo.ml_gesture.PointType.UNKNOWN
            },
            confidence = 0.95f,
            player = player
        )
        _lastPointEvent.value = simulatedEvent
        _debugInfo.value = "Simulated point: ${simulatedEvent.type} by ${simulatedEvent.player}"
    }
    
    /**
     * Records a point in the database.
     *
     * @param pointEvent The point event to record
     */
    fun recordPoint(pointEvent: PointEvent) {
        if (currentGameId <= 0) {
            Log.e(TAG, "Cannot record point: no active game")
            _debugInfo.value = "Cannot record point: no active game"
            return
        }
        
        viewModelScope.launch {
            try {
                // Convert from ml_gesture.Player to data_store.entity.Player
                val player = when (pointEvent.player) {
                    com.example.tennis_roo.ml_gesture.Player.PLAYER_1 -> Player.PLAYER_1
                    com.example.tennis_roo.ml_gesture.Player.PLAYER_2 -> Player.PLAYER_2
                }
                
                // Convert from ml_gesture.PointType to data_store.entity.StrokeType
                val strokeType = when (pointEvent.type) {
                    com.example.tennis_roo.ml_gesture.PointType.SERVE -> StrokeType.SERVE
                    com.example.tennis_roo.ml_gesture.PointType.FOREHAND -> StrokeType.FOREHAND
                    com.example.tennis_roo.ml_gesture.PointType.BACKHAND -> StrokeType.BACKHAND
                    com.example.tennis_roo.ml_gesture.PointType.VOLLEY -> StrokeType.VOLLEY
                    com.example.tennis_roo.ml_gesture.PointType.SMASH -> StrokeType.SMASH
                    else -> StrokeType.UNKNOWN
                }
                
                val pointId = matchRepository.recordPoint(
                    gameId = currentGameId,
                    winner = player,
                    strokeType = strokeType,
                    confidence = pointEvent.confidence
                )
                
                Log.d(TAG, "Recorded point: $pointId")
                _debugInfo.value = "Recorded point: $pointId"
                
                // Update scores
                updateScores()
            } catch (e: Exception) {
                Log.e(TAG, "Error recording point", e)
                _debugInfo.value = "Error recording point: ${e.message}"
            }
        }
    }
    
    /**
     * Undoes the last point.
     */
    fun undoLastPoint() {
        if (currentGameId <= 0) {
            Log.e(TAG, "Cannot undo point: no active game")
            _debugInfo.value = "Cannot undo point: no active game"
            return
        }
        
        viewModelScope.launch {
            try {
                matchRepository.undoLastPoint(currentGameId)
                Log.d(TAG, "Undid last point")
                _debugInfo.value = "Undid last point"
                
                // Update scores
                updateScores()
            } catch (e: Exception) {
                Log.e(TAG, "Error undoing point", e)
                _debugInfo.value = "Error undoing point: ${e.message}"
            }
        }
    }
    
    /**
     * Creates a new game.
     *
     * @param server The player who is serving
     */
    fun createNewGame(server: Player) {
        if (currentMatchId <= 0) {
            Log.e(TAG, "Cannot create game: no active match")
            _debugInfo.value = "Cannot create game: no active match"
            return
        }
        
        viewModelScope.launch {
            try {
                currentGameId = matchRepository.createGame(
                    matchId = currentMatchId,
                    server = server
                )
                
                Log.d(TAG, "Created new game: $currentGameId")
                _debugInfo.value = "Created new game: $currentGameId"
                
                // Reset scores
                _player1Score.value = 0
                _player2Score.value = 0
            } catch (e: Exception) {
                Log.e(TAG, "Error creating game", e)
                _debugInfo.value = "Error creating game: ${e.message}"
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Stop service if it's running
        if (_serviceRunning.value) {
            val context = getApplication<Application>()
            val intent = Intent(context, SensorService::class.java).apply {
                action = SensorService.ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
