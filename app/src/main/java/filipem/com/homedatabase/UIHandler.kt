package filipem.com.homedatabase

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.Toast
import java.util.ArrayList

class UIHandler (val firebaseHandler: FirebaseHandler, val rvHandler: RVHandler,
                 var mainActivity: Home) {

    val TAG: String = "UIHandler"



    fun refreshHomeItems(): Unit{
        Log.i(TAG, "refreshHomeItems was called")

        //Check for internet connection
        if (NetworkHandler(mainActivity).isNetworkConnected()) {
            //Clear old data
            rvHandler.setVisibility(View.VISIBLE)
            rvHandler.clearItemList()

            //Fill with new data
            Log.i(TAG, "Filling with new data")
            Log.d(TAG, "itemCollectionExists")

            firebaseHandler.getItems()

        } else {
            Toast.makeText(mainActivity, R.string.no_network, Toast.LENGTH_LONG).show()
            Log.e(TAG, "No internet connection in refreshHomeItems")
            mainActivity.mSwipeRefreshLayoutItems.isRefreshing = false
        }
    }
}