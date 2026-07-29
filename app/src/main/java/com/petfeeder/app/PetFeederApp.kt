package com.petfeeder.app

import android.app.Application

class PetFeederApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        BootReceiver.scheduleDailyMotivation(this)
    }
}
