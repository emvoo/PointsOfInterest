package marcin.wisniewski.com.pointsofinterest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class PointOfInterest extends Activity {
    // define map variable
    private MapView mv;
    // define location manager variable
    private LocationManager lm;
    // define variables to store both latitude and longitude
    double lat, lon;
    // flag for GPS status
    boolean isGPSEnabled = false;
    // define variable with all POI's to be displayed as markers
    ItemizedIconOverlay<OverlayItem> items;
    // define gesture listener for sigle.long clicks on chosen POI
    MyOverlayGestureListener markerGestureListener;
    // define list to store added POI's before saving them to the file
    ArrayList<POI> usersPOIs;

    SharedPreferences prefs;
    boolean savetoweb;

    /**
     *      OVERRIDES
     *
     *      Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // call superclass function and get layout file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize variables
        usersPOIs = new ArrayList<>();
        markerGestureListener = new MyOverlayGestureListener();
        items = new ItemizedIconOverlay<OverlayItem>(this, new ArrayList<OverlayItem>(), markerGestureListener);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        savetoweb = prefs.getBoolean("savetoweb", false);

        // initialize MapView and display it setting zoom and default map controls
        mv = (MapView)findViewById(R.id.map);
        mv.setBuiltInZoomControls(true);
        mv.getController().setZoom(14);

        // initialize location manager (gets access to location services of the device)
        lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // getting GPS status
        isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        // if gps is not enabled display error message allowing user to go to change the settings
        if(!isGPSEnabled){
            // show error message
            showSettingsAlert();
        }else{
            // gps enabled than get current position
            requestLocUpdates();
        }
    }

    // called when activity becomes visible (usually visible again after being hidden)
    @Override
    public void onStart(){
        super.onStart();
    }

    // executed when different activity closes and this activity becomes active again
    @Override
    protected void onResume() {
        super.onResume();
        // update position on the map
        requestLocUpdates();
        // read file contents
        //getUserPois();
    }

    // this activity will be paused if different activity will be called and opened
    @Override
    protected void onPause() {
        super.onPause();
        // stop updates when application is not being used but not closed
        lm.removeUpdates(myLocationListener);
    }

    // called when application closes
    @Override
    protected void onDestroy(){
        super.onDestroy();
        // save all added POIs when closing the application
        saveNewPOIs();
    }

    /**
     * END OVERRIDES
     */


    class MyOverlayGestureListener implements ItemizedIconOverlay.OnItemGestureListener<OverlayItem>
    {
        public boolean onItemLongPress(int i, OverlayItem item)
        {
            Toast.makeText(PointOfInterest.this, "LONG PRESS: " + item.getSnippet(), Toast.LENGTH_SHORT).show();
            return true;
        }

        public boolean onItemSingleTapUp(int i, OverlayItem item)
        {
            // przyda sie do zadania chyba 8
            // GeoPoint point = (GeoPoint) item.getPoint();
            Toast.makeText(PointOfInterest.this, "TAP: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    // display all saved in the file POIs on the map as markers
    public void displaySavedMarkers(){
        ArrayList<POI> savedPois = getUserPois();
        if(savedPois != null){
            for(POI poi:savedPois){
                displayPoiMarker(poi);
            }
        }
    }

    class GETpois extends AsyncTask<Void, Void, String>{
        public String doInBackground(Void... unused){
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://www.free-map.org.uk/course/mad/ws/get.php?year=17&username=user024&format=json");
                conn = (HttpURLConnection)url.openConnection();
                InputStream in = conn.getInputStream();
                if(conn.getResponseCode() == 200){
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String result = "", line;
                    while((line = br.readLine()) != null){
                        result += line;
                    }
                    return result;
                }
                else{
                    return "HTTP ERROR: " + conn.getResponseCode();
                }
            }catch(IOException e){
                return e.toString();
            }
            finally{
                if(conn!=null){
                    conn.disconnect();
                }
            }
        }

        public void onPostExecute(String result){
            try {
                JSONArray jsonArr = new JSONArray(result);
                for(int i=0; i<jsonArr.length(); i++){
                    JSONObject curObj = jsonArr.getJSONObject(i);
                    String name = curObj.getString("name");
                    String type = curObj.getString("type");
                    String desc = curObj.getString("description");
                    Double lat = Double.parseDouble(curObj.getString("lat"));
                    Double lon = Double.parseDouble(curObj.getString("lon"));

                    POI poi = new POI(name, type, desc, lat, lon);
                    displayPoiMarker(poi);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
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

    private void saveToWeb(POI poi) {
        POSTpois posTpois = new POSTpois();
        posTpois.execute(poi);
    }

    // retrieves poi's from web
    public void displayWebPOIs(){
        GETpois t = new GETpois();
        t.execute();
    }

    // display single POI on the map
    public void displayPoiMarker(POI poi){
        OverlayItem item = new OverlayItem(
                poi.getType(),
                poi.getName(),
                poi.getDescription(),
                new GeoPoint(poi.getLat(), poi.getLon())
        );
        item.setMarker(getResources().getDrawable(R.drawable.user_marker));
        items.addItem(item);
        mv.getOverlays().add(items);
        mv.invalidate();
    }

    // location listener required to keep users position updated (centered)
    private LocationListener myLocationListener = new LocationListener(){

        @Override
        public void onLocationChanged(Location location) {
            updateLoc(location);
        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

    };

    // center map to users current position
    private void updateLoc(Location loc){
        // get cuerrent location and
        this.lat = loc.getLatitude();
        this.lon = loc.getLongitude();
        GeoPoint locGeoPoint = new GeoPoint(this.lat, this.lon);
        mv.getController().setCenter(locGeoPoint);
    }

    // display alert message if GPS is not enabled
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(PointOfInterest.this);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                PointOfInterest.this.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    // update map position
    public void requestLocUpdates(){
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
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
            bundle.putDouble("lat", this.lat);
            bundle.putDouble("lon", this.lon);
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
            saveNewPOIs();
            return true;
        }

        // display all saved to file markers
        if(item.getItemId() == R.id.displaymarkers){
            displaySavedMarkers();
            return true;
        }

        if(item.getItemId() == R.id.displayfromweb){
            displayWebPOIs();
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
                POI newPOI= new POI(poiname, poitype, poidesc, lat, lon);
                // add newPOi to ArrayList
                usersPOIs.add(newPOI);
                // add new POI to the map
                displayPoiMarker(newPOI);
                if(savetoweb){
                    saveToWeb(newPOI);
                }
            }
        }
    }


    // save all new POIs to file
    public void saveNewPOIs(){
        if(usersPOIs.size() > 0){
            for(POI poi:usersPOIs){
                savePoiToFile(poi);
            }
            usersPOIs.clear();
            Toast.makeText(PointOfInterest.this, "Saved", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(PointOfInterest.this, "Nothing to save. Add new POI to the map first.", Toast.LENGTH_SHORT).show();
        }
    }

    // save single poi to file
    public void savePoiToFile(POI poi){
        String poiname = poi.getName();
        String poitype = poi.getType();
        String poidesc = poi.getDescription();
        Double lat = poi.getLat();
        Double lon = poi.getLon();
        PrintWriter writer;
        String dirPath = getFilesDir().getAbsolutePath() + File.separator;
        File file = new File(dirPath, "pois.csv");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            writer = new PrintWriter(new FileWriter(file, true));
            writer.print(poiname + ','+poitype+','+poidesc+','+lat+','+lon);
            writer.append("\r\n");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // function to get all saved to the file POIs by the user
    public ArrayList<POI> getUserPois(){
        // get path to file
        String dirPath = getFilesDir().getAbsolutePath() + File.separator;
        ArrayList<POI> usersPois = new ArrayList<>();
        try {
            // read file contents line by line treating each line as new POI
            FileReader file = new FileReader(dirPath + "pois.csv");
            BufferedReader reader = new BufferedReader(file);
            String line = "";
            String[] values = new String[5];
            while((line = reader.readLine()) != null){
                values = line.split(",");
                usersPois.add(new POI(values[0], values[1], values[2], Double.parseDouble(values[3]), Double.parseDouble(values[4])));
            }
            reader.close();
            return usersPois;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
