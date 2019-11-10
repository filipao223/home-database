package filipem.com.homedatabase

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat.getSystemService

class NetworkHandler (val mainActivity: Home) {

    fun isNetworkConnected(): Boolean {
        val cm = mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null
    }
}