package com.example.barcode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback{
    private static final int MY_CAMERA_REQUEST_CODE = 1;
    private Button _scanbutn;
    private ProgressBar _progressbar;
    private ImageView _thumbnail;
    private TextView _textview;
    private String _response;
    private String _formattedresponse = "";
    private HashMap<String,String> _requireddata = new HashMap<String, String>();
    private final String[] _requiredkeys = {
            "product_name",
            "ingredients_text_with_allergens",
            "ingredients_text",
            "labels",
            "labels_hierarchy",
            "ingredients_analysis_tags",
            "brands",
            "nutriments",
            "nutriscore_points",
            "energy",
            "nutrient_levels_tags",
            "nutrition_grades",
            "ingredients_from_or_that_may_be_from_palm_oil_n"
    };
    private final String[] _requirednutrientskeys = {
            "salt",
            "fat",
            "sodium",
            "sugars",
            "saturated_fat",
            "proteins",
            "energy",
            "carbohydrates"
    };
    private JSONObject _parsedresponse;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _scanbutn = findViewById(R.id.scanbutn);
        _textview = findViewById(R.id.results);
        _progressbar = findViewById(R.id.progressBar);
        _thumbnail = findViewById(R.id.thumbnail);
        _textview.setMovementMethod(new ScrollingMovementMethod());
        _scanbutn.setOnClickListener(this);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
            switch (requestCode) {
                case MY_CAMERA_REQUEST_CODE:
                    // If request is cancelled, the result arrays are empty.
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // Permission is granted. Continue the action or workflow
                        // in your app.
                    }  else {
                        // Explain to the user that the feature is unavailable because
                        // the features requires a permission that the user has denied.
                        // At the same time, respect the user's decision. Don't link to
                        // system settings in an effort to convince the user to change
                        // their decision.
                        finishAffinity();
                    }
                    return;
            }
            // Other 'case' lines to check for other
            // permissions this app might request.
    }
    @Override
    public void onClick(View v) {
        scanCode();
    }
    private void scanCode(){
        IntentIntegrator integrator = new IntentIntegrator(this); // IntentIntegrator is part of ZXing module
        integrator.setCaptureActivity(CaptureAct.class);
        integrator.setOrientationLocked(false);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.PRODUCT_CODE_TYPES);
        integrator.setPrompt("scanning...");
        integrator.initiateScan();
    }

    private void handleResponse(){
        try {
            _formattedresponse = "";
            _requireddata.clear();
            _parsedresponse = new JSONObject(_response);
            JSONObject j = _parsedresponse.getJSONObject("product");
            Iterator<String> it = j.keys();
            while(it.hasNext()) {
                String key =  it.next();
                String val = j.getString(key);
                if(val != ""){
                    for (int i=0; i<_requiredkeys.length; i++){ // for selecting certain keys
                        if(key.equals( _requiredkeys[i])){
                            //_formattedresponse += key +": "+ val + "\n"+ "\n";
                            _requireddata.put(key,val);
                        }
                    }
                    //_formattedresponse += key +": "+ val + "\n"+ "\n";
                }
            }
            _formattedresponse = produceFormattedResponse();
            String url = "";
            try {
                url = j.getString("image_url");
                if(url != null && url != ""){
                    try {
                        Picasso.get()
                                .load(url)
                                .resize(800, 800)
                                .centerInside()
                                .into(_thumbnail);
                        _thumbnail.setVisibility(View.VISIBLE);
                    }
                    catch (Exception e){
                        _thumbnail.setVisibility(View.INVISIBLE);
                    }

                }
            }
            catch (Exception e){
                System.out.println(e.getMessage());
                System.out.println(e.getCause());
            }
            //JSONObject images_parsed = new JSONObject(j.getString("images"));


            //System.out.println(_parsedresponse);
        }
        catch(JSONException e){
            //_formattedresponse = e.getMessage();
            _thumbnail.setVisibility(View.INVISIBLE);
        }
        finally {
            _textview.setText(Html.fromHtml(_formattedresponse));
            //_textview.setText(_formattedresponse);
            _progressbar.setVisibility(View.INVISIBLE);
        }
    }

    private String produceFormattedResponse() {
//                "product_name",
//                "ingredients_text_with_allergens",
//                "ingredients_text",
//                "labels",
//                "labels_hierarchy",
//                "ingredients_analysis_tags",
//                "brands",
//                "nutriments",
//                "nutriscore_points",
//                "nutrient_levels_tags",
//                "nutrition_grades",
//                "ingredients_from_or_that_may_be_from_palm_oil_n"
        String response = "";
        if(_requireddata.containsKey("product_name")){
            response += "<b>Product Name :</b> "+ _requireddata.get("product_name") + ", ";
        }
        if(_requireddata.containsKey("brands")){
            response += "<b>Brand :</b> "+ _requireddata.get("brands") + "\n"+ "\n";
        }
        if(_requireddata.containsKey("labels")){
            response += "<b>Labels :</b> "+ _requireddata.get("labels") + "\n"+ "\n";
        }
        if(_requireddata.containsKey("labels_hierarchy")){
            if(!_requireddata.get("label_hierarchy").equals("[]")) {
                response += "<b>Labels Hierarchy :</b> " + _requireddata.get("labels_hierarchy") + "\n" + "\n";
            }
        }
        if(_requireddata.containsKey("ingredients_text_with_allergens")){
            response += "<b>Ingredients (With Allergens) :</b> "+ _requireddata.get("ingredients_text_with_allergens") + "\n"+ "\n";
        }
        if(_requireddata.containsKey("ingredients_from_or_that_may_be_from_palm_oil_n")){
            response += "<b>Number of Possible Palm Oil Ingredients :</b> "+ _requireddata.get("ingredients_from_or_that_may_be_from_palm_oil_n") + "\n"+ "\n";
        }
        if(_requireddata.containsKey("ingredients_text")){
            response += "<b>Ingredients :</b> "+ _requireddata.get("ingredients_text") + "\n"+ "\n";
        }

        if(_requireddata.containsKey("ingredients_analysis_tags")){
            response += "<b>Ingredients Tags :</b> "+ _requireddata.get("ingredients_analysis_tags") + "\n"+ "\n";
        }

        if(_requireddata.containsKey("nutriments")){
            //response += "Nutritional Information : "+ _requireddata.get("nutriments") + "\n"+ "\n";
            response += generateNutrientsBPoints(_requireddata.get("nutriments"))+ "\n"+ "\n";
        }
        if(_requireddata.containsKey("nutriscore_points")){
            response += "<b>Nutriscore :</b> "+ _requireddata.get("nutriscore_points") + "\n"+ "\n";
        }
        if(_requireddata.containsKey("nutrient_levels_tags")){
            if(!_requireddata.get("nutrient_levels_tags").equals("[]")){
                response += "<b>Nutrient Tags :</b> "+ _requireddata.get("nutrient_levels_tags") + "\n"+ "\n";
            }
        }
        if(_requireddata.containsKey("nutrition_grades")){
            response += "<b>Nutrition Grades :</b> "+ _requireddata.get("nutrition_grades") + "\n"+ "\n";
        }

        return response;
    }

    private String generateNutrientsBPoints(String nutristring) {
        String return_s = "";

        try {
            JSONObject parsed = new JSONObject(nutristring);
            return_s += "<ul>";
            return_s += "<b>Nutritional Information Per 100g</b> \n";
            Iterator<String> it = parsed.keys();
            while(it.hasNext()) {
                String key =  it.next();
                String val = parsed.getString(key);
                if(val != ""){
                    for (int i=0; i<_requirednutrientskeys.length; i++){ // for selecting certain keys
                        if(key.equals( _requirednutrientskeys[i])){
                            return_s += "<li><b>"+key+":</b> "+(String)val+" "+parsed.getString(key+"_unit")+"</li>";
                            //return_s += key +": "+(String)val+ parsed.getString(key+"_unit")+"\n";

                        }
                    }
                    //_formattedresponse += key +": "+ val + "\n"+ "\n";
                }
            }
            return_s += "</ul>";

        }
        catch (JSONException e){
            return_s =  e.getMessage();
        }
        finally {
            return return_s;
        }

    }

    @Override
    protected void onActivityResult(int requestcode, int resultcode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestcode,resultcode,data);
        if(result != null){
            if(result.getContents() != null){
                lookupBarcode(result.getContents());
            }
            else{
                Toast.makeText(this,"no results",Toast.LENGTH_LONG).show();
            }
        }
        else{
            super.onActivityResult(requestcode,resultcode,data);
        }
    }
    private void lookupBarcode(String number){
        _progressbar.setVisibility(View.VISIBLE);
        RequestQueue queue = Volley.newRequestQueue(this);
        //String url ="https://api.barcodelookup.com/v2/products?barcode="+number+"&formatted=y&key=ya5ltzg3e9j0fdcq26wwdnutkh6lnx";
        String url = "https://world.openfoodfacts.org/api/v0/product/"+number+".json";
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        _response = response;
                        handleResponse();
                    }

                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        _response = "ERROR";
                        _thumbnail.setVisibility(View.INVISIBLE);
                        handleResponse();
                    }

        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("User-Agent", "In Development App - Android - V1.0 - jimmarshall35@gmail.com");

                return params;
            }
        };
        queue.add(stringRequest);
    }
}
