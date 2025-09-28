package xyz.koleno.internetradioplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import xyz.koleno.internetradioplayer.ui.App
import xyz.koleno.internetradioplayer.ui.main.MainScreenContract
import xyz.koleno.internetradioplayer.ui.main.MainScreenViewModel

// TODO request notification permission
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainScreenViewModel by viewModels()
    private lateinit var streamingService: StreamingService
    private lateinit var serviceIntent: Intent

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as StreamingService.StreamingServiceBinder
            streamingService = binder.getService()
            streamingService.setListener { text ->
                viewModel.setEvent(MainScreenContract.Event.RadioTextReceived(text))
            }

            streamingService.setErrorListener { error ->
                // TODO: send to viewmodel
                Toast.makeText(this@MainActivity, error.orEmpty(), Toast.LENGTH_LONG).show()
            }

            streamingService.setPlayListener {
                viewModel.setEvent(MainScreenContract.Event.PlayStarted)
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