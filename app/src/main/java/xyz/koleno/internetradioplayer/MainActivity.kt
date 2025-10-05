package xyz.koleno.internetradioplayer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.koleno.internetradioplayer.ui.App
import xyz.koleno.internetradioplayer.ui.main.MainScreenContract
import xyz.koleno.internetradioplayer.ui.main.MainScreenViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainScreenViewModel by viewModels()
    private lateinit var streamingService: StreamingService
    private lateinit var serviceIntent: Intent

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewModel.setEvent(MainScreenContract.Event.PermissionsGrantedClicked)
            } else {
                viewModel.setEvent(MainScreenContract.Event.PermissionsDeniedClicked)
            }
        }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as StreamingService.StreamingServiceBinder
            streamingService = binder.getService()
            streamingService.setListener { text ->
                viewModel.setEvent(MainScreenContract.Event.RadioTextReceived(text))
            }

            streamingService.setErrorListener { error ->
                viewModel.setEvent(MainScreenContract.Event.PlayError(error))
            }

            streamingService.setPlayListener {
                viewModel.setEvent(MainScreenContract.Event.PlayStarted)
            }

            streamingService.setStopListener {
                finish()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // empty
            streamingService.setListener(null)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // start streaming service
        serviceIntent = Intent(this, StreamingService::class.java)
        startService(serviceIntent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect {
                    when (it) {
                        is MainScreenContract.Effect.StartPlaying -> {
                            streamingService.play(it.station)
                        }

                        MainScreenContract.Effect.StopPlaying -> {
                            streamingService.stop()
                        }

                        is MainScreenContract.Effect.ShowToast -> {
                            // TODO: play also sound if it.sound true
                            Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_SHORT).show()
                        }

                        MainScreenContract.Effect.AskForNotificationPermissions -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        MainScreenContract.Effect.RestartService -> {
                            streamingService.showNotification()
                        }
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }


}