package my.packlol.pootracker

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.local.DataStore
import my.packlol.pootracker.local.PoopBase
import my.packlol.pootracker.local.dataStore
import my.packlol.pootracker.sync.FirebaseSyncManager
import my.packlol.pootracker.sync.FirebaseSyncer
import my.packlol.pootracker.ui.MainVM
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.module


val appModule = module {

    viewModel {
        MainVM(
            dao = get(),
            poopApi = get(),
            syncManager = get()
        )
    }

    single {
        PoopApi(get())
    }

    single {
        Firebase.firestore
    }

    single {
        FirebaseSyncManager(androidContext())
    }

    worker {
        FirebaseSyncer(get(), get())
    }

    single {
        DataStore(androidContext().dataStore)
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