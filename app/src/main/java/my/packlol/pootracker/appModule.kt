package my.packlol.pootracker

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopBase
import my.packlol.pootracker.local.dataStore
import my.packlol.pootracker.repository.AuthRepository
import my.packlol.pootracker.repository.PoopLogRepository
import my.packlol.pootracker.sync.FirebaseSyncManager
import my.packlol.pootracker.sync.FirebaseSyncer
import my.packlol.pootracker.ui.MainVM
import my.packlol.pootracker.ui.auth.AuthVM
import my.packlol.pootracker.ui.home.HomeVM
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.compose.viewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val appModule = module {

    viewModel {
        MainVM(get(), get())
    }

    single {
        AuthRepository(get(), get())
    }

    viewModel {
        AuthVM(get(), get())
    }

    viewModel {
        HomeVM(get(), get(), get())
    }

    single {
        PoopApi(get())
    }

    single {
        Firebase.auth(get<FirebaseApp>())
    }

    single {
        Firebase.firestore(get<FirebaseApp>())
    }

    single {
        FirebaseApp.initializeApp(androidContext())
    }

    singleOf(::PoopLogRepository)

    single { NetworkMonitor(androidContext()) }

    single {
        FirebaseSyncManager(androidContext())
    }

    worker {
        FirebaseSyncer(get(), get())
    }

    single { Gson() }

    single {
        DataStore(androidContext().dataStore, get())
    }

    single {
        get<PoopBase>().poopDao()
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