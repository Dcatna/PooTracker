package my.packlol.pootracker

import androidx.room.Room
import my.packlol.pootracker.ui.theme.PoopBase
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    viewModel {
        Collector(
            dao = get()
        )
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