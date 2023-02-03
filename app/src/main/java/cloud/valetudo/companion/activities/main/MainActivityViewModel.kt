package cloud.valetudo.companion.activities.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import cloud.valetudo.companion.repositories.ValetudoInstancesRepository

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val valetudoInstancesRepository = ValetudoInstancesRepository
        .fromContext(application.applicationContext)


    val devicesLiveData = valetudoInstancesRepository.valetudoInstances.asLiveData()
}
