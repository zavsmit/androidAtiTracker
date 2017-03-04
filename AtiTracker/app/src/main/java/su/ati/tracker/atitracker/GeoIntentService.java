package su.ati.tracker.atitracker;


import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import su.ati.tracker.atitracker.api.Api;
import su.ati.tracker.atitracker.api.SharedPref;
import su.ati.tracker.atitracker.api.model.Point;
import su.ati.tracker.atitracker.api.model.SetPoint;
import su.ati.tracker.atitracker.api.model.StartRide;

import static su.ati.tracker.atitracker.MainActivity.MONEY_COUNT;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_END_LAT;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_END_LON;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_START_LAT;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_START_LON;
import static su.ati.tracker.atitracker.api.Api.API_URL;

public class GeoIntentService extends Service implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_UPDATE = "su.ati.tracker.atitracker.intentservice.UPDATE";
    public static final String ACTION_DESTROY = "su.ati.tracker.atitracker.intentservice.DESTROY";
    public static final String EXTRA_KEY_UPDATE = "EXTRA_UPDATE";
    public static final String EXTRA_KEY_NEED_PHOTO = "EXTRA_NEED_PHOTO";
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
    private Api.Location mService;
    private String rideId;
    private boolean isStarted;

    private DestroyBroadcast mDestroyBroadcast;

    public class DestroyBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
           onDestroy();
        }
    }


    public GeoIntentService() {
    }

    private OkHttpClient.Builder makeHttpClient() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(interceptor);
        }

        httpClient.interceptors().add(new Interceptor() {
            @Override public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder().header("Content-Type", "application/json");
                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        return httpClient;
    }

    public void onCreate() {
        super.onCreate();

        mDestroyBroadcast = new DestroyBroadcast();

        // Регистрируем второй приёмник
        IntentFilter updateIntentFilter = new IntentFilter(ACTION_DESTROY);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mDestroyBroadcast, updateIntentFilter);

        mLastUpdateTime = "";

        buildGoogleApiClient();
        mGoogleApiClient.connect();
        Intent intent1 = new Intent(this, MainActivity.class);
        intent1.putExtra("sdfsdf", "somefile");
        pIntent = PendingIntent.getActivity(this, 0, intent1, 0);

        initRetrofit();
    }

    private void initRetrofit() {
        OkHttpClient.Builder httpClient = makeHttpClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .client(httpClient.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = retrofit.create(Api.Location.class);
    }

    private void startRide(Intent intent) {

        double startLat = intent.getDoubleExtra(EXTRA_START_LAT, 0);
        double startLon = intent.getDoubleExtra(EXTRA_START_LON, 0);
        double endLat = intent.getDoubleExtra(EXTRA_END_LAT, 0);
        double endLon = intent.getDoubleExtra(EXTRA_END_LON, 0);

        rideId = String.valueOf(startLat + startLon + endLat + endLon);
        SharedPref.saveRideId(rideId, getApplicationContext());
        long time = Calendar.getInstance().getTimeInMillis();

        StartRide startRide = new StartRide();
        startRide.setMoney(MONEY_COUNT);
        if (mCurrentLocation != null) {
            startRide.setLat(mCurrentLocation.getLatitude());
            startRide.setLon(mCurrentLocation.getLongitude());
        }
        startRide.setTime(time);

        startRide.setStartPoint(new Point(startLat, startLon, time));
        startRide.setEndPoint(new Point(endLat, endLon, time));


        Call<ResponseBody> call = mService.startRide(rideId, startRide);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                isStarted = true;
            }

            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    private void setPoint(List<Point> points) {
        Call<SetPoint> call = mService.setPoint(rideId, points);
        call.enqueue(new Callback<SetPoint>() {
            @Override public void onResponse(Call<SetPoint> call, retrofit2.Response<SetPoint> response) {
                int percent = response.body().getPercent();
                boolean needPhoto = response.body().isNeedsPhoto();
                sendUpdateProgress(percent, needPhoto);
                showNotif(percent);

                SharedPref.savePointParams(mCurrentLocation, percent, needPhoto, getApplicationContext());
            }

            @Override public void onFailure(Call<SetPoint> call, Throwable t) {
                Log.d("sdf", "sdf");
            }
        });
    }

    private void endRide() {
        Call<SetPoint> call = mService.endRide(rideId, makePoint(mCurrentLocation));
        call.enqueue(new Callback<SetPoint>() {
            @Override public void onResponse(Call<SetPoint> call, retrofit2.Response<SetPoint> response) {
                isStarted = false;
            }

            @Override public void onFailure(Call<SetPoint> call, Throwable t) {
                Log.d("sdf", "sdf");
            }
        });
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
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
        mGoogleApiClient.disconnect();
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

        if (mDestroyBroadcast != null) {
            if (mDestroyBroadcast.isOrderedBroadcast())
                unregisterReceiver(mDestroyBroadcast);
        }

        String notice;
        stopLocationUpdates();
        endRide();
        notice = "Маршрут завершен";
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

        startRide(intent);
        showNotif(0);
        startForeground(998, notification);

        return START_REDELIVER_INTENT;
    }

    private void showNotif(int i) {
        String notificationText = String.valueOf((MONEY_COUNT * i / 100)) + " \u20BD, " + i + "%";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Маршрут отслеживается")
                    .setContentText(notificationText)
                    .setTicker("Notification!")
                    .setContentIntent(pIntent)
                    .setWhen(System.currentTimeMillis())
                    .setAutoCancel(true).setSmallIcon(R.mipmap.ic_launcher)
                    .getNotification();
        } else {
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Маршрут отслеживается")
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

    private void sendUpdateProgress(int i, boolean isNeedPhoto) {
        Intent updateIntent = new Intent();
        updateIntent.setAction(ACTION_UPDATE);
        updateIntent.addCategory(Intent.CATEGORY_DEFAULT);
        updateIntent.putExtra(EXTRA_KEY_UPDATE, i);
        updateIntent.putExtra(EXTRA_KEY_NEED_PHOTO, isNeedPhoto);
        sendBroadcast(updateIntent);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        startLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Log.d("sdf", String.valueOf(mLastLocation.getLatitude()));
        }
    }

    @Override public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (isStarted) {
            List<Point> points = new ArrayList<>();
            points.add(makePoint(location));
            setPoint(points);
        }
    }

    private Point makePoint(Location location) {
        Point point = new Point();
        point.setTime(Calendar.getInstance().getTimeInMillis());
        point.setSpeed(location.getSpeed());
        point.setLat(location.getLatitude());
        point.setLon(location.getLongitude());
        return point;
    }

}
