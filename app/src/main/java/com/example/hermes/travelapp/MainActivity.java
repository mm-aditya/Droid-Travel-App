package com.example.hermes.travelapp;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import supportlib.HawkerUtils;
import supportlib.Location;
import supportlib.NearestNeighbour;
import supportlib.PathsAndCost;
import supportlib.SearchUtils;
import supportlib.PathInfo.TRANSPORTATION;
import supportlib.TravelSQL;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    static ValueAnimator posAnim, opAnim, posAnim1, opAnim1, posAnim2, opAnim2, posAnim3, opAnim3, posAnimScreenOutLeft, opAnimScreenOutLeft, posAnimScreenOutRight, opAnimScreenOutRight, posAnimScreenInLeft, opAnimScreenInLeft, posAnimScreenInRight, opAnimScreenInRight;
    static int currScreen = 1;
    static int animScreen = 1;
    static int nextScreen;
    static int initial = 0;
    static int hotel;
    static ArrayList<RelativeLayout> screens = new ArrayList<RelativeLayout>();
    RelativeLayout screen1, screen2, screen3, screen4, screen5, screen6;
    ArrayList<Integer> selectedLoc = new ArrayList<Integer>();
    static double budget = 0;

    private GoogleMap mMap;
    private LatLng curr;

    String copy2clip;

    PathsAndCost resultPnC;

    ViewGroup linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        screen1 = (RelativeLayout) findViewById(R.id.content_main);
        screen2 = (RelativeLayout) findViewById(R.id.budget);
        screen3 = (RelativeLayout) findViewById(R.id.hotels);
        screen4 = (RelativeLayout) findViewById(R.id.destinations);
        screen5 = (RelativeLayout) findViewById(R.id.activity_itinerary);
        screen6 = (RelativeLayout) findViewById(R.id.maps);
        screen1.setVisibility(View.VISIBLE);
        screen2.setX(2000);
        screen2.setVisibility(View.VISIBLE);
        screen3.setX(2000);
        screen3.setVisibility(View.VISIBLE);
        screen4.setX(2000);
        screen4.setVisibility(View.VISIBLE);
        screen5.setX(2000);
        screen5.setVisibility(View.VISIBLE);
        screen6.setX(2000);
        screen6.setVisibility(View.VISIBLE);
        screens.add(null);
        screens.add(screen1);
        screens.add(screen2);
        screens.add(screen3);
        screens.add(screen4);
        screens.add(screen5);
        screens.add(screen6);
        initial = 0;
        TravelSQL tsql = new TravelSQL(getApplicationContext());
        SQLiteDatabase db = tsql.getWritableDatabase();
        tsql.onUpgrade(db,1,2);

        final GridView hotelsview = (GridView) findViewById(R.id.hotelsview);
        hotelsview.setAdapter(new HotelsAdapter(this));

        final GridView gridview = (GridView) findViewById(R.id.gridview);

        hotelsview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                hotel = (int) hotelsview.getAdapter().getItemId(position);
                gridview.setAdapter(new ImageAdapter(MainActivity.this, hotel));
                animScreen = 3;
                nextScreen = 4;
                animationStart();
                currScreen = 4;
            }
        });

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if(! selectedLoc.contains(position)){
                    selectedLoc.add((int) gridview.getAdapter().getItemId(position));
                    gridview.getChildAt(position).findViewById(R.id.imageViewTick).setVisibility(View.VISIBLE);
                }
                else{
                    selectedLoc.remove(selectedLoc.indexOf((int) gridview.getAdapter().getItemId(position)));
                    gridview.getChildAt(position).findViewById(R.id.imageViewTick).setVisibility(View.INVISIBLE);
                }
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab2);
        fab.setImageBitmap(textAsBitmap("Fast Approximation", 40, Color.WHITE));
        linearLayout = (ViewGroup) findViewById(R.id.scroller);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Integer> feedArray = new ArrayList<Integer>();
                for (int i = 0; i < selectedLoc.size(); i++) feedArray.add(selectedLoc.get(i));
                HashMap<Integer, Location> allLocations  = SearchUtils.getRawData(feedArray, getApplicationContext(), hotel);
                resultPnC = SearchUtils.getBestPath((ArrayList) SearchUtils.generateAllPaths(feedArray),budget,allLocations,hotel);
                resultPnC = NearestNeighbour.getApproximatedPath(allLocations,budget,hotel);
                generateItinerary();
                animScreen = 4;
                nextScreen = 5;
                animationStart();
                currScreen = 5;


            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Button locationSubmit = (Button) findViewById(R.id.mapSubmitBtn);
        locationSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                hideKeyboard(MainActivity.this);
                onMapSearch(v);
            }
        });

        final Button nearbySubmit = (Button) findViewById(R.id.mapNearbyButton);
        nearbySubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //hideKeyboard(MapsActivity.this);
                nearbySearch(v);
            }
        });


        //call genElement here for each destination and travel method
        //format: genElement( ArrayList of string , identifierCode)
        //case 0: display destination card with just destination name
        //string contains at position 0:name of destination
        //case 1: display travel by public or foot card with time
        //string contains at position 0:travel by foot/take public transport
        //at position 1: time required
        //case 2: display clickable private transport which opens uber app with time
        //string contains at position 0: time taken

    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        posAnimScreenOutLeft = ValueAnimator.ofFloat(0, -1 * screen1.getWidth());
        posAnimScreenOutLeft.setDuration(600);
        posAnimScreenOutLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(animScreen).setX((float) animation.getAnimatedValue());
                screens.get(animScreen).requestLayout();
            }
        });
        opAnimScreenOutLeft = ValueAnimator.ofFloat(1, 0);
        opAnimScreenOutLeft.setDuration(600);
        opAnimScreenOutLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(animScreen).setAlpha((float) animation.getAnimatedValue());
                screens.get(animScreen).requestLayout();
            }
        });

        posAnimScreenOutRight = ValueAnimator.ofFloat(0, screen1.getWidth());
        posAnimScreenOutRight.setDuration(600);
        posAnimScreenOutRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(animScreen).setX((float) animation.getAnimatedValue());
                screens.get(animScreen).requestLayout();
            }
        });
        opAnimScreenOutRight = ValueAnimator.ofFloat(1, 0);
        opAnimScreenOutRight.setDuration(600);
        opAnimScreenOutRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(animScreen).setAlpha((float) animation.getAnimatedValue());
                screens.get(animScreen).requestLayout();
            }
        });

        posAnimScreenInLeft = ValueAnimator.ofFloat(-1 * screen1.getWidth(), 0);
        posAnimScreenInLeft.setDuration(600);
        posAnimScreenInLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(nextScreen).setX((float) animation.getAnimatedValue());
                screens.get(nextScreen).requestLayout();
            }
        });
        opAnimScreenInLeft = ValueAnimator.ofFloat(0, 1);
        opAnimScreenInLeft.setDuration(600);
        opAnimScreenInLeft.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(nextScreen).setAlpha((float) animation.getAnimatedValue());
                screens.get(nextScreen).requestLayout();
            }
        });

        posAnimScreenInRight = ValueAnimator.ofFloat(screen1.getWidth(),0);
        posAnimScreenInRight.setDuration(600);
        posAnimScreenInRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(nextScreen).setX((float) animation.getAnimatedValue());
                screens.get(nextScreen).requestLayout();
            }
        });
        opAnimScreenInRight = ValueAnimator.ofFloat(0, 1);
        opAnimScreenInRight.setDuration(600);
        opAnimScreenInRight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                screens.get(nextScreen).setAlpha((float) animation.getAnimatedValue());
                screens.get(nextScreen).requestLayout();
            }
        });

        if (initial == 0) {
            final TextView what = (TextView) findViewById(R.id.textView2);
            posAnim = ValueAnimator.ofFloat(screen1.getHeight() / 2 - 150, screen1.getHeight() / 2 - 350);//1000, 800);
            posAnim.setDuration(1000).setStartDelay(250);
            posAnim.start();
            posAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    what.setY((float) animation.getAnimatedValue());
                    what.requestLayout();
                }
            });
            opAnim = ValueAnimator.ofFloat(0, 1);
            opAnim.setDuration(1000).setStartDelay(250);
            opAnim.start();
            opAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    what.setAlpha((float) animation.getAnimatedValue());
                    what.requestLayout();
                }
            });

            final Button newTrip = (Button) findViewById(R.id.newTrip);
            posAnim1 = ValueAnimator.ofFloat(screen1.getHeight() / 2 + 200, screen1.getHeight() / 2);//1000, 800);
            posAnim1.setDuration(800).setStartDelay(750);
            posAnim1.start();
            posAnim1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    newTrip.setY((float) animation.getAnimatedValue());
                    newTrip.requestLayout();
                }
            });
            opAnim1 = ValueAnimator.ofFloat(0, 1);
            opAnim1.setDuration(800).setStartDelay(750);
            opAnim1.start();
            opAnim1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    newTrip.setAlpha((float) animation.getAnimatedValue());
                    newTrip.requestLayout();
                }
            });

            final Button checkItinerary = (Button) findViewById(R.id.checkItinerary);
            posAnim2 = ValueAnimator.ofFloat(screen1.getHeight() / 2 + 350, screen1.getHeight() / 2 + 150);//1150, 950);
            posAnim2.setDuration(800).setStartDelay(900);
            posAnim2.start();
            posAnim2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    checkItinerary.setY((float) animation.getAnimatedValue());
                    checkItinerary.requestLayout();
                }
            });
            opAnim2 = ValueAnimator.ofFloat(0, 1);
            opAnim2.setDuration(800).setStartDelay(900);
            opAnim2.start();
            opAnim2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    checkItinerary.setAlpha((float) animation.getAnimatedValue());
                    checkItinerary.requestLayout();
                }
            });

            final Button settings = (Button) findViewById(R.id.settings);
            posAnim3 = ValueAnimator.ofFloat(screen1.getHeight() / 2 + 500, screen1.getHeight() / 2 + 300);//1300, 1100);
            posAnim3.setDuration(800).setStartDelay(1050);
            posAnim3.start();
            posAnim3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    settings.setY((float) animation.getAnimatedValue());
                    settings.requestLayout();
                }
            });
            opAnim3 = ValueAnimator.ofFloat(0, 1);
            opAnim3.setDuration(800).setStartDelay(1050);
            opAnim3.start();
            opAnim3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    settings.setAlpha((float) animation.getAnimatedValue());
                    settings.requestLayout();
                }
            });
            initial = 1;
        }
    }

    public void budget(View v){
        animScreen = 1;
        nextScreen = 2;
        animationStart();
        currScreen = 2;
    }

    public void submitBudget(View v){
        if ((((EditText) findViewById(R.id.editText2)).getText().toString()).equals("")){
                    Toast toast = Toast.makeText(this,"Invalid entry!", Toast.LENGTH_SHORT);
                    toast.show();
        }
        else{
            budget = Double.parseDouble(((EditText) findViewById(R.id.editText2)).getText().toString());
            selectedLoc.clear();
            GridView gridview = (GridView) findViewById(R.id.gridview);
            for (int i = 0; i < gridview.getChildCount(); i++){
                gridview.getChildAt(i).findViewById(R.id.imageViewTick).setVisibility(View.INVISIBLE);
            }
            hideKeyboard(MainActivity.this);
            animScreen = 2;
            nextScreen = 3;
            animationStart();
            currScreen = 3;
        }

    }

    public void maps(View v){
        animScreen = 1;
        nextScreen = 6;
        animationStart();
        currScreen = 6;
    }

    public static void hideKeyboard(Activity act) {
        InputMethodManager inputMethodManager = (InputMethodManager) act.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(act.getCurrentFocus().getWindowToken(), 0);
    }

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        curr = new LatLng(1.352083, 103.819836);
        mMap.addMarker(new MarkerOptions().position(curr).title("Marker in Singapore"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(curr, 10.0f));
    }

    public void onMapSearch(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.mapsEdittext);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        if (location != null || !location.equals("") || location.length() != 0) {
            Geocoder geocoder = new Geocoder(this);
            try {
                //only sets preference, not a restriction
                addressList = geocoder.getFromLocationName(location, 1, 1.216988, 103.589401, 1.475781, 104.099579);

                if (addressList.size() > 0) {
                    Address address = addressList.get(0);
                    curr = new LatLng(address.getLatitude(), address.getLongitude());
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions().position(curr));
                    mMap.animateCamera(CameraUpdateFactory.newLatLng(curr));
//                    String loc = address.getLocality()+" in "+address.getCountryName();
//                    Toast toast = Toast.makeText(context,loc,duration);
//                    toast.show();
                } else {
                    Toast toast = Toast.makeText(context, "Location Unknown", duration);
                    toast.show();
                }
            } catch (IOException e) {
                System.err.println(e);
                Toast toast = Toast.makeText(context, "Location Unknown", duration);
                toast.show();
            }
        }
    }

    public void nearbySearch(View v){
        ArrayList<ArrayList<String>> nearby = HawkerUtils.getNearbyHawkers(HawkerUtils.getHawkerLocations(getResources()),curr.latitude,curr.longitude);
        if(nearby.size() ==0){
            Toast toast = Toast.makeText(getApplicationContext(), "No nearby hawkers", Toast.LENGTH_SHORT);
            toast.show();
        }
        for(int i =0;i<nearby.size();i++){
            LatLng mark = new LatLng(Double.parseDouble(nearby.get(i).get(1)),Double.parseDouble(nearby.get(i).get(0)));
            mMap.addMarker(new MarkerOptions().position(mark).title(nearby.get(i).get(2)));
        }
    }



    public void roadmap(View v){
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    public void satellite(View v){
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    }

    public void genElement(ArrayList<String> a, int type){


        LayoutInflater inflater=LayoutInflater.from(this);
        View view;

        switch(type){
            case 0:
                view = inflater.inflate(R.layout.itinerary_destination_item, linearLayout, false);
                TextView x = (TextView) view.findViewById(R.id.textView3);
                x.setText(a.get(0));
                break;
            case 1:
                view = inflater.inflate(R.layout.itenerary_travelmethod_item, linearLayout, false);
                TextView y = (TextView) view.findViewById(R.id.textView3);
                y.setText(a.get(0));
                TextView f = (TextView) view.findViewById(R.id.textView4);
                f.setText(a.get(1));
                TextView k = (TextView) view.findViewById(R.id.textView5);
                k.setText(a.get(2));
                break;
            default:
                view = inflater.inflate(R.layout.itenerary_travelcab_interactive_item, linearLayout, false);
                TextView z = (TextView) view.findViewById(R.id.textView3);
                z.setText("Take a cab!");
                TextView b = (TextView) view.findViewById(R.id.textView4);
                b.setText(a.get(0));
                TextView m = (TextView) view.findViewById(R.id.textView5);
                m.setText(a.get(1));
                break;
        }

        linearLayout.addView(view);
    }

    public void animationStart(){
        animScreen = currScreen;
        posAnimScreenOutLeft.start();
        opAnimScreenOutLeft.start();
        posAnimScreenInRight.start();
        opAnimScreenInRight.start();
    }

    public void onBackPressed() {
        if (currScreen == 1) super.onBackPressed();
        else if (currScreen == 6){
            animScreen = currScreen;
            nextScreen = 1;
            posAnimScreenOutRight.start();
            opAnimScreenOutRight.start();
            posAnimScreenInLeft.start();
            opAnimScreenInLeft.start();
            currScreen = 1;
        }
        else{
            animScreen = currScreen;
            nextScreen = currScreen - 1;
            posAnimScreenOutRight.start();
            opAnimScreenOutRight.start();
            posAnimScreenInLeft.start();
            opAnimScreenInLeft.start();
            currScreen -= 1;
        }



    }

    public void StartUber(View v){
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.ubercab", PackageManager.GET_ACTIVITIES);
            String uri = "uber://?action=setPickup&pickup=my_location";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(uri));
            startActivity(intent);
        } catch (PackageManager.NameNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ubercab")));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.ubercab")));
            }
        }
    }

    public void generateItinerary(){
        copy2clip = "M Y   I T I N E R A R Y\n\n";
        linearLayout.removeAllViews();

        for (int i = 0; i < resultPnC.getPath().size();i++){
            ArrayList<String> xx = new ArrayList<>();
            xx.add(resultPnC.getPath().get(i).getFrom());
            genElement(xx, 0);
            if(i==0)
                copy2clip = copy2clip + "Start from hotel!\n\n";
            if(i!=0)
                copy2clip = copy2clip + "Destination "+(i)+": "+resultPnC.getPath().get(i).getFrom()+"\n\n";
            Double cost = resultPnC.getPath().get(i).getCost();
            cost = round(cost * 100.00)/100.00;
            if (resultPnC.getPath().get(i).getMode() == TRANSPORTATION.TAXI) {
                xx = new ArrayList<>();
                xx.add(Integer.toString(resultPnC.getPath().get(i).getDuration()));
                xx.add("$"+Double.toString(cost));
                copy2clip = copy2clip+"Take a cab for around "+"$"+Double.toString(cost)+" for "+Integer.toString(resultPnC.getPath().get(i).getDuration())+"mins";
                genElement(xx, 2);
            }
            else {
                xx = new ArrayList<>();
                if (resultPnC.getPath().get(i).getMode() == TRANSPORTATION.BUS) {
                    xx.add("Take Public Transport");
                    copy2clip = copy2clip+"Take public transport for around "+"$"+Double.toString(cost)+" for "+Integer.toString(resultPnC.getPath().get(i).getDuration())+"mins";
                }
                else {
                    xx.add("Take a Walk!");
                    copy2clip = copy2clip+"Walk from here for "+Integer.toString(resultPnC.getPath().get(i).getDuration())+"mins";
                }
                xx.add(Integer.toString(resultPnC.getPath().get(i).getDuration()));
                xx.add("$"+Double.toString(cost));
                genElement(xx,1);
            }
            copy2clip = copy2clip + "\n";

        }

        ArrayList<String> xx = new ArrayList<>();
        xx.add(resultPnC.getPath().get(resultPnC.getPath().size()-1).getTo());
        genElement(xx, 0);
        copy2clip = copy2clip + "Back at your hotel!\n";
    }

    public void Copy2Clip(View v){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("TravelItineraryAppStuff", copy2clip);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getApplicationContext(), "Your itinerary has been copied to the clipboard!", Toast.LENGTH_SHORT).show();
    }

    public static Bitmap textAsBitmap(String text, float textSize, int textColor) {
        Paint paint = new Paint(ANTI_ALIAS_FLAG);
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.0f); // round
        int height = (int) (baseline + paint.descent() + 0.0f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }


}

