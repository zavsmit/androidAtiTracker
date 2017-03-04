package su.ati.tracker.atitracker;


import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import su.ati.tracker.atitracker.api.Api;

import static su.ati.tracker.atitracker.api.Api.API_URL;

public class GeoIntentService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_UPDATE = "su.ati.tracker.atitracker.intentservice.UPDATE";
    public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    private static final int NOTIFICATION_ID = 1;
    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;
    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;

    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;
    private PendingIntent pIntent;
    private Notification notification;
    private boolean mIsSuccess;
    private boolean mIsStopped;


    public GeoIntentService() {
        mIsSuccess = false;
        mIsStopped = false;
    }

    public void onCreate() {
        super.onCreate();

        mLastUpdateTime = "";

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        Intent intent1 = new Intent(this, MainActivity.class);
        intent1.putExtra("sdfsdf", "somefile");
        pIntent = PendingIntent.getActivity(this, 0, intent1, 0);





        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Api.AtiTracker service = retrofit.create(Api.AtiTracker.class);


//        Call<List<Api.Contributor>> call = service.contributors();
//        call.enqueue(new Callback<List<Api.Contributor>>() {
//            @Override
//            public void onResponse(Response<List<Api.Contributor>> response) {
//                // Get result Repo from response.body()
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//
//            }
//        });

    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }


    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        //        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, GeoIntentService.this);
    }


    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    public void onDestroy() {
        String notice;
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        mGoogleApiClient.disconnect();

        mIsStopped = true;

        if (mIsSuccess) {
            notice = "onDestroy with success";

        } else {
            notice = "onDestroy WITHOUT success!";
        }

        Toast.makeText(getApplicationContext(), notice, Toast.LENGTH_LONG).show();
        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
        someTask();
        return START_REDELIVER_INTENT;
    }

    void someTask() {

        new Thread(new Runnable() {
            public void run() {
                for (int i = 0; i <= 10; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (mIsStopped) {
                        break;
                    }

                    // посылаем промежуточные данные
                    Intent updateIntent = new Intent();
                    updateIntent.setAction(ACTION_UPDATE);
                    updateIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    updateIntent.putExtra(EXTRA_KEY_UPDATE, i);
                    sendBroadcast(updateIntent);

                    mIsSuccess = true;

                    // формируем уведомление
                    String notificationText = String.valueOf((100 * i / 10))
                            + " %";

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        notification = new Notification.Builder(getApplicationContext())
                                .setContentTitle("Progress")
                                .setContentText(notificationText)
                                .setTicker("Notification!")
                                .setContentIntent(pIntent)
                                .setWhen(System.currentTimeMillis())
                                .setAutoCancel(true).setSmallIcon(R.mipmap.ic_launcher)
                                .getNotification();
                    } else {
                        notification = new Notification.Builder(getApplicationContext())
                                .setContentTitle("Progress")
                                .setContentText(notificationText)
                                .setTicker("Notification!")
                                .setContentIntent(pIntent)
                                .setAutoCancel(false)
                                .setWhen(System.currentTimeMillis())
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .build();
                    }
                    startForeground(998, notification);

                }
                //                stopForeground(true);
            }
        }).start();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Log.d("sdf", "");
        }

        startLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("sdf", String.valueOf(mLastLocation.getLatitude()));
        }
    }

    @Override public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }
}
