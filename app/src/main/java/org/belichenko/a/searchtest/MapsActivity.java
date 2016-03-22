package org.belichenko.a.searchtest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;

import org.belichenko.a.searchtest.data_structure.PointsData;
import org.belichenko.a.searchtest.data_structure.Results;
import org.belichenko.a.searchtest.data_structure.googleNearbyPlaces;
import org.belichenko.a.searchtest.map_points.Legs;
import org.belichenko.a.searchtest.map_points.RootClass;
import org.belichenko.a.searchtest.map_points.Steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
        , GoogleApiClient.ConnectionCallbacks
        , GoogleApiClient.OnConnectionFailedListener
        , GoogleMap.OnMarkerClickListener
        , LocationListener
        , Constant {

    private static final String TAG = "Main activity";
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Marker currentMarker;
    private ArrayList<Results> searchResult = new ArrayList<>();
    private AutoCompleteAdapter adapter;
    private ArrayList<Marker> markers = new ArrayList<>();
    private ArrayList<Legs> routes = new ArrayList<>();
    private ArrayList<Polyline> polylineArrayList = new ArrayList<>();
    private boolean isSearchView = false;
    private boolean isBottomPanelVisible = false;
    private boolean showsTextPanel = false;
    private RetrofitListener retrofitListener = new RetrofitListener();
    private RetrofitRouteListener retrofitRouteListener = new RetrofitRouteListener();

    @Bind(R.id.search_bt)
    ImageView mSearchBt;
    @Bind(R.id.menu_bt)
    ImageView mMenuBt;
    @Bind(R.id.map_earth)
    ImageView mEarthBt;
    @Bind(R.id.map_position)
    ImageView mPositionBt;
    @Bind(R.id.map_direction)
    ImageView mDirectionBt;

    @Bind(R.id.textDistance)
    TextView mTextDistance;
    @Bind(R.id.textDuration)
    TextView mTextDuration;

    @Bind(R.id.car)
    RelativeLayout mCarBt;
    @Bind(R.id.bicycle)
    RelativeLayout mBicycleBt;
    @Bind(R.id.walk)
    RelativeLayout mWalkBt;

    @Bind(R.id.navigation_panel)
    LinearLayout mNavigationPanel;
    @Bind(R.id.bottom_panel)
    LinearLayout mBottomPanel;
    @Bind(R.id.bottom_text_panel)
    LinearLayout mBottomTextPanel;

    @Bind(R.id.autoCompleteSearchView)
    AutoCompleteTextView searshResultsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        ButterKnife.setDebug(true);
        ButterKnife.bind(this);
        mapFragment.getMapAsync(this);
        checkLocationServiceEnabled();
        buildGoogleApiClient();
        createLocationRequest();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        mBottomPanel.setVisibility(View.GONE);
        mNavigationPanel.setVisibility(View.GONE);
        mBottomTextPanel.setVisibility(View.GONE);

        adapter = new AutoCompleteAdapter(this
                , R.layout.simple_dropdown_item_2line
                , searchResult);
        searshResultsView.setAdapter(adapter);

        searshResultsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Results result = (Results) parent.getItemAtPosition(position);
                currentMarker = null;
                setPinOnMap(result);
                returnToMapView();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        restoreSrttings();
        onSearchBtClick();
    }

    private void buildGoogleApiClient() {
        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void createLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(TWENTY_SECONDS);
        mLocationRequest.setFastestInterval(TEN_SECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    protected void onPause() {
        super.onPause();
        storeSettings();
        stopLocationUpdates();
    }

    private void storeSettings() {
        SharedPreferences mPrefs = getSharedPreferences(STORAGE_OF_SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = mPrefs.edit();
        Gson gson = new Gson();
        if (currentLocation != null) {
            edit.putString(CURRENT_LOCATION, gson.toJson(currentLocation));
        }
        if (mMap != null) {
            CameraPosition cp = mMap.getCameraPosition();
            edit.putString(CURRENT_CAMERA, gson.toJson(cp));
        }
        if (searchResult != null) {
            edit.putString(PLACE_LIST, gson.toJson(searchResult));
        }
        edit.apply();
    }

    private void restoreSrttings() {
        SharedPreferences sharedPref = getSharedPreferences(STORAGE_OF_SETTINGS, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        CameraPosition cp = null;

        if (sharedPref.contains(CURRENT_LOCATION)) {
            String jsonLoc = sharedPref.getString(CURRENT_LOCATION, null);
            currentLocation = gson.fromJson(jsonLoc, Location.class);
        }
        if (sharedPref.contains(CURRENT_CAMERA)) {
            String jsonCam = sharedPref.getString(CURRENT_CAMERA, null);
            cp = gson.fromJson(jsonCam, CameraPosition.class);
        }
        if (sharedPref.contains(PLACE_LIST)) {
            String jsonMsg = sharedPref.getString(PLACE_LIST, null);
            Results[] tempList = gson.fromJson(jsonMsg, Results[].class);
            searchResult.clear();
            searchResult.addAll(Arrays.asList(tempList));
        }
        if (currentLocation != null) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        } else {
            Log.d(TAG, "onResume() mGoogleApiClient not connected");
        }
    }

    @OnClick(R.id.menu_bt)
    protected void onMenuBtClick() {
        if (isSearchView) {
            returnToMapView();
        } else {
            if (mNavigationPanel.getVisibility() == View.GONE) {
                Animation animBt = AnimationUtils.loadAnimation(this, R.anim.rotate_rght);
                Animation animPanel = AnimationUtils.loadAnimation(this, R.anim.from_right_anim);
                mMenuBt.startAnimation(animBt);
                mNavigationPanel.startAnimation(animPanel);
                mNavigationPanel.setVisibility(View.VISIBLE);
            } else if (mNavigationPanel.getVisibility() == View.VISIBLE) {
                Animation animBt = AnimationUtils.loadAnimation(this, R.anim.rotate_left);
                Animation animPanel = AnimationUtils.loadAnimation(this, R.anim.go_right_anim);
                mMenuBt.startAnimation(animBt);
                mNavigationPanel.startAnimation(animPanel);
                mNavigationPanel.setVisibility(View.VISIBLE);
                mNavigationPanel.setVisibility(View.GONE);
            }
        }
    }

    @OnClick(R.id.search_bt)
    protected void onSearchBtClick() {
        if (mMap == null) {
            Log.d(TAG, "onSearchBtClick() called with: " + "mMap == null");
            return;
        }
        returnToMapView();
        if (searchResult == null) {
            Log.d(TAG, "onSearchBtClick() called with: " + "null ArrayList");
            return;
        }
        mBottomTextPanel.setVisibility(View.GONE);
        showHideBottomPanel(View.GONE);
        currentMarker = null;
        for (Polyline polyline : polylineArrayList) {
            polyline.remove();
        }
        polylineArrayList.clear();
        currentMarker = null;
        markers.clear();
        mMap.clear();
        Results result = null;
        for (int i = 0; (i < searchResult.size() && i < 20); i++) {
            result = searchResult.get(i);
            if (result == null) {
                continue;
            }
            Marker currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(result.getPosition())
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_24dp))
                    .draggable(false)
                    .title(result.name)
                    .snippet(result.vicinity));
            markers.add(currentMarker);
        }
        if (result != null) {
            //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(result.getPosition(), 14));
        }
    }

    @OnClick(R.id.map_earth)
    protected void onEarthBtClick() {
        if (mMap == null) {
            Log.d(TAG, "onEarthBtClick() called with: " + "map == null");
            return;
        }
        if (mMap.getMapType() == GoogleMap.MAP_TYPE_SATELLITE) {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
    }

    @OnClick(R.id.map_position)
    protected void onPositionBtClick() {
        if (currentLocation == null) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_current_location)
                    , Toast.LENGTH_LONG).show();
            return;
        }
        if (mMap == null) {
            Log.d(TAG, "onPositionBtClick() called with: " + "map == null");
            return;
        }

        //move to current position
        if (ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_permission)
                    , Toast.LENGTH_LONG).show();
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory
                .newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 15));
    }

    @OnClick(R.id.map_direction)
    protected void onDirectionBtClick() {
        if (mMap == null) {
            Log.d(TAG, "onDirectionBtClick() called with: " + "map == null");
            return;
        }
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mMap.getCameraPosition().target)
                .zoom(mMap.getCameraPosition().zoom)
                .bearing(0)
                .tilt(0)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @OnClick(R.id.car)
    protected void onCarBtClick() {
        reDrawRoute("driving");
    }

    @OnClick(R.id.bicycle)
    protected void onBicycleBtClick() {
        reDrawRoute("bicycling");
    }

    @OnClick(R.id.walk)
    protected void onWalkBtClick() {
        reDrawRoute("walking");
    }

    private void reDrawRoute(String mode) {
        if (currentMarker == null) {
            Toast.makeText(this, getString(R.string.marker_gone), Toast.LENGTH_SHORT).show();
        } else {
            for (Polyline polyline : polylineArrayList) {
                polyline.remove();
            }
            polylineArrayList.clear();
            makeRoute(currentMarker, mode);
        }
        showsTextPanel = true;
        showHideBottomPanel(View.GONE);
    }

    protected void stopLocationUpdates() {

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            Log.d(TAG, "stopLocationUpdates()");
        } else {
            Log.d(TAG, "stopLocationUpdates() mGoogleApiClient not connected");
        }

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_permission)
                    , Toast.LENGTH_LONG).show();
            Log.d(TAG, "onConnected() called with: " + "not permissions");
            return;
        }
        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
        onPositionBtClick();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this
                , Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_permission)
                    , Toast.LENGTH_LONG).show();
            Log.d(TAG, "startLocationUpdates() called with: " + "not permissions");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient,
                mLocationRequest,
                this
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                Log.d(TAG, "startLocationUpdates() called with: " + "status = [" + status + "]");
            }
        });

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called with: " + "i = [" + i + "]");
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called with: " + "connectionResult = [" + connectionResult + "]");
    }

    private void checkLocationServiceEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean geolocationEnabled = false;
        try {
            geolocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "checkLocationServiceEnabled() called with Exception: " + ex.toString());
        }
        if (!geolocationEnabled) {
            buildAlertMessageNoLocationService();
        }
    }

    private void setPinOnMap(Results result) {
        mMap.clear();
        if (result == null) {
            Log.d(TAG, "setPinOnMap() called with: " + "result = [" + null + "]");
            return;
        }
        markers.clear();
        Marker currentMarker = mMap.addMarker(new MarkerOptions()
                .position(result.getPosition())
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_24dp))
                .flat(true)
                .draggable(false)
                .title(result.name)
                .snippet(result.vicinity));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(result.getPosition(), 15));
        markers.add(currentMarker);
    }

    @OnTextChanged(R.id.autoCompleteSearchView)
    protected void onTextChange(CharSequence s, int start, int before, int count) {
        isSearchView = true;
        mMenuBt.setImageResource(R.drawable.ic_clear_black_24dp);
        if (s.length() > 2) {
            updatePlaces(s.toString());
        }
    }

    private void returnToMapView() {
        searshResultsView.setText("");
        isSearchView = false;
        // hide keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        mMenuBt.setImageResource(R.drawable.ic_play_arrow_black_24dp);
    }

    protected void updatePlaces(String keyword) {
        if (currentLocation == null) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_current_location)
                    , Toast.LENGTH_LONG).show();
            return;
        }
        LinkedHashMap<String, String> filter = new LinkedHashMap<>();
        filter.put("location", String.valueOf(currentLocation.getLatitude()) + ","
                + String.valueOf(currentLocation.getLongitude()));
        filter.put("rankby", "distance");
        filter.put("language", "ru");
        filter.put("keyword", keyword);
        filter.put("key", getString(R.string.google_maps_web_key));

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // prepare call in Retrofit 2.0
        googleNearbyPlaces nearbyPlaces = retrofit.create(googleNearbyPlaces.class);

        Call<PointsData> call = nearbyPlaces.getPlacesData(filter);
        //asynchronous call
        call.enqueue(retrofitListener);
    }

    protected void makeRoute(Marker endPoint, String mode) {
        if (endPoint == null) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.marker_gone)
                    , Toast.LENGTH_LONG).show();
            Log.d(TAG, "makeRoute() called with: " + "endPoint == null");
            return;
        }
        if (currentLocation == null) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.dosnt_current_location)
                    , Toast.LENGTH_LONG).show();
            Log.d(TAG, "makeRoute() called with: " + "currentLocation == null");
            return;
        }

        LinkedHashMap<String, String> filter = new LinkedHashMap<>();
        filter.put("origin", String.valueOf(currentLocation.getLatitude()) + ","
                + String.valueOf(currentLocation.getLongitude()));
        filter.put("destination", String.valueOf(endPoint.getPosition().latitude) + ","
                + String.valueOf(endPoint.getPosition().longitude));
        filter.put("language", "ru");
        filter.put("mode", mode);
        filter.put("key", getString(R.string.google_maps_web_key));

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        // prepare call in Retrofit 2.0
        googleNearbyPlaces nearbyPlaces = retrofit.create(googleNearbyPlaces.class);

        Call<RootClass> call = nearbyPlaces.getRoute(filter);
        //asynchronous call
        call.enqueue(retrofitRouteListener);
    }

    private void buildAlertMessageNoLocationService() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setMessage(getString(R.string.provider))
                .setPositiveButton(getString(R.string.msg_turn_on), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog
                            , @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mBottomTextPanel.setVisibility(View.GONE);
        currentMarker = marker;
        for (Polyline polyline : polylineArrayList) {
            polyline.remove();
        }
        polylineArrayList.clear();
        for (Marker oldMarker : markers) {
            oldMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_24dp));
        }
        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_pin_black_24dp));
        makeRoute(marker, "driving");
        showHideBottomPanel(View.VISIBLE);
        return false;
    }

    private void showHideBottomPanel(int visibiliti) {
        mBottomPanel.setVisibility(visibiliti);
        if (visibiliti == View.VISIBLE) {
            isBottomPanelVisible = true;
        } else {
            isBottomPanelVisible = false;
        }
    }

    private class RetrofitListener implements Callback<PointsData> {

        @Override
        public void onResponse(Call<PointsData> call, Response<PointsData> response) {
            if (response.body() != null) {
                if (response.body().status.equals("OK")) {
                    if (response.body().results != null) {
                        searchResult.clear();
                        searchResult.addAll(response.body().results);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    searchResult.clear();
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onFailure(Call<PointsData> call, Throwable t) {
            Toast.makeText(MapsActivity.this
                    , getString(R.string.internet_problem)
                    , Toast.LENGTH_LONG).show();
        }
    }

    private class RetrofitRouteListener implements Callback<RootClass> {

        @Override
        public void onResponse(Call<RootClass> call, Response<RootClass> response) {
            if (response.body() != null) {
                if (response.body().status.equals("OK")) {
                    if (response.body().routes != null && response.body().routes.size() > 0) {
                        routes.clear();
                        routes.addAll(response.body().routes.get(0).legs);
                        showRouteDetails(response.body().routes.get(0).legs.get(0).distance.text
                                , response.body().routes.get(0).legs.get(0).duration.text);
                    }
                } else {
                    routes.clear();
                    Toast.makeText(MapsActivity.this
                            , getString(R.string.dont_rout)
                            , Toast.LENGTH_SHORT).show();
                }
            }
            if (routes.size() > 0) {
                drawRoute();
            }
        }

        @Override
        public void onFailure(Call<RootClass> call, Throwable t) {
            Log.d(TAG, "RetrofitRouteListener called with: "
                    + "call = [" + call + "], t = [" + t + "]");
            Toast.makeText(MapsActivity.this
                    , getString(R.string.internet_problem)
                    , Toast.LENGTH_LONG).show();
        }

    }

    private void showRouteDetails(String distance, String duration) {
        if (showsTextPanel) {
            mTextDistance.setText(getText(R.string.distance_text) + " " + distance);
            mTextDuration.setText(getText(R.string.duration_text) + " " + duration);
            mBottomTextPanel.setVisibility(View.VISIBLE);
            showsTextPanel = false;
        }
    }

    private void drawRoute() {
        if (mMap == null) {
            Log.d(TAG, "drawRoute() called with: " + "null map");
            return;
        }
        if (routes == null || routes.size() < 1) {
            Toast.makeText(this, getString(R.string.dont_rout), Toast.LENGTH_SHORT).show();
            return;
        }
        for (Polyline polyline : polylineArrayList) {
            polyline.remove();
        }
        polylineArrayList.clear();
        ArrayList<LatLng> points = new ArrayList<>();
        PolylineOptions polyLineOptions = new PolylineOptions();
        points.add(new LatLng(routes.get(0).start_location.lat, routes.get(0).start_location.lng));
        for (Steps step : routes.get(0).steps) {
            points.addAll(PolyUtil.decode(step.polyline.points));
        }
        points.add(new LatLng(routes.get(0).end_location.lat, routes.get(0).end_location.lng));
        polyLineOptions.addAll(points);
        polyLineOptions.width(2);
        polyLineOptions.color(Color.BLUE);
        Polyline newPolyline = mMap.addPolyline(polyLineOptions);
        polylineArrayList.add(newPolyline);
    }
}