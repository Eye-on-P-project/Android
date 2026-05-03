package ac.sbmax002.eye_on

import android.app.Application
import ac.sbmax002.eye_on.network.NetworkConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EyeOnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkConfig.initialize(this)
    }
}

