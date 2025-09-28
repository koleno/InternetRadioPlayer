package xyz.koleno.internetradioplayer.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Remembers the result of [flowWithLifecycle]. Updates the value if the [flow]
 * or [lifecycle] changes. Cancels collection in onStop() and start it in onStart()
 *
 * See https://proandroiddev.com/consuming-flows-safely-in-non-composable-scopes-in-jetpack-compose-d0154565bd68
 * and https://github.com/worldline/Compose-MVI/blob/main/presentation/src/main/java/com/worldline/composemvi/presentation/utils/ComposeUtils.kt
 *
 * @param flow The [Flow] that is going to be collected.
 * @param lifecycle The [lifecycle] to validate the [Lifecycle.State] from
 *
 * @return [Flow] with the remembered value of type [T]
 */
@Composable
fun <T> rememberFlowWithLifecycle(
    // 1
    flow: Flow<T>,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
): Flow<T> {
    return remember(flow, lifecycle) { // 2
        flow.flowWithLifecycle( // 3
            lifecycle, // 4
            Lifecycle.State.STARTED // 5
        )
    }
}