package com.nas.neo.fwc.app

import android.app.Application
import android.os.Handler
import com.rulerbug.bugutils.nas_library_utils.Utils.NasApp

class FWCApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NasApp.init(this )
    }
}