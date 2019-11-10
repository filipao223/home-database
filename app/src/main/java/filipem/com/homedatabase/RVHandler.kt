package filipem.com.homedatabase

import android.app.Activity
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.StorageReference

class RVHandler (val recyclerView: RecyclerView, var mainActivity: Home, var imagesRef: StorageReference) {

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
}