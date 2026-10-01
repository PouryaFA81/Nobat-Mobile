package app.nobat.mobile

import android.app.Application
import app.nobat.mobile.data.AppDatabase

class NobatApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
}
