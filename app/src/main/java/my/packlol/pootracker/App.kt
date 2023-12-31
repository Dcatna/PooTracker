package my.packlol.pootracker

import android.app.Application
import com.google.android.gms.ads.MobileAds
import my.packlol.pootracker.notification.NotificationStarter
import my.packlol.pootracker.sync.SyncStarter
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin


class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            workManagerFactory()
            modules(appModule)
        }

        MobileAds.initialize(this)

        NotificationStarter.start(this)

        SyncStarter.start(this)
    }
}