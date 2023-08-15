package my.packlol.pootracker

import androidx.room.Room
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import my.packlol.pootracker.firebase.PoopApi
import my.packlol.pootracker.ui.theme.PoopBase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel {
        Collector(
            dao = get(),
            poopApi = get()
        )
    }

    single {
        PoopApi(get())
    }

    single {
        Firebase.firestore
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