package filipem.com.homedatabase;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "My home";
    private Context context;

    private com.getbase.floatingactionbutton.FloatingActionsMenu menuButtons;
    private com.getbase.floatingactionbutton.FloatingActionButton addByHand;
    private com.getbase.floatingactionbutton.FloatingActionButton addBarcode;

    private RecyclerView recyclerViewItems;

    private ItemsCardsAdapter cardsAdapter;

    private Home thisHome;

    private List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        recyclerViewItems = findViewById(R.id.recyclerViewItems);
        if ( recyclerViewItems != null){
            recyclerViewItems.setHasFixedSize(true); //RecyclerView terá sempre o mesmo tamanho, performance improvement

            LinearLayoutManager llm = new LinearLayoutManager(context); //Manager que gere como os cartoes aparecem na view
            recyclerViewItems.setLayoutManager(llm);

            refreshHomeItems(); //Por agora objectos FeedPost são criados à mao e colocados na lista feedPost

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


        itemList.add(new Item("", "Item1"));
        itemList.add(new Item("", "Item1"));
        itemList.add(new Item("", "Item1"));
        itemList.add(new Item("", "Item1"));


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

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //Set onclick of button to barcode scanner
                    addBarcode.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(context, "Clicked addBarcode", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Home.this, BarcodeCaptureActivity.class);
                            Home.this.startActivity(intent);
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
}
