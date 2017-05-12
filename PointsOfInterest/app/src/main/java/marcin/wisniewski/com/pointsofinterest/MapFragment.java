package marcin.wisniewski.com.pointsofinterest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MapFragment extends Fragment
{
    // define map variable
    private MapView mv;
    // define location manager variable
    private LocationManager lm;
    // define variables to store both latitude and longitude
    private double lat, lon;
    // flag for GPS status
    boolean isGPSEnabled = false;
    // define variable with all POI's to be displayed as markers
    ItemizedIconOverlay<OverlayItem> items;
    // define gesture listener for sigle.long clicks on chosen POI
    MyOverlayGestureListener markerGestureListener;
    // define list to store added POI's before saving them to the file
    ArrayList<POI> usersPOIs;

    boolean follow = true;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView (LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.mapfragment, parent);
    }

    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();

        // initialize variables
        usersPOIs = new ArrayList<>();
        markerGestureListener = new MyOverlayGestureListener();
        items = new ItemizedIconOverlay<OverlayItem>(getActivity(), new ArrayList<OverlayItem>(), markerGestureListener);


        // initialize MapView and display it setting zoom and default map controls
        mv = (MapView)activity.findViewById(R.id.map);
        mv.setBuiltInZoomControls(true);
        mv.getController().setZoom(14);

        // initialize location manager (gets access to location services of the device)
        lm = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
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

    class MyOverlayGestureListener implements ItemizedIconOverlay.OnItemGestureListener<OverlayItem>{
        public boolean onItemLongPress(int i, OverlayItem item)
        {
            Toast.makeText(getActivity(), "LONG PRESS: " + item.getSnippet(), Toast.LENGTH_SHORT).show();
            return true;
        }

        public boolean onItemSingleTapUp(int i, OverlayItem item)
        {
            // przyda sie do zadania chyba 8
            // GeoPoint point = (GeoPoint) item.getPoint();
            Toast.makeText(getActivity(), "TAP: " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    // display alert message if GPS is not enabled
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getActivity().startActivity(intent);
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

    // location listener required to keep users position updated (centered)
    private LocationListener myLocationListener = new LocationListener(){

        @Override
        public void onLocationChanged(Location location) {
            if(follow)
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

    // save all new POIs to file
    public void saveNewPOIs(){
        if(usersPOIs.size() > 0){
            for(POI poi:usersPOIs){
                savePoiToFile(poi);
            }
            usersPOIs.clear();
            Toast.makeText(getActivity(), "Saved", Toast.LENGTH_SHORT).show();
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
        String dirPath = getActivity().getFilesDir().getAbsolutePath() + File.separator;
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
            JSONObject jsonObj = new JSONObject("{\"phonetype\":\"N95\",\"cat\":\"WP\"}");
            String toJson = "{\"name\":\""+poiname+"\",\"type\":\""+poitype+"\",\"description\":\""+poidesc+"\",\"lon\":\""+lon+"\",\"lat\":\""+lat+"\"}";
            JSONObject jsonObject = new JSONObject(toJson);
            writer.print(jsonObject);
            writer.append("\r\n");
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // display all saved in the file POIs on the map as markers
    public void displaySavedMarkers(){
        ArrayList<String> savedPois = getUserPois();
        if(savedPois != null){
            for(String savedPoi:savedPois){
                try {
                    JSONObject jsonPoi = new JSONObject(savedPoi);
                    POI poi = new POI(jsonPoi.getString("name"), jsonPoi.getString("type"), jsonPoi.getString("description"), Double.parseDouble(jsonPoi.getString("lon")), Double.parseDouble(jsonPoi.getString("lat")));
                    System.out.println(poi.getType());
                    displayPoiMarker(poi);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // function to get all saved to the file POIs by the user
    public ArrayList<String> getUserPois(){
        // get path to file
        ArrayList<String> usersPois = new ArrayList<>();
        try {
            String dirPath = getActivity().getFilesDir().getAbsolutePath() + File.separator;
            File file = new File(dirPath, "pois.csv");
            if(file.exists()){
                FileReader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader((fileReader));
                String line;
                while ((line = br.readLine()) != null) {
                    usersPois.add(line);
                }
                fileReader.close();
                return usersPois;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // retrieves poi's from web
    public void displayWebPOIs(){
        GETpois t = new GETpois();
        t.execute();
    }

    class GETpois extends AsyncTask<Void, Void, String> {
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
                    Double lon = Double.parseDouble(curObj.getString("lon"));
                    Double lat = Double.parseDouble(curObj.getString("lat"));

                    POI poi = new POI(name, type, desc, lon, lat);
                    displayPoiMarker(poi);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void centerToPOI(String poi){
        follow = false;
        try {
            JSONObject p = new JSONObject(poi);
            POI newPoi = new POI(p.getString("name"), p.getString("type"), p.getString("description"), Double.parseDouble(p.getString("lon")), Double.parseDouble(p.getString("lat")));
            Double lat = Double.parseDouble(p.getString("lat"));
            Double lon = Double.parseDouble(p.getString("lon"));
            GeoPoint gp = new GeoPoint(lat, lon);
            mv.getController().setZoom(15);
            mv.getController().setCenter(gp);
            items.removeAllItems();
            displayPoiMarker(newPoi);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Double getLat(){ return this.lat; }
    public Double getLon(){ return this.lon; }
    public ArrayList<POI> getUsersPOIs(){ return this.usersPOIs; }
    public MapView getMapView(){ return this.mv; }

    public void stopLocationListener(){ lm.removeUpdates(myLocationListener); }
    public void addToUsersPOIs(POI poi){ this.usersPOIs.add(poi); }
}