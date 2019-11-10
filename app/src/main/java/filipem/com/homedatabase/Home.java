package filipem.com.homedatabase;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import android.util.Log;
import android.view.View;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import filipem.com.homedatabase.Barcode.Scanner;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "My home";
    private Context context;
    //Testing gpg key
    private static int RC_BARCODE_SCAN = 1;

    private com.getbase.floatingactionbutton.FloatingActionsMenu menuButtons;
    private com.getbase.floatingactionbutton.FloatingActionButton addByHand;
    private com.getbase.floatingactionbutton.FloatingActionButton addBarcode;

    private Item tempItem;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser user;
    private DocumentReference userDocument;
    private DocumentReference newUserDocument;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();
    private StorageReference imagesRef = storageRef.child("itemImages");

    protected RecyclerView recyclerViewItems;
    SwipeRefreshLayout mSwipeRefreshLayoutItems;

    private SearchView searchView;

    TextView noItemsText;

    ItemsCardsAdapter cardsAdapter;

    private Home thisHome;

    ArrayList<Item> itemList = new ArrayList<>();
    private List<Item> currentlyEditing = new ArrayList<>();
    List<String> categories;
    List<String> subcategories;
    ArrayAdapter<String> dataAdapterMain;
    ArrayAdapter<String> dataAdapterSub;
    Map<String, Object> data;

    private boolean itemCollectionExists = false;
    private boolean itemInCollectionExists = false;
    private boolean nextItemIsNew = false;

    private AlertDialog addItemConfirm;
    EditText itemName;
    EditText itemQuantity;
    EditText itemBarcode;
    Button confirmButton;
    Button cancelButton;
    Button checkButton;

    private NavigationView mNavigationView;
    private LinearLayout navHeader;
    private LinearLayoutManager llm;

    private ZXingScannerView scannerView;

    RVHandler rvHandler;
    FirebaseHandler firebaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        scannerView = new ZXingScannerView(this);

        /*-----Initialize drawer image, name and email-----*/
        final Handler h1 = new Handler();
        h1.postDelayed(new Runnable() {
            @Override
            public void run() {
                navHeader = findViewById(R.id.nav_header);
                ImageView userPhoto = navHeader.findViewById(R.id.drawer_user_image);
                TextView userName = navHeader.findViewById(R.id.drawer_user_name);
                TextView userEmail = navHeader.findViewById(R.id.drawer_user_email);

                userName.setText(user.getDisplayName());
                userEmail.setText(user.getEmail());
                userPhoto.setImageURI(null);
                userPhoto.setImageURI(user.getPhotoUrl());
            }
        }, 300);


        /*-----If no items exist, this text view appears----*/
        noItemsText = findViewById(R.id.home_no_items);

        //Get storage
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        imagesRef = storageRef.child("itemImages");


        //Get user
        Bundle extra = getIntent().getExtras();
        if ( extra == null ){
            Log.e(TAG, "No user retrieved");
        }
        else{
            user = (FirebaseUser) extra.get("User");
        }

        //Check if user exists in database
        checkIfUserExists();

        Log.i(TAG, "Got user, name is : " + user.getDisplayName());
        Log.i(TAG, "Got user, email is : " + user.getEmail());
        Log.i(TAG, "Got user, uuid is : " + user.getUid());

        context = this;
        thisHome = this;

        menuButtons = findViewById(R.id.menu_multiple_actions);
        addByHand = findViewById(R.id.addByHand);
        addBarcode = findViewById(R.id.addBarcode);

        addByHand.setColorNormalResId(R.color.lightWhite);
        addBarcode.setColorNormalResId(R.color.lightWhite);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        /*Gets camera permission*/
        ActivityCompat.requestPermissions(Home.this,
                new String[]{Manifest.permission.CAMERA},
                1);

        /*Init the swipe to refresh layout elements*/
        mSwipeRefreshLayoutItems = findViewById(R.id.home_swipe_refresh);
        Log.i(TAG, "mSwipeRefreshLayoutPosts is " + (mSwipeRefreshLayoutItems ==null?"null":"not null"));
        if (mSwipeRefreshLayoutItems != null){
            mSwipeRefreshLayoutItems.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Log.i(TAG, "Called onRefresh");

                    try{
                        Log.i(TAG, "Refreshing data");

                        mSwipeRefreshLayoutItems.setRefreshing(true); //Refresh icon gets toggled
                        refreshHomeItems(mSwipeRefreshLayoutItems);
                        //mSwipeRefreshLayoutItems.setRefreshing(false); //finished
                    }catch(NullPointerException e){
                        Log.e(TAG, "Error onRefresh: " + e);
                    }
                }
            });
            mSwipeRefreshLayoutItems.setColorSchemeResources(android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light);
        }



        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        recyclerViewItems.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && menuButtons.getVisibility() == View.VISIBLE) {
                    menuButtons.setVisibility(View.GONE);
                } else if (dy < 0 && menuButtons.getVisibility() != View.VISIBLE) {
                    menuButtons.setVisibility(View.VISIBLE);
                }
            }
        });
        if ( recyclerViewItems != null){
            recyclerViewItems.setHasFixedSize(true); //RecyclerView terá sempre o mesmo tamanho, performance improvement

            llm = new LinearLayoutManager(context); //Manager que gere como os cartoes aparecem na view
            recyclerViewItems.setLayoutManager(llm);

            /*Initial refresh if first time opening the activity*/
            if(isNetworkConnected()){
                Toast.makeText(this, R.string.get_data_server, Toast.LENGTH_LONG).show();
                mSwipeRefreshLayoutItems.setRefreshing(true);

                /*Initialize handlers for getting the data from firestore and for displaying it in a recycler view*/
                rvHandler = new RVHandler(recyclerViewItems, thisHome, imagesRef);
                firebaseHandler = new FirebaseHandler(db, user, thisHome, rvHandler);
                /*Get the items from the firestore*/
                firebaseHandler.getItems();
            }
            else{
                //Placeholder item, without adapter, swipe to refresh doesnt work
                itemList.add(new Item("", "empty", 0, "919234", "Food", "Pasta"));
                cardsAdapter = new ItemsCardsAdapter(itemList, thisHome, imagesRef);
                recyclerViewItems.setAdapter(cardsAdapter);
                recyclerViewItems.setVisibility(View.GONE);
                Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error, no network in Home");
            }

            //------Set add by hand---------
            addByHand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menuButtons.collapse();
                    final AlertDialog.Builder mBuilder = new AlertDialog.Builder(thisHome);
                    final View mView = getLayoutInflater().inflate(R.layout.item_add_dialog_hand, null);

                    /*Default locale*/
                    Locale currentLocale = Locale.getDefault();
                    final String language = currentLocale.getLanguage();

                    /*Get main category spinner*/
                    final Spinner categorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_category);
                    categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            ((TextView) view).setTextColor(Color.BLACK);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    /*Get sub category spinner*/
                    final Spinner subCategorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_subcategory);
                    subCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    categories = new ArrayList<>();
                    subcategories = new ArrayList<>();
                    dataAdapterMain = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, categories);
                    dataAdapterSub = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, subcategories);

                    /*Get categories from server*/
                    firebaseHandler.getCategories(language);

                    /*Get subcategories from server*/
                    firebaseHandler.getSubCategories(language);

                    dataAdapterMain.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    dataAdapterSub.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(dataAdapterMain);
                    categorySpinner.setSelection(0);
                    subCategorySpinner.setAdapter(dataAdapterSub);
                    subCategorySpinner.setSelection(0);

                    itemName = mView.findViewById(R.id.add_item_dialog_name);
                    itemQuantity = mView.findViewById(R.id.add_item_dialog_quantity);
                    itemBarcode = mView.findViewById(R.id.add_item_dialog_barcode);
                    confirmButton = mView.findViewById(R.id.add_item_dialog_confirm);
                    cancelButton = mView.findViewById(R.id.add_item_dialog_cancel);
                    checkButton = mView.findViewById(R.id.add_item_dialog_check_barcode);

                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            addItemConfirm.dismiss();
                        }
                    });

                    checkButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String barcodeString = itemBarcode.getText().toString();

                            if (barcodeString.matches("") || barcodeString.matches("[^0-9]")){
                                Toast.makeText(thisHome, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
                            }
                            else{
                                addItemDialogHandler(barcodeString, categorySpinner, subCategorySpinner, false);
                            }
                        }
                    });

                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Get barcode from text box
                            final String barcodeString = itemBarcode.getText().toString();

                            if (barcodeString.matches("") || barcodeString.matches("[^0-9]")){
                                Toast.makeText(thisHome, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
                            }
                            else{
                                addItemDialogHandler(barcodeString, categorySpinner, subCategorySpinner, false);

                                updateItemData(categorySpinner, subCategorySpinner, barcodeString);

                                if (((String)thisHome.data.get("item_name")).matches("")){
                                    addItemConfirm.dismiss();
                                    return;
                                }
                                try{
                                    if (thisHome.data.get("item_quantity").toString().matches("") ||
                                            thisHome.data.get("item_quantity").toString().matches("[^0-9]")){
                                        addItemConfirm.dismiss();
                                        return;
                                    }
                                }catch (Exception e){
                                    Log.e(TAG, "Caught exception: " + e);
                                }

                                firebaseHandler.updateItemTimestamp(barcodeString);

                                //Push data to database
                                if (nextItemIsNew){
                                    firebaseHandler.pushNewItem(barcodeString, data);
                                }
                                else{
                                    firebaseHandler.updateItem(barcodeString, data);
                                }

                                addItemConfirm.dismiss();
                            }
                        }
                    });

                    final Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBuilder.setView(mView);
                            addItemConfirm = mBuilder.create();
                            addItemConfirm.show();
                        }
                    }, 100);
                }
            });
        }
    }






    @Override
    protected void onPause() {
        super.onPause();

        //Save refresh status to boolean
        SharedPreferences saved = getPreferences(0);
        SharedPreferences.Editor editor = saved.edit();

    }






    @Override
    protected void onResume() {
        super.onResume();

        //Check if there are saved values in shared preferences
        SharedPreferences saved = getPreferences(0);
        if (saved.contains("items")){

        }
    }






    private void checkIfUserExists() {
        if (isNetworkConnected()){
            userDocument = db.collection("users").document(user.getUid());
            userDocument.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    DocumentSnapshot document = task.getResult();
                    if ( document.exists() ){
                        Log.i(TAG, "User already exists!");

                    }
                    else{
                        Log.i(TAG, "User not found!");
                        /*Create new document then*/
                        Map<String, Object> nestedData = new HashMap<>();
                        nestedData.put("exists", true);
                        try{
                            db.collection("users").document(user.getUid()).set(nestedData);
                        } catch(Exception e){
                            Log.e(TAG, "Exception caught while trying to add document: "  +e);
                        }
                    }
                }
            });
        }
        else{
            Log.e(TAG, "Error, no network in checkIfUserExists");
        }
    }





    private void refreshHomeItems(SwipeRefreshLayout swipeLayout) {
        Log.i(TAG, "refreshHomeItems was called");

        //Check for internet connection
        if(isNetworkConnected()){
            //Clear old data
            recyclerViewItems.setVisibility(View.VISIBLE);
            if (itemList != null){
                itemList.clear();
                Log.i(TAG, "Cleared itemslist data");
            }
            else itemList = new ArrayList<>();

            //Fill with new data
            Log.i(TAG, "Filling with new data");
            Log.d(TAG, "itemCollectionExists");

            firebaseHandler.getItems();
        }
        else{
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show();
            Log.e(TAG, "No internet connection in refreshHomeItems");
            mSwipeRefreshLayoutItems.setRefreshing(false);
        }
    }





    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the itemCollectionExists arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Set onclick of button to barcode scanner
                    addBarcode.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Toast.makeText(context, R.string.af_error, Toast.LENGTH_LONG).show();
                            menuButtons.collapse();
                            Intent intent = new Intent(Home.this, Scanner.class);
                            startActivityForResult(intent, RC_BARCODE_SCAN);
                            //Home.this.startActivityForResult(intent, RC_BARCODE_SCAN);
                        }
                    });

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(Home.this, R.string.refused_camera, Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }





    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "Got query: " + query);
                // filter recycler view when query submitted
                cardsAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                Log.d(TAG, "Got query updated: " + query);
                // filter recycler view when text is changed
                cardsAdapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }







    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }






    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_signout) {
            //Sign out from the app and go back to login
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            finish();
            return true;
        } else if(id == R.id.nav_exit){
            //Exit app
            finish();
            return true;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }





    //After the barcode scan activity ends, it starts this method
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == RC_BARCODE_SCAN){
            if (resultCode == RESULT_OK ){
                /*Get barcode*/
                Bundle barcodeData = data.getExtras();
                final String barcode = (String) barcodeData.get("Barcode");
                if (barcode != null ){
                    Log.i(TAG, "Got barcode: " + barcode);
                    final AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
                    final View mView = getLayoutInflater().inflate(R.layout.item_add_dialog, null);

                    /*Default locale*/
                    Locale currentLocale = Locale.getDefault();
                    final String language = currentLocale.getLanguage();

                    /*Get main category spinner*/
                    final Spinner categorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_category);
                    categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            ((TextView) view).setTextColor(Color.BLACK);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    /*Get sub categpry spinner*/
                    final Spinner subCategorySpinner = mView.findViewById(R.id.add_item_dialog_spinner_subcategory);
                    subCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    categories = new ArrayList<>();
                    subcategories = new ArrayList<>();
                    dataAdapterMain = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, categories);
                    dataAdapterSub = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, subcategories);

                    /*Get categories from server*/
                    firebaseHandler.getCategories(language);

                    /*Get subcategories from server*/
                    firebaseHandler.getSubCategories(language);

                    dataAdapterMain.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    dataAdapterSub.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(dataAdapterMain);
                    categorySpinner.setSelection(0);
                    subCategorySpinner.setAdapter(dataAdapterSub);
                    subCategorySpinner.setSelection(0);

                    itemName = mView.findViewById(R.id.add_item_dialog_name);
                    itemQuantity = mView.findViewById(R.id.add_item_dialog_quantity);
                    itemBarcode = mView.findViewById(R.id.add_item_dialog_barcode); itemBarcode.setText(barcode);
                    confirmButton = mView.findViewById(R.id.add_item_dialog_confirm);
                    cancelButton = mView.findViewById(R.id.add_item_dialog_cancel);

                    Log.d(TAG, "Entering addItemDialogHandler");
                    addItemDialogHandler(barcode, categorySpinner, subCategorySpinner, false);

                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            addItemConfirm.dismiss();
                        }
                    });

                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            updateItemData(categorySpinner, subCategorySpinner, barcode);

                            if (((String)thisHome.data.get("item_name")).matches("")){
                                addItemConfirm.dismiss();
                                return;
                            }
                            try{
                                if (((String)thisHome.data.get("item_quantity")).matches("") ||
                                        ((String)thisHome.data.get("item_quantity")).matches("[^0-9]")){
                                    addItemConfirm.dismiss();
                                    return;
                                }
                            }catch (Exception e){
                                Log.e(TAG, "Caught exception: " + e);
                            }

                            firebaseHandler.updateItemTimestamp(barcode);

                            //Push data to database
                            if (nextItemIsNew){
                                userDocument.collection("items").document(barcode).set(thisHome.data)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                                Item item = new Item("", (String)thisHome.data.get("item_name"), (int)thisHome.data.get("item_quantity")
                                                        ,barcode, (String)thisHome.data.get("item_category"), (String)thisHome.data.get("item_subcategory"));
                                                if (cardsAdapter.items.isEmpty()){
                                                    cardsAdapter.items.add(item);
                                                    cardsAdapter.notifyItemInserted(cardsAdapter.items.size()-1);
                                                }
                                                else{
                                                    cardsAdapter.items.add(0, item);
                                                    cardsAdapter.notifyItemInserted(0);
                                                }
                                            }
                                        });
                            }
                            else{
                                userDocument.collection("items").document(barcode).update(thisHome.data)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                            addItemConfirm.dismiss();
                        }
                    });

                    final Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBuilder.setView(mView);
                            addItemConfirm = mBuilder.create();
                            addItemConfirm.show();
                        }
                    }, 100);
                }
                else{
                    Log.e(TAG, "Returned null barcode");
                }
            }
        }
    }






    private void addItemDialogHandler(final String barcode, final Spinner categorySpinner, final Spinner subCategorySpinner, final boolean byHand) {
        /*---------NEW CODE-------------*/
        if (isNetworkConnected()){
            db.collection("users").document(user.getUid())
                    .collection("items")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            //------------Items exists
                            Log.d(TAG, "Items collection exists for user " + user.getUid());
                            db.collection("users").document(user.getUid())
                                    .collection("items")
                                    .document(barcode)
                                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot.exists()){
                                        //---------Specific item already exists
                                        nextItemIsNew = false;
                                        Log.d(TAG, "Item with barcode => " +
                                                barcode + " already exists");
                                        Toast.makeText(thisHome, R.string.add_item_already_exists_error, Toast.LENGTH_LONG).show();

                                        Log.d(TAG, "Entering updateEditTextFields");
                                        if (!byHand){
                                            updateEditTextFields(documentSnapshot, categorySpinner, subCategorySpinner);
                                        }

                                        //updateItemData(categorySpinner, barcode);
                                        //firebaseHandler.updateItem();

                                        //updateItemTimestamp(barcode);
                                    }
                                    else {
                                        //---------Specific item doesnt exist yet
                                        nextItemIsNew = true;
                                        Log.d(TAG, "Item with barcode => " +
                                                barcode + " doesn't yet exist");
                                        //updateItemData(categorySpinner, barcode);

                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Error trying to retrieve item information");
                                    Toast.makeText(thisHome, "Error contacting server", Toast.LENGTH_LONG).show();
                                }
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Error retrieving collection \"items\"");
                }
            });
        }
        else{
            /*-------NO NETWORK CONNECTION-------*/
            Log.e(TAG, "Tried to add item without network connection");
            Toast.makeText(thisHome, R.string.no_network, Toast.LENGTH_LONG).show();
        }
    }







    private void updateEditTextFields(DocumentSnapshot documentSnapshot, Spinner categorySpinner, Spinner subCategorySpinner) {
        itemBarcode.setText(documentSnapshot.getId());
        itemName.setText((String)documentSnapshot.get("item_name"));
        itemQuantity.setText(String.valueOf(documentSnapshot.get("item_quantity")));
        int spinnerPosMain = dataAdapterMain.getPosition((String)documentSnapshot.get("item_category"));
        int spinnerPosSub = dataAdapterSub.getPosition((String)documentSnapshot.get("item_subcategory"));
        categorySpinner.setSelection(spinnerPosMain);
        subCategorySpinner.setSelection(spinnerPosSub);
    }






    private void updateItemData(Spinner categorySpinner, Spinner subCategorySpinner, String barcode) {
        data = new HashMap<>();

        data.put("item_quantity", "");

        String text = itemName.getText().toString();
        Log.d(TAG, "itemName: " + text);
        if (text.matches("")){
            Log.e(TAG, "Invalid item name");
            Toast.makeText(context, R.string.add_item_invalid_name, Toast.LENGTH_LONG).show();
            //addItemConfirm.dismiss();
            return;
        }
        data.put("item_name", text);
        text = itemQuantity.getText().toString();
        Log.d(TAG, "itemquantity: " + text);
        if (text.matches("") || text.matches("[^0-9]")){
            Log.e(TAG, "Invalid item quantity");
            Toast.makeText(context, R.string.add_item_invalid_number, Toast.LENGTH_LONG).show();
            //addItemConfirm.dismiss();
            return;
        }
        data.put("item_quantity", Integer.parseInt(text));
        text = (String)categorySpinner.getSelectedItem();
        Log.d(TAG, "Select item is: " + text);
        data.put("item_category", text);

        text = (String)subCategorySpinner.getSelectedItem();
        Log.d(TAG, "Select item is: " + text);
        data.put("item_subcategory", text);

        if (nextItemIsNew){
            data.put("timestamp", System.currentTimeMillis() / 1000L );
        }
        data.put("updated", System.currentTimeMillis() / 1000L );
    }





    protected void itemLongClick(View view, final ItemsCardsAdapter.CardViewHolder holder){
        //Create alert dialog
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(holder.itemName.getText().toString())
                .setMessage(R.string.item_delete_confirmation)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        if (isNetworkConnected()){
                            db.collection("users").document(user.getUid())
                                    .collection("items").document(cardsAdapter.items.get(holder.getAdapterPosition()).getItemBarcode())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "Item deleted");
                                            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                                            cardsAdapter.items.remove(holder.getAdapterPosition());
                                            cardsAdapter.notifyItemRemoved(holder.getAdapterPosition());
                                            cardsAdapter.notifyItemRangeChanged(holder.getAdapterPosition(), cardsAdapter.items.size());
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "Failed item deletion");
                                        }
                                    });
                        }
                        else{
                            Log.e(TAG, "Tried to delete without network");
                            Toast.makeText(context, R.string.no_network, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .show();
    }






    protected void itemClick(View view, final ItemsCardsAdapter.CardViewHolder holder){
        switch (view.getId()){
            case R.id.home_card_item_add:
                //An add enables the 'done' icon
                holder.pendingChanges = true;
                ((ImageView)holder.itemView.findViewById(R.id.home_card_item_done)).setBackgroundResource(R.drawable.baseline_check_black_18dp);

                //Add one item to quantity in textviews
                //Current quantity
                TextView textView = holder.itemView.findViewById(R.id.home_card_item_quantity);
                int current = Integer.parseInt(textView.getText().toString());
                current+=1;
                ((TextView)holder.itemView.findViewById(R.id.home_card_item_quantity)).setText(String.valueOf(current));

                //Add one item to actual class
                Item item = cardsAdapter.items.get(holder.getAdapterPosition());
                item.setItem_quantity(item.getItem_quantity()+1);

                //Add class to change to list
                currentlyEditing.add(item);
                break;

            case R.id.home_card_item_remove:
                //Remove one item to quantity in textviews (if not already 0)
                textView = holder.itemView.findViewById(R.id.home_card_item_quantity);
                current = Integer.parseInt(textView.getText().toString());
                if (current > 0){
                    //A remove enables the done icon
                    holder.pendingChanges = true;
                    ((ImageView)holder.itemView.findViewById(R.id.home_card_item_done)).setBackgroundResource(R.drawable.baseline_check_black_18dp);
                    current -=1;
                    ((TextView)holder.itemView.findViewById(R.id.home_card_item_quantity)).setText(String.valueOf(current));

                    //Remove one item from actual class
                    item = cardsAdapter.items.get(holder.getAdapterPosition());
                    item.setItem_quantity(item.getItem_quantity()-1);
                }
                break;

            case R.id.home_card_item_done:
                //Check if there are changes to make in this card
                if (holder.pendingChanges){
                    //Make the changes
                    if (isNetworkConnected()){
                        Map<String, Object> data = new HashMap<>();
                        data.put("item_quantity", cardsAdapter.items.get(holder.getAdapterPosition()).getItem_quantity());
                        db.collection("users").document(user.getUid())
                                .collection("items").document(cardsAdapter.items.get(holder.getAdapterPosition()).getItemBarcode())
                                .update(data).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, R.string.item_update, Toast.LENGTH_LONG).show();
                                //Set boolean and background to default
                                holder.pendingChanges = false;
                                ((ImageView)holder.itemView.findViewById(R.id.home_card_item_done)).setBackgroundResource(R.drawable.baseline_check_black_18dp_opaque);
                            }
                        });
                    }
                    else{
                        Log.e(TAG, "Tried to update item without internet connection");
                        Toast.makeText(context, R.string.no_network, Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }




    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }
}
