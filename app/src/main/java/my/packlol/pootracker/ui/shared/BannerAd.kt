package my.packlol.pootracker.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    test: Boolean = true
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                if (test) {
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                } else {
                    adUnitId = "ca-app-pub-8507003000701707~6713023717"
                }
                loadAd(
                    AdRequest.Builder()
                        .build()
                )
            }
        },
        update = {

        }
    )
}