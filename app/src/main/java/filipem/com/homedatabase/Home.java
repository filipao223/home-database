package filipem.com.homedatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private RecyclerView recyclerViewItems;
    private SwipeRefreshLayout mSwipeRefreshLayoutItems;

    private TextView noItemsText;

    private ItemsCardsAdapter cardsAdapter;

    private Home thisHome;

    private List<Item> itemList;

    private boolean itemCollectionExists = false;
    private boolean itemInCollectionExists = false;

    private AlertDialog addItemConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        noItemsText = findViewById(R.id.home_no_items);

        //Get user
        Bundle extra = getIntent().getExtras();
        if ( extra == null ){
            Log.e(TAG, "No user retrieved");
        }
        else{
            user = (FirebaseUser) extra.get("User");
        }

        Log.i(TAG, "Got user, name is : " + user.getDisplayName());
        Log.i(TAG, "Got user, email is : " + user.getEmail());
        Log.i(TAG, "Got user, uuid is : " + user.getUid());

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

        context = this;
        thisHome = this;

        menuButtons = findViewById(R.id.menu_multiple_actions);
        addByHand = findViewById(R.id.addByHand);
        addBarcode = findViewById(R.id.addBarcode);

        addByHand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "Clicked addByHand", Toast.LENGTH_LONG).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
                    mSwipeRefreshLayoutItems.setRefreshing(true); //Refresh icon gets toggled


                    try{
                        Log.i(TAG, "Refreshing data");

                        refreshHomeItems();
                        mSwipeRefreshLayoutItems.setRefreshing(false); //finished
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

            refreshHomeItems(); //Por agora objectos FeedPost são criados à mao e colocados na lista feedPost

            if(!itemList.isEmpty()){
                noItemsText.setVisibility(View.GONE);
            }

            cardsAdapter = new ItemsCardsAdapter(itemList, thisHome); //RecyclerView adapterPosts
            recyclerViewItems.setAdapter(cardsAdapter);
        }
    }

    private void refreshHomeItems() {
        Log.i(TAG, "refreshHomeItems was called");

        //Clear old data
        if (itemList != null){
            itemList.clear();
            Log.i(TAG, "Cleared itemslist data");
        }
        else itemList = new ArrayList<>();

        //Fill with new data
        Log.i(TAG, "Filling with new data");


        checkForItemCollection();
        if(itemCollectionExists){

            userDocument.collection("items").get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                for(QueryDocumentSnapshot documentSnapshot: task.getResult()){
                                    if (documentSnapshot.getId().matches("testitem")) continue;
                                    Item item = new Item("", (String)documentSnapshot.get("item_name"), (long)documentSnapshot.get("item_quantity"));
                                    itemList.add(item);
                                }
                            }
                        }
                    });

        }

        if (itemList.isEmpty()){
            noItemsText.setVisibility(View.VISIBLE);
        }
        else noItemsText.setVisibility(View.GONE);


        Log.i(TAG, "New data added, example: " + (itemList.isEmpty()?"null":itemList.get(0).toString()));

        //Create adapterPosts if null
        if (cardsAdapter == null){
            cardsAdapter = new ItemsCardsAdapter(itemList, thisHome);
            Log.i(TAG, "New adapterPosts created");
        }
        else{
            //Notify adapterPosts of the change
            cardsAdapter = new ItemsCardsAdapter(itemList, thisHome);
            //adapterPosts.notifyDataSetChanged(); <-- Não consigo por a funcionar com este metodo
            recyclerViewItems.setAdapter(cardsAdapter);
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
                            Toast.makeText(context, "Clicked addBarcode", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Home.this, BarcodeCaptureActivity.class);
                            Home.this.startActivityForResult(intent, RC_BARCODE_SCAN);
                        }
                    });

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(Home.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

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

                    final EditText itemName = mView.findViewById(R.id.add_item_dialog_name); itemName.setHint(R.string.item_name_hint);
                    final EditText itemQuantity = mView.findViewById(R.id.add_item_dialog_quantity); itemQuantity.setHint(R.string.item_quantity_hint);
                    final Button confirmButton = mView.findViewById(R.id.add_item_dialog_confirm);
                    final Button cancelButton = mView.findViewById(R.id.add_item_dialog_cancel);

                    confirmButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            /*------Check if item collection already exists------*/
                            checkForItemCollection();
                            if (itemCollectionExists){
                                /*-------Check if current item already exists*/
                                checkIfItemAlreadyExists(barcode.rawValue);
                                if (itemInCollectionExists){

                                    /*First get current values*/
                                    userDocument.collection("items").document(barcode.rawValue).get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    try{
                                                        tempItem.setItem_name((String) documentSnapshot.get("item_name"));
                                                        tempItem.setItem_quantity((int) documentSnapshot.get("item_quantity"));
                                                    } catch(ClassCastException e){
                                                        Log.e(TAG, "Wrong casting while getting data: " + e);
                                                    } finally {
                                                        Log.e(TAG, "Other error while getting data");
                                                    }

                                                    Log.d(TAG, "Got item name from db: " + tempItem.getItem_name());
                                                    Log.d(TAG, "Got item quantity from db: " + tempItem.getItem_quantity());
                                                }
                                            });

                                }
                                else{
                                /*---------Item doesn't exist yet-------*/
                                    Map<String, Object> data = new HashMap<>();
                                    String text = itemName.getText().toString();
                                    Log.d(TAG, "itemName: " + text);
                                    if (text.matches("")){
                                        Toast.makeText(context, "Invalid item name", Toast.LENGTH_LONG).show();
                                        addItemConfirm.dismiss();
                                        return;
                                    }
                                    data.put("item_name", text);
                                    text = itemQuantity.getText().toString();
                                    Log.d(TAG, "itemquantity: " + text);
                                    if (text.matches("") || text.matches("[^0-9]")){
                                        Toast.makeText(context, "Invalid item quantity", Toast.LENGTH_LONG).show();
                                        addItemConfirm.dismiss();
                                        return;
                                    }
                                    data.put("item_quantity", Integer.parseInt(text));
                                    userDocument.collection("items").document(barcode.rawValue).set(data)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    Toast.makeText(context, "Item added", Toast.LENGTH_LONG).show();
                                                }
                                            });
                                    addItemConfirm.dismiss();
                                }
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
                else{
                    Log.e(TAG, "Returned null barcode");
                }
            }
        }
    }

    private void checkIfItemAlreadyExists(final String barcode) {
        userDocument = db.collection("users").document(user.getUid());
        userDocument.collection("items").document(barcode).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Log.d(TAG, "Item " + barcode + " in collection \"items\" exists");
                            itemInCollectionExists = true;
                        }
                        else{
                            Log.d(TAG, "Item " + barcode + " in collection \"items\" does not exist");
                            itemInCollectionExists = false;
                        }
                    }
                });
    }

    private void checkForItemCollection() {
        Log.d(TAG, "userDocument is : " + userDocument.getId());
        db.collection("users").document(user.getUid()).collection("items")
                .document("testitem")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()){
                            Log.d(TAG, "Collection \"items\" exists");
                            itemCollectionExists = true;
                        }
                        else{
                            Log.d(TAG, "Collection \"items\" does not exist");
                            itemCollectionExists = false;
                        }
                    }
                });

    }
}
