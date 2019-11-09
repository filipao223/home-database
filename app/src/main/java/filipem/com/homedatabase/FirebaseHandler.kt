package filipem.com.homedatabase

import android.app.Activity
import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.util.logging.Handler

class FirebaseHandler (var db: FirebaseFirestore, var user: FirebaseUser, var mainActivity: Home, val handlerRV: RVHandler) {


    fun getItems (): Unit{

        /*Get items from firestore*/
        db.collection("users")
                .document(user.uid)
                .collection("items").get()
                .addOnSuccessListener { result ->
                    /*Create new item list*/
                    var itemList: ArrayList<Item> = arrayListOf()

                    for (document in result){
                        Log.d("FirebaseHandler", "Received ${document.get("item_name")}")

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
                            Log.d("FirebaseHandler", "Found localized category (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.get("name_"+language) as String)
                        }
                        else{
                            Log.d("FirebaseHandler", "Did not find localized category (" + language + "_" + document.get("name_"+language) + ")")
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
                            Log.d("FirebaseHandler", "Found localized subcategory (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.get("name_"+language) as String)
                        }
                        else{
                            Log.d("FirebaseHandler", "Did not find localized subcategory (" + language + "_" + document.get("name_"+language) + ")")
                            categoryList.add(document.id)
                        }
                    }

                    mainActivity.subcategories.clear()
                    mainActivity.subcategories.addAll(categoryList)
                    mainActivity.dataAdapterSub.notifyDataSetChanged()
                }
    }
}