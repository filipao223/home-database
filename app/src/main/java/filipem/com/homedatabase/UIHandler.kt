package filipem.com.homedatabase

import android.app.Activity
import android.graphics.Color
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseUser
import java.util.*

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


    fun createAddItemByHandDialog(): Unit{

        mainActivity.addByHand.setOnClickListener(View.OnClickListener {
            mainActivity.menuButtons.collapse()
            val mBuilder = AlertDialog.Builder(mainActivity)
            val mView = mainActivity.layoutInflater.inflate(R.layout.item_add_dialog_hand, null)

            /*Default locale*/
            val currentLocale = Locale.getDefault()
            val language = currentLocale.language

            /*Get main category spinner*/
            val categorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_category) as Spinner
            categorySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    (view as TextView).setTextColor(Color.BLACK)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {

                }
            })

            /*Get sub category spinner*/
            val subCategorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_subcategory) as Spinner
            subCategorySpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {

                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {

                }
            })

            mainActivity.categories = ArrayList<String>()
            mainActivity.subcategories = ArrayList<String>()
            mainActivity.dataAdapterMain = ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, mainActivity.categories)
            mainActivity.dataAdapterSub = ArrayAdapter<String>(mainActivity, android.R.layout.simple_spinner_item, mainActivity.subcategories)

            /*Get categories from server*/
            firebaseHandler.getCategories(language)

            /*Get subcategories from server*/
            firebaseHandler.getSubCategories(language)

            mainActivity.dataAdapterMain.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            mainActivity.dataAdapterSub.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            categorySpinner.adapter = mainActivity.dataAdapterMain
            categorySpinner.setSelection(0)
            subCategorySpinner.adapter = mainActivity.dataAdapterSub
            subCategorySpinner.setSelection(0)

            mainActivity.itemName = mView.findViewById(R.id.add_item_dialog_name)
            mainActivity.itemQuantity = mView.findViewById(R.id.add_item_dialog_quantity)
            mainActivity.itemBarcode = mView.findViewById(R.id.add_item_dialog_barcode)
            mainActivity.confirmButton = mView.findViewById(R.id.add_item_dialog_confirm)
            mainActivity.cancelButton = mView.findViewById(R.id.add_item_dialog_cancel)
            mainActivity.checkButton = mView.findViewById(R.id.add_item_dialog_check_barcode)

            mainActivity.cancelButton.setOnClickListener(View.OnClickListener { mainActivity.addItemConfirm.dismiss() })

            mainActivity.checkButton.setOnClickListener(View.OnClickListener {
                val barcodeString = mainActivity.itemBarcode.getText().toString()

                if (barcodeString.matches("".toRegex()) || barcodeString.matches("[^0-9]".toRegex())) {
                    Toast.makeText(mainActivity, R.string.invalid_barcode, Toast.LENGTH_LONG).show()
                } else {
                    mainActivity.addItemDialogHandler(barcodeString, categorySpinner, subCategorySpinner, false)
                }
            })

            mainActivity.confirmButton.setOnClickListener(View.OnClickListener {
                //Get barcode from text box
                val barcodeString = mainActivity.itemBarcode.getText().toString()

                if (barcodeString.matches("".toRegex()) || barcodeString.matches("[^0-9]".toRegex())) {
                    Toast.makeText(mainActivity, R.string.invalid_barcode, Toast.LENGTH_LONG).show()
                } else {
                    mainActivity.addItemDialogHandler(barcodeString, categorySpinner, subCategorySpinner, false)

                    mainActivity.updateItemData(categorySpinner, subCategorySpinner, barcodeString)

                    if ((mainActivity.data.get("item_name") as String).matches("".toRegex())) {
                        mainActivity.addItemConfirm.dismiss()
                        return@OnClickListener
                    }
                    try {
                        if (mainActivity.data.get("item_quantity")!!.toString().matches("".toRegex()) || mainActivity.data.get("item_quantity")!!.toString().matches("[^0-9]".toRegex())) {
                            mainActivity.addItemConfirm.dismiss()
                            return@OnClickListener
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Caught exception: $e")
                    }

                    firebaseHandler.updateItemTimestamp(barcodeString)

                    //Push data to database
                    if (mainActivity.nextItemIsNew) {
                        firebaseHandler.pushNewItem(barcodeString, mainActivity.data as Map<String, Any>)
                    } else {
                        firebaseHandler.updateItem(barcodeString, mainActivity.data as Map<String, Any>)
                    }

                    mainActivity.addItemConfirm.dismiss()
                }
            })

            val h = Handler()
            h.postDelayed({
                mBuilder.setView(mView)
                mainActivity.addItemConfirm = mBuilder.create()
                mainActivity.addItemConfirm.show()
            }, 100)
        })
    }


    fun initDrawer(user: FirebaseUser): Unit{
        val h1 = Handler()
        h1.postDelayed({
            mainActivity.navHeader = mainActivity.findViewById(R.id.nav_header)
            val userPhoto = mainActivity.navHeader.findViewById(R.id.drawer_user_image) as ImageView
            val userName = mainActivity.navHeader.findViewById(R.id.drawer_user_name) as TextView
            val userEmail = mainActivity.navHeader.findViewById(R.id.drawer_user_email) as TextView

            userName.text = user.displayName
            userEmail.text = user.email
            userPhoto.setImageURI(null)
            userPhoto.setImageURI(user.photoUrl)
        }, 300)
    }
}