package my.packlol.pootracker.onboarding

import androidx.lifecycle.ViewModel
import my.packlol.pootracker.local.DataStore

class OnboardingVM(
    private val dataStore: DataStore
): ViewModel() {


    private val userPrefs = dataStore.userPrefs()
}