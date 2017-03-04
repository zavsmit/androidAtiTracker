package su.ati.tracker.atitracker.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Zavsmit on 04.03.2017.
 */

public class SharedPref {
    private static final String PREF_FILE_NAME = "android_ati_track_pref_file";


    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public static void clear(Context context) {
        getPref(context).edit().clear().apply();
    }


    public static void savePointParams(Location location, int progress, boolean isNeedPhoto, Context context) {
        SharedPreferences.Editor ed = getPref(context).edit();
        ed.putString("lat", String.valueOf(location.getLatitude()));
        ed.putString("lon", String.valueOf(location.getLongitude()));
        ed.putInt("progress", progress);
        ed.putBoolean("isNeedPhoto", isNeedPhoto);
        ed.apply();
    }

    public static LatLng getLastLatLng(Context context) {
        double lat = Double.valueOf(getPref(context).getString("lat", "0"));
        double lon = Double.valueOf(getPref(context).getString("lon", "0"));

        if (lat == 0 && lon == 0) {
            return null;
        }
        return new LatLng(lat, lon);
    }

    public static int getProgress(Context context) {
        return getPref(context).getInt("progress", 0);
    }


    public static void setNeedPhoto(boolean isNeed, Context context) {
        SharedPreferences.Editor ed = getPref(context).edit();
        ed.putBoolean("isNeedPhoto", isNeed);
        ed.apply();
    }

    public static boolean isNeedPhoto(Context context) {
        return getPref(context).getBoolean("isNeedPhoto", false);
    }


    public static void saveRideId(String id, Context context) {
        SharedPreferences.Editor ed = getPref(context).edit();
        ed.putString("rideId", id);
        ed.apply();
    }

    public static String getRideId(Context context) {
        return getPref(context).getString("rideId", "");
    }

}
