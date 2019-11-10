package filipem.com.homedatabase

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.StorageReference

class RVHandler (val recyclerView: RecyclerView, var mainActivity: Home, var imagesRef: StorageReference) {

    val TAG: String = "RVHandler"



    fun updateList(itemList: ArrayList<Item>): Unit{
        mainActivity.cardsAdapter = ItemsCardsAdapter(itemList, mainActivity, imagesRef)
        recyclerView.adapter = mainActivity.cardsAdapter

        /*If list has items, remove 'no items here' text*/
        if (!itemList.isEmpty()) mainActivity.noItemsText.visibility = View.GONE
        else {
            Toast.makeText(mainActivity, "No items found", Toast.LENGTH_LONG).show()
        }

        if (mainActivity.mSwipeRefreshLayoutItems.isRefreshing){
            mainActivity.mSwipeRefreshLayoutItems.isRefreshing = false
        }
    }



    fun setVisibility(visibility: Int): Unit{
        mainActivity.recyclerViewItems.visibility = visibility
    }




    fun clearItemList(): Unit{
        if (mainActivity.itemList != null) {
            mainActivity.itemList.clear()
            Log.i(TAG, "Cleared itemslist data")
        } else
            mainActivity.itemList = java.util.ArrayList<Item>()
    }
}