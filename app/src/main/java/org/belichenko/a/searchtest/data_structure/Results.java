package org.belichenko.a.searchtest.data_structure;


import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class Results {
    public Geometry geometry;
    public String icon;
    public String id;
    public String name;
    public ArrayList<Photos> photos;
    public String place_id;
    public float rating;
    public String reference;
    public String scope;
    public ArrayList<String> types;
    public String vicinity;

    public Results(String status) {
        this.name = status;
    }
    public LatLng getPosition(){
        return new LatLng(geometry.location.lat, geometry.location.lng);
    }
}
