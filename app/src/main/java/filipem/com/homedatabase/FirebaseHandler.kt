package filipem.com.homedatabase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.*
import java.util.logging.Handler
import kotlin.collections.ArrayList

class FirebaseHandler (var db: FirebaseFirestore, var user: FirebaseUser, var mainActivity: Home, val handlerRV: RVHandler) {

    val TAG: String = "FirebaseHandler"

    fun getItems (): Unit{

        /*Get items from firestore*/
        db.collection("users")
                .document(user.uid)
                .collection("items").get()
                .addOnSuccessListener { result ->
                    /*Create new item list*/
                    var itemList: ArrayList<Item> = arrayListOf()

                    for (document in result){
                        Log.d(TAG, "Received ${document.get("item_name")}")

                        /*Add all received items to list*/
                        val item: Item = Item("", document.get("item_name") as String,
                                                document.get("item_quantity") as Long,
                                                document.id, document.get("item_category") as String,
                                                document.get("item_subcategory") as String)

                        itemList.add(item)
                    }

                    mainActivity.itemList.clear()
                    mainActivity.itemList.addAll(itemList.asIterable())

                    handlerRV.updateList(mainActivity.itemList)
                }
    }





    fun getCategories (language: String): Unit{
        db.collection("categories").get()
                .addOnSuccessListener { result ->
                    /*Create new category list*/
                    var categoryList: ArrayList<String> = arrayListOf()

                    for (document in result){
                        /*Skip test category*/
                        if (document.id.matches("testcategory".toRegex())) continue

                        if (document.get("name_"+language) != null){
                            Log.d(TAG, "Found localized category (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.get("name_"+language) as String)
                        }
                        else{
                            Log.d(TAG, "Did not find localized category (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.id)
                        }
                    }

                    mainActivity.categories.clear()
                    mainActivity.categories.addAll(categoryList)
                    mainActivity.dataAdapterMain.notifyDataSetChanged()
                }
    }





    fun getSubCategories (language: String): Unit{
        db.collection("subcategories").get()
                .addOnSuccessListener { result ->
                    /*Create new category list*/
                    var categoryList: ArrayList<String> = arrayListOf()

                    for (document in result){
                        /*Skip test subcategory*/
                        if (document.id.matches("testsubcategory".toRegex())) continue

                        if (document.get("name_"+language) != null){
                            Log.d(TAG, "Found localized subcategory (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.get("name_"+language) as String)
                        }
                        else{
                            Log.d(TAG, "Did not find localized subcategory (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.id)
                        }
                    }

                    mainActivity.subcategories.clear()
                    mainActivity.subcategories.addAll(categoryList)
                    mainActivity.dataAdapterSub.notifyDataSetChanged()
                }
    }



    fun updateItemTimestamp(barcode: String): Unit{
        db.collection("users").document(user.uid)
                .collection("items")
                .document(barcode)
                .update("updated", System.currentTimeMillis() / 1000L)
                .addOnSuccessListener {
                    Log.d(TAG, "Timestamp of tem with barcode => " +
                            barcode + " updated")
                }.addOnFailureListener{
                    Log.d(TAG, "Failed to update timestamp of tem with barcode => $barcode")
                }
    }




    fun pushNewItem(barcode: String, data: Map<String, Any>): Unit{
        db.collection("users").document(user.uid)
                .collection("items")
                .document(barcode)
                .set(data)
                .addOnCompleteListener{
                    Toast.makeText(mainActivity, "Item added", Toast.LENGTH_LONG).show()

                    val item = Item("", mainActivity.data.get("item_name") as String,
                                    (mainActivity.data.get("item_quantity") as Int).toLong(),
                                    barcode, mainActivity.data.get("item_category") as String,
                                    mainActivity.data.get("item_subcategory") as String)

                    if (mainActivity.cardsAdapter.items.isEmpty()) {
                        mainActivity.cardsAdapter.items.add(item)
                        mainActivity.cardsAdapter.notifyItemInserted(mainActivity.cardsAdapter.items.size - 1)
                    } else {
                        mainActivity.cardsAdapter.items.add(0, item)
                        mainActivity.cardsAdapter.notifyItemInserted(0)
                    }
                }
    }




    fun updateItem(barcode: String, data: Map<String, Any>): Unit{
        db.collection("users").document(user.uid)
                .collection("items")
                .document(barcode)
                .update(data)
                .addOnSuccessListener {
                    Toast.makeText(mainActivity, "Item added", Toast.LENGTH_LONG).show()
                }
    }
}