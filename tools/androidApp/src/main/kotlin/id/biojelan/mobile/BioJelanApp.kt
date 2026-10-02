package id.biojelan.mobile

import android.app.Application
import id.biojelan.app.di.initKoin

class BioJelanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
