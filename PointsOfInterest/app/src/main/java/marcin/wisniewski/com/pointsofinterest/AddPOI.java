package marcin.wisniewski.com.pointsofinterest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddPOI extends Activity implements View.OnClickListener {
    Double lat, lon;
    MapFragment mapFragment;
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addpoi);
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.map_fragment);
        // define a button and attach event listener to it
        Button addpoibtn = (Button)findViewById(R.id.addpoibtn);
        addpoibtn.setOnClickListener(this);

        Bundle extras = getIntent().getExtras();
        lat = extras.getDouble("lat");
        lon = extras.getDouble("lon");
        System.out.println(lat);

    }

    // function to handle click event on the button
    public void onClick(View v){
        // get values of text fields
        String poiname = ((EditText)findViewById(R.id.poiname)).getText().toString();
        String poitype = ((EditText)findViewById(R.id.poitype)).getText().toString();
        String  poidesc = ((EditText)findViewById(R.id.poidesc)).getText().toString();
        // check fields not empty
        if(poiname.equals("") || poitype.equals("") || poidesc.equals("")){
            // display error message stating that
            Toast.makeText(AddPOI.this, R.string.empty_fields , Toast.LENGTH_SHORT).show();
        }
        else{
            // add entered values to the bundle and return to main screen
            Intent intent = new Intent();
            Bundle bundle = new Bundle();

            bundle.putString("poiname", poiname);
            bundle.putString("poitype", poitype);
            bundle.putString("poidesc", poidesc);
            bundle.putDouble("lat", lat);
            bundle.putDouble("lon", lon);
            intent.putExtras(bundle);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
