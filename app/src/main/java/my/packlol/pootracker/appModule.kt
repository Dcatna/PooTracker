package my.packlol.pootracker

import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopBase
import my.packlol.pootracker.local.dataStore
import my.packlol.pootracker.notification.PoopPredictionNotifier
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import my.packlol.pootracker.sync.FirebaseSyncer
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.auth.AuthVM
import my.packlol.pootracker.ui.home.HomeVM
import my.packlol.pootracker.ui.home.PoopChartVM
import my.packlol.pootracker.ui.stats.StatsVM
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val appModule = module {

    viewModelOf(::MainVM)

    singleOf(::AuthRepository)

    viewModelOf(::AuthVM)

    viewModelOf(::HomeVM)

    viewModelOf(::StatsVM)

    single {
        PoopApi(get())
    }

    single { Firebase.auth(get<FirebaseApp>()) }

    single { Firebase.analytics }

    single { Firebase.firestore(get<FirebaseApp>()) }

    single { FirebaseApp.initializeApp(androidContext()) }

    singleOf(::PoopLogRepository)

    single { NetworkMonitor(androidContext()) }

    single { FirebaseSyncManager(androidContext()) }

    worker { FirebaseSyncer(get(), get()) }

    worker { PoopPredictionNotifier(get(), get()) }

    single { Gson() }

    viewModelOf(::PoopChartVM)

    single {
        DataStore(androidContext().dataStore, get())
    }

    single {
        get<PoopBase>().poopLogDao()
    }

    single {
        get<PoopBase>().offlineDeletedDao()
    }

    single {
        get<PoopBase>().poopCollectionDao()
    }

    single {
        Room.databaseBuilder(
            androidContext(),
            PoopBase::class.java,
            "Poop.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}