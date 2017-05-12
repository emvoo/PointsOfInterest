package marcin.wisniewski.com.pointsofinterest;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;


import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class PointOfInterest extends Activity
{
    ArrayList<POI> usersPOIs;
    SharedPreferences prefs;
    boolean savetoweb;

    MapFragment mapFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        usersPOIs = new ArrayList<>();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        savetoweb = prefs.getBoolean("savetoweb", false);

        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map_fragment);
    }

    // called when activity becomes visible (usually visible again after being hidden)
    @Override
    public void onStart(){
        super.onStart();
        mapFragment.requestLocUpdates();
    }

    // executed when different activity closes and this activity becomes active again
    @Override
    protected void onResume() {
        super.onResume();
        // update position on the map
//        mapFragment.requestLocUpdates();
        // read file contents
//        getUserPois();

    }

    // this activity will be paused if different activity will be called and opened
    @Override
    protected void onPause() {
        super.onPause();
        // stop updates when application is not being used but not closed
        mapFragment.stopLocationListener();
        mapFragment.saveNewPOIs();
    }

    // called when application closes
    @Override
    protected void onDestroy(){
        super.onDestroy();
        // save all added POIs when closing the application
        mapFragment.saveNewPOIs();
    }

    // make menu appear in our application
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // action to be taken when menu item selected
    public boolean onOptionsItemSelected(MenuItem item){

        // option in the menu to add new POI selected
        if(item.getItemId() == R.id.addpoi){
            Intent intent = new Intent(this, AddPOI.class);
            Bundle bundle = new Bundle();
            // pass latitude and longitude to the new activity
            bundle.putDouble("lat", mapFragment.getLat());
            bundle.putDouble("lon", mapFragment.getLon());
            intent.putExtras(bundle);
            startActivityForResult(intent, 0);
            return true;
        }

        // preferences option in the menu selected
        if(item.getItemId() == R.id.savetoweb){
            Intent intent = new Intent(this, PreferencesActivity.class);
            startActivity(intent);
            return true;
        }

        // save all added pois to the map option selected
        if(item.getItemId() == R.id.savealladded){
            mapFragment.saveNewPOIs();
            return true;
        }

        // display all saved to file markers
        if(item.getItemId() == R.id.displaymarkers){
            mapFragment.displaySavedMarkers();
            return true;
        }

        if(item.getItemId() == R.id.displayfromweb){
            mapFragment.displayWebPOIs();
            return true;
        }

        if(item.getItemId() == R.id.viewlistofallpois){
            Intent intent = new Intent(this, AllPoisListActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("list", mapFragment.getUsersPOIs());
            intent.putExtras(bundle);
            startActivityForResult(intent, 1);
            return true;
        }

        return false;
    }

    // action to be taken when theres expected result from other activity
    protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                // get data saved in AddPOI activity
                final Bundle extras = intent.getExtras();
                String poiname = extras.getString("poiname");
                String poitype = extras.getString("poitype");
                String poidesc = extras.getString("poidesc");
                Double lat = extras.getDouble("lat");
                Double lon = extras.getDouble("lon");
                // create new POI object
                POI newPOI= new POI(poiname, poitype, poidesc, lon, lat);
                // add newPOi to ArrayList
                mapFragment.addToUsersPOIs(newPOI);
                usersPOIs.add(newPOI);
                // add new POI to the map
                mapFragment.displayPoiMarker(newPOI);
                if(savetoweb){
                    saveToWeb(newPOI);
                }
            }
        }
        if(requestCode == 1){
            if(resultCode == RESULT_OK){
                Bundle extras = intent.getExtras();
                String selected = extras.getString("selectedPOI");
                receivePOI(selected);
            }
        }
    }

    private void saveToWeb(POI poi) {
        POSTpois posTpois = new POSTpois();
        posTpois.execute(poi);
    }

    class POSTpois extends AsyncTask<POI, Void, POI> {
        public POI doInBackground(POI... pois){
            POI poi = pois[0];
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://www.free-map.org.uk/course/mad/ws/add.php");
                conn = (HttpURLConnection)url.openConnection();
                String username = "user024";
                String poiName = poi.getName();
                String poiType = poi.getType();
                String poiDesc = poi.getDescription();
                String lat = String.valueOf(poi.getLat());
                String lon = String.valueOf(poi.getLon());
                String postData = "username="+username+"&name="+poiName+"&type="+poiType+"&description="+poiDesc+"&lat="+lat+"&lon="+lon+"&year=17";
                // For POST
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(postData.length());

                OutputStream out = null;
                out = conn.getOutputStream();
                out.write(postData.getBytes());

                if(conn.getResponseCode() == 200)
                    return poi;
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally{
                if(conn!=null){
                    conn.disconnect();
                }
            }
            return null;
        }

        public void onPostExecute(POI poi){
            if(poi != null){
                Toast.makeText(PointOfInterest.this, poi.getName()+" saved to web.", Toast.LENGTH_LONG).show();
            }else
                Toast.makeText(PointOfInterest.this, "Something went wrong. POI not saved to web.", Toast.LENGTH_LONG).show();
        }

    }

    public ArrayList<POI> getUsersPOIs(){
        return this.usersPOIs;
    }

    public void receivePOI(String poi){
        mapFragment.centerToPOI(poi);
    }
}
