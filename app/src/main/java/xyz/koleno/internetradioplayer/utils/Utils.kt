package xyz.koleno.internetradioplayer.utils

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

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

/**
 * This will totally invalidate the point of having https connections, but it is sometimes
 * needed for stations with invalid certificates
 */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
fun validateAllCertificates() {

    val trustManager = object : X509TrustManager {

        override fun checkClientTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) {
        }

        override fun checkServerTrusted(
            chain: Array<out X509Certificate>?,
            authType: String?
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate>? {
            return null
        }

    }

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, arrayOf(trustManager), SecureRandom())

    HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
}

/**
 * Restores back default certificate validation
 */
fun restoreDefaultCertificateValidation() {
    HttpsURLConnection.setDefaultSSLSocketFactory(SSLSocketFactory.getDefault() as SSLSocketFactory)
    HttpsURLConnection.setDefaultHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier())
}