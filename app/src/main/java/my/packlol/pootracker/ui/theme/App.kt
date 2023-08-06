package my.packlol.pootracker.ui.theme

import android.app.Application


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DB.createDB(this.applicationContext)
    }
}