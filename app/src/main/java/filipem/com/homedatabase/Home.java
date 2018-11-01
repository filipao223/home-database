package filipem.com.homedatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "My home";
    private Context context;

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
    private SwipeRefreshLayout mSwipeRefreshLayoutItems;

    private TextView noItemsText;

    private ItemsCardsAdapter cardsAdapter;

    private Home thisHome;

    private List<Item> itemList = new ArrayList<>();
    List<String> categories;
    ArrayAdapter<String> dataAdapter;
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
        }, 150);


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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        /*Gets camera permission*/
        ActivityCompat.requestPermissions(Home.this,
                new String[]{Manifest.permission.CAMERA},
                1);

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
        if ( recyclerViewItems != null){
            recyclerViewItems.setHasFixedSize(true); //RecyclerView terá sempre o mesmo tamanho, performance improvement

            LinearLayoutManager llm = new LinearLayoutManager(context); //Manager que gere como os cartoes aparecem na view
            recyclerViewItems.setLayoutManager(llm);

            /*Initial refresh*/
            if(isNetworkConnected()){
                Toast.makeText(this, R.string.get_data_server, Toast.LENGTH_LONG).show();
                mSwipeRefreshLayoutItems.setRefreshing(true);
                db.collection("users").document(user.getUid())
                        .collection("items").get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if(task.isSuccessful()){
                                    if (!task.getResult().isEmpty()){
                                        for(QueryDocumentSnapshot documentSnapshot: task.getResult()){
                                            if (documentSnapshot != null){
                                                if (documentSnapshot.getId().matches("testitem")) continue;
                                                Log.v(TAG, "Got item, barcode => " + (String)documentSnapshot.getId());
                                                Item item = new Item("", (String)documentSnapshot.get("item_name"), (long)documentSnapshot.get("item_quantity")
                                                        ,documentSnapshot.getId(), (String)documentSnapshot.get("item_category"), (String)documentSnapshot.get("item_subcategory"));
                                                itemList.add(item);
                                            }
                                        }

                                        noItemsText.setVisibility(View.GONE);

                                        cardsAdapter = new ItemsCardsAdapter(itemList, thisHome, imagesRef);
                                        recyclerViewItems.setAdapter(cardsAdapter);
                                    }
                                    else{
                                        Toast.makeText(thisHome, "Error retrieving categories from server", Toast.LENGTH_SHORT).show();
                                    }
                                    mSwipeRefreshLayoutItems.setRefreshing(false);
                                }
                            }
                        });
            }
            else{
                //Placeholder item, without adapter, swipe to refresh doesnt work
                itemList.add(new Item("", "empty", 0, "919234", "Food", "Pasta"));
                cardsAdapter = new ItemsCardsAdapter(itemList, thisHome, imagesRef);
                recyclerViewItems.setAdapter(cardsAdapter);
                Toast.makeText(this, R.string.no_network, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error, no network in Home");
            }

            //------Set add by hand---------
            addByHand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

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

                    categories = new ArrayList<>();
                    dataAdapter = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, categories);

                    /*Get categories from server*/
                    db.collection("categories").get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (queryDocumentSnapshots.isEmpty()){
                                        Log.e(TAG, "No categories found in database");
                                        Toast.makeText(thisHome, R.string.no_categories_db, Toast.LENGTH_LONG).show();
                                    }
                                    else{
                                        for(DocumentSnapshot documents: queryDocumentSnapshots.getDocuments()){
                                            if (documents.getId().matches("testcategory")) continue;
                                            Object tryCategoryLanguage = documents.get("name_"+language);
                                            if (tryCategoryLanguage != null){
                                                Log.d(TAG, "Found localized category (" + language + "_" + tryCategoryLanguage + ")");
                                                categories.add((String)tryCategoryLanguage);
                                            }
                                            else{
                                                Log.d(TAG, "Did not found localized category (" + language + "_" + tryCategoryLanguage + ")");
                                                categories.add(documents.getId());
                                            }
                                        }

                                        dataAdapter.notifyDataSetChanged();
                                    }
                                }
                            });

                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(dataAdapter);
                    categorySpinner.setSelection(0);

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
                                Barcode barcode = new Barcode();
                                barcode.rawValue = barcodeString;

                                addItemDialogHandler(barcode, categorySpinner, false);
                            }
                        }
                    });

                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Get barcode from text box
                            String barcodeString = itemBarcode.getText().toString();

                            if (barcodeString.matches("") || barcodeString.matches("[^0-9]")){
                                Toast.makeText(thisHome, R.string.invalid_barcode, Toast.LENGTH_LONG).show();
                            }
                            else{
                                Barcode barcode = new Barcode();
                                barcode.rawValue = barcodeString;
                                addItemDialogHandler(barcode, categorySpinner, false);

                                updateItemData(categorySpinner, barcode);

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

                                updateItemTimestamp(barcode);

                                //Push data to database
                                if (nextItemIsNew){
                                    userDocument.collection("items").document(barcode.rawValue).set(thisHome.data)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                }
                                else{
                                    userDocument.collection("items").document(barcode.rawValue).update(thisHome.data)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                                }
                                            });
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
            if (itemList != null){
                itemList.clear();
                Log.i(TAG, "Cleared itemslist data");
            }
            else itemList = new ArrayList<>();

            //Fill with new data
            Log.i(TAG, "Filling with new data");



            Log.d(TAG, "itemCollectionExists");

            db.collection("users").document(user.getUid())
                    .collection("items").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                for(QueryDocumentSnapshot documentSnapshot: task.getResult()){
                                    if (documentSnapshot.getId().matches("testitem")) continue;
                                    Log.v(TAG, "Got item, barcode => " + (String)documentSnapshot.getId());
                                    Item item = new Item("", (String)documentSnapshot.get("item_name"), (long)documentSnapshot.get("item_quantity")
                                            ,documentSnapshot.getId(), (String)documentSnapshot.get("item_category"), (String)documentSnapshot.get("item_subcategory"));
                                    itemList.add(item);
                                }

                                if (itemList.isEmpty()){
                                    noItemsText.setVisibility(View.VISIBLE);
                                }
                                else noItemsText.setVisibility(View.INVISIBLE);


                                Log.i(TAG, "New data added, example: " + (itemList.isEmpty()?"null":itemList.get(0).toString()));

                                //Create adapterPosts if null
                                if (cardsAdapter == null){
                                    cardsAdapter = new ItemsCardsAdapter(itemList, thisHome, imagesRef);
                                    Log.i(TAG, "New adapterPosts created");
                                }
                                else{
                                    //Notify adapterPosts of the change
                                    cardsAdapter = new ItemsCardsAdapter(itemList, thisHome, imagesRef);
                                    //adapterPosts.notifyDataSetChanged(); <-- Não consigo por a funcionar com este metodo
                                    recyclerViewItems.setAdapter(cardsAdapter);
                                }

                                mSwipeRefreshLayoutItems.setRefreshing(false);
                            }
                        }
                    });
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
                            Toast.makeText(context, R.string.af_error, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Home.this, BarcodeCaptureActivity.class);
                            Home.this.startActivityForResult(intent, RC_BARCODE_SCAN);
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

            // other 'case' lines to check for other
            // permissions this app might request
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
                final Barcode barcode = (Barcode) barcodeData.get("Barcode");
                if (barcode != null ){
                    Log.i(TAG, "Got barcode: " + barcode.rawValue);
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

                    categories = new ArrayList<>();
                    dataAdapter = new ArrayAdapter<String>(thisHome, android.R.layout.simple_spinner_item, categories);

                    /*Get categories from server*/
                    db.collection("categories").get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (queryDocumentSnapshots.isEmpty()){
                                        Log.e(TAG, "No categories found in database");
                                        Toast.makeText(thisHome, R.string.no_categories_db, Toast.LENGTH_LONG).show();
                                    }
                                    else{
                                        for(DocumentSnapshot documents: queryDocumentSnapshots.getDocuments()){
                                            if (documents.getId().matches("testcategory")) continue;
                                            Object tryCategoryLanguage = documents.get("name_"+language);
                                            if (tryCategoryLanguage != null){
                                                Log.d(TAG, "Found localized category (" + language + "_" + tryCategoryLanguage + ")");
                                                categories.add((String)tryCategoryLanguage);
                                            }
                                            else{
                                                Log.d(TAG, "Did not found localized category (" + language + "_" + tryCategoryLanguage + ")");
                                                categories.add(documents.getId());
                                            }
                                        }

                                        dataAdapter.notifyDataSetChanged();
                                    }
                                }
                            });

                    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(dataAdapter);
                    categorySpinner.setSelection(0);

                    itemName = mView.findViewById(R.id.add_item_dialog_name);
                    itemQuantity = mView.findViewById(R.id.add_item_dialog_quantity);
                    itemBarcode = mView.findViewById(R.id.add_item_dialog_barcode); itemBarcode.setText(barcode.rawValue);
                    confirmButton = mView.findViewById(R.id.add_item_dialog_confirm);
                    cancelButton = mView.findViewById(R.id.add_item_dialog_cancel);

                    Log.d(TAG, "Entering addItemDialogHandler");
                    addItemDialogHandler(barcode, categorySpinner, false);

                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            addItemConfirm.dismiss();
                        }
                    });

                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            updateItemData(categorySpinner, barcode);

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

                            updateItemTimestamp(barcode);

                            //Push data to database
                            if (nextItemIsNew){
                                userDocument.collection("items").document(barcode.rawValue).set(thisHome.data)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                            else{
                                userDocument.collection("items").document(barcode.rawValue).update(thisHome.data)
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

    private void addItemDialogHandler(final Barcode barcode, final Spinner categorySpinner, final boolean byHand) {
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
                                    .document(barcode.rawValue)
                                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot.exists()){
                                        //---------Specific item already exists
                                        nextItemIsNew = false;
                                        Log.d(TAG, "Item with barcode => " +
                                                barcode.rawValue + " already exists");
                                        Toast.makeText(thisHome, R.string.add_item_already_exists_error, Toast.LENGTH_LONG).show();

                                        Log.d(TAG, "Entering updateEditTextFields");
                                        if (!byHand){
                                            updateEditTextFields(documentSnapshot, categorySpinner);
                                        }

                                        //updateItemData(categorySpinner, barcode);

                                        //updateItemTimestamp(barcode);
                                    }
                                    else {
                                        //---------Specific item doesnt exist yet
                                        nextItemIsNew = true;
                                        Log.d(TAG, "Item with barcode => " +
                                                barcode.rawValue + " doesn't yet exist");
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

    private void updateItemTimestamp(final Barcode barcode) {
        db.collection("users").document(user.getUid())
                .collection("items")
                .document(barcode.rawValue)
                .update("updated", System.currentTimeMillis() / 1000L)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "Timestamp of tem with barcode => " +
                        barcode.rawValue + " updated");
            }
            }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Failed to update timestamp of tem with barcode => " +
                        barcode.rawValue);
            }
        });
    }

    private void updateEditTextFields(DocumentSnapshot documentSnapshot, Spinner categorySpinner) {
        itemBarcode.setText(documentSnapshot.getId());
        itemName.setText((String)documentSnapshot.get("item_name"));
        itemQuantity.setText(String.valueOf(documentSnapshot.get("item_quantity")));
        int spinnerPos = dataAdapter.getPosition((String)documentSnapshot.get("item_category"));
        categorySpinner.setSelection(spinnerPos);
    }

    private void updateItemData(Spinner categorySpinner, Barcode barcode) {
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

        data.put("item_subcategory", "Outros");

        if (nextItemIsNew){
            data.put("timestamp", System.currentTimeMillis() / 1000L );
        }
        data.put("updated", System.currentTimeMillis() / 1000L );
    }

    protected void itemLongClick(View view){
        //Get card data
        String barcode = ((TextView)view.findViewById(R.id.home_card_item_barcode)).getText().toString();
        Toast.makeText(context, "Long pressed item with barcode => " + barcode, Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }
}
