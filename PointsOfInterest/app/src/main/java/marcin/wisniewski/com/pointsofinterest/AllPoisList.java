package marcin.wisniewski.com.pointsofinterest;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AllPoisList extends ListActivity {

    ArrayList<String> pois;
    ArrayList<POI> usersPois;
    String[] poisNames;
    String[] poisTypes;


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        pois = new ArrayList<>();

        // get pois added by the user
        Bundle extras = getIntent().getExtras();
        usersPois = (ArrayList<POI>) extras.getSerializable("list");
        if(usersPois.size() > 0){
            for(int i=0; i < usersPois.size(); i++){
                POI poi = usersPois.get(i);
                String toJson = "{\"name\":\""+poi.getName()+"\",\"type\":\""+poi.getType()+"\",\"description\":\""+poi.getDescription()+"\",\"lon\":\""+poi.getLon()+"\",\"lat\":\""+poi.getLat()+"\"}";
                pois.add(toJson);
            }
        }

        // add all file saved pois
        readFile();

        // add all web pois
        GETpois t = new GETpois();
        t.execute();

    }

    public void gatherAllInOne(String result){
        try {
            JSONArray jsonArr = new JSONArray(result);
            for(int i=0; i<jsonArr.length(); i++){
                JSONObject curObj = jsonArr.getJSONObject(i);
                pois.add(curObj.toString());
            }
            poisNames = new String[pois.size()];
            poisTypes = new String[pois.size()];
            for(int i=0; i < pois.size(); i++){
                String poi = pois.get(i);
//                System.out.println(poi);
                JSONObject curObj = new JSONObject(poi);
                poisNames[i] = curObj.getString("name");
                poisTypes[i] = curObj.getString("type");
            }
            MyAdapter adapter = new MyAdapter();
            setListAdapter(adapter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class MyAdapter extends ArrayAdapter<String>{
        public MyAdapter(){
            super(AllPoisList.this, android.R.layout.simple_list_item_1, poisNames);
        }

        public View getView(int index, View convertView, ViewGroup parent){
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.customapoiview, parent, false);
            TextView name = (TextView)view.findViewById(R.id.poi_name);
            TextView type = (TextView)view.findViewById(R.id.poi_type);
            name.setText(poisNames[index]);
            type.setText(poisTypes[index]);
            return view;
        }
    }

    // gets saved pois from everywhere
    private void readFile() {
        try {
            String dirPath = getFilesDir().getAbsolutePath() + File.separator;
            File file = new File(dirPath, "pois.csv");
            if(file.exists()){
                FileReader fileReader = new FileReader(file);
                BufferedReader br = new BufferedReader((fileReader));
                String line;
                while ((line = br.readLine()) != null) {
                    pois.add(line);
                }
                fileReader.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            gatherAllInOne(result);
        }
    }

    public void onListItemClick(ListView lv, View view, int index, long id)
    {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString("selectedPOI", pois.get(index));
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }


}
