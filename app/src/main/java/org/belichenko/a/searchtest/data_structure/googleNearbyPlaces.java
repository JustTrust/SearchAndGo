package org.belichenko.a.searchtest.data_structure;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

/**
 * Get nearby places
 */
public interface googleNearbyPlaces {
    @GET("/maps/api/place/nearbysearch/json")
    Call<PointsData> getPlacesData(@QueryMap Map<String,String> filters);

}
