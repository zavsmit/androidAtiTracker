package su.ati.tracker.atitracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.android.gms.maps.model.LatLng;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import su.ati.tracker.atitracker.api.Api;
import su.ati.tracker.atitracker.api.SharedPref;
import su.ati.tracker.atitracker.api.model.SendPhoto;

import static su.ati.tracker.atitracker.GeoIntentService.ACTION_DESTROY;
import static su.ati.tracker.atitracker.GeoIntentService.ACTION_UPDATE;
import static su.ati.tracker.atitracker.GeoIntentService.EXTRA_KEY_NEED_PHOTO;
import static su.ati.tracker.atitracker.GeoIntentService.EXTRA_KEY_UPDATE;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_END_LAT;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_END_LON;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_START_LAT;
import static su.ati.tracker.atitracker.MapsActivity.EXTRA_START_LON;
import static su.ati.tracker.atitracker.api.Api.API_URL;
import static su.ati.tracker.atitracker.api.SharedPref.getRideId;

public class MainActivity extends AppCompatActivity {

    public final static int MONEY_COUNT = 500;
    private final static int PERMISSION_PHOTO_CODE = 99;
    private final static int PERMISSION_GEO_CODE = 199;

    private final static int MENU_GEO = 0;
    private final static int MENU_START = 1;
    private final static int MENU_END = 2;
    private final static int TAKE_PHOTO_CODE = 55;
    private final static int MAP_CODE = 155;
    private int lastUpdate;
    private AppCompatTextView price;
    private ProgressBar progressBar;
    private LinearLayout llPhoto;
    private boolean isStarted;
    private Menu mMenu;
    private UpdateBroadcastReceiver mUpdateBroadcastReceiver;
    private Intent mMyServiceIntent;
    private LatLng lngStart;
    private LatLng lngEnd;
    private Api.Checked mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        AppCompatTextView takePhoto = (AppCompatTextView) findViewById(R.id.tv_take_photo);
        AppCompatImageView send = (AppCompatImageView) findViewById(R.id.iv_send);
        AppCompatImageView call = (AppCompatImageView) findViewById(R.id.iv_call);
        price = (AppCompatTextView) findViewById(R.id.tv_price);
        //        AppCompatTextView descriptionToolbar = (AppCompatTextView) findViewById(R.id.tv_descriptionToolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        llPhoto = (LinearLayout) findViewById(R.id.ll_photo);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name));

        takePhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(intent, TAKE_PHOTO_CODE);

                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.CAMERA)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, PERMISSION_PHOTO_CODE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, PERMISSION_PHOTO_CODE);
                    }
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String uriMail = "mailto:";
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse(uriMail + "google@gmail.com"));
                startActivity(emailIntent);
            }
        });
        call.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                String uriPhone = "tel:";
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(uriPhone + "12345678"));
                startActivity(intent);
            }
        });

        initRetrofit();


        Button b = (Button) findViewById(R.id.b_copy);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {

                String ddd = SharedPref.getRideId(MainActivity.this);

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", ddd);
                clipboard.setPrimaryClip(clip);
            }
        });


        setUpdate(SharedPref.getProgress(this));
        showOrHidePhoto(SharedPref.isNeedPhoto(this));
    }

    private void sendPhoto(String photo) {

        String rideId = getRideId(this);
        if (rideId.isEmpty()) {
            return;
        }

        LatLng lastLocation = SharedPref.getLastLatLng(this);

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setTime(Calendar.getInstance().getTimeInMillis());
        if (lastLocation != null) {
            sendPhoto.setLon(lastLocation.longitude);
            sendPhoto.setLat(lastLocation.latitude);
        }

        sendPhoto.setPhoto(photo);

        Call<ResponseBody> call = mService.sendPhoto(rideId, sendPhoto);
        call.enqueue(new Callback<ResponseBody>() {
            @Override public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                llPhoto.setVisibility(View.GONE);
                SharedPref.setNeedPhoto(false, MainActivity.this);
            }

            @Override public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }


    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = retrofit.create(Api.Checked.class);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_PHOTO_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(intent, TAKE_PHOTO_CODE);
                }
                return;
            }
            case PERMISSION_GEO_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (lngStart == null) {
                        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                        startActivityForResult(intent, MAP_CODE);
                    } else {
                        startRoute();
                    }
                }
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == TAKE_PHOTO_CODE) {
                try {
                    Bitmap bp = (Bitmap) data.getExtras().get("data");
                    bp = scaleDown(bp, 500, true);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bp.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                    sendPhoto(encoded);
                } catch (Exception e) {
                    Log.e("Camera", e.toString());
                }
            }
            if (requestCode == MAP_CODE) {
                double startLat = data.getDoubleExtra(EXTRA_START_LAT, 0);
                double startLon = data.getDoubleExtra(EXTRA_START_LON, 0);
                lngStart = new LatLng(startLat, startLon);
                double endLat = data.getDoubleExtra(EXTRA_END_LAT, 0);
                double endLon = data.getDoubleExtra(EXTRA_END_LON, 0);
                lngEnd = new LatLng(endLat, endLon);

                changeItemMenu(MENU_START);
                price.setText(0 + " \u20BD");
                lastUpdate = 0;
                progressBar.setProgress(0);
            }
        }
    }

    public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                   boolean filter) {

        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, realImage.getWidth(), realImage.getHeight()), new RectF(0, 0, maxImageSize, maxImageSize), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(realImage, 0, 0, realImage.getWidth(), realImage.getHeight(), m, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateBroadcastReceiver != null) {
            if (mUpdateBroadcastReceiver.isOrderedBroadcast())
                unregisterReceiver(mUpdateBroadcastReceiver);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_action_start:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    startRoute();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GEO_CODE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GEO_CODE);
                    }
                }
                break;
            case R.id.menu_action_end:
                endRoute();
                break;
            case R.id.menu_action_geo:
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    startActivityForResult(intent, MAP_CODE);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GEO_CODE);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_GEO_CODE);
                    }
                }
                break;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);

        if (getRideId(this).isEmpty()) {
            changeItemMenu(MENU_GEO);
        } else {
            if (isStarted) {
                changeItemMenu(MENU_START);
            } else {
                changeItemMenu(MENU_END);
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    public void changeItemMenu(int showType) {
        if (mMenu != null) {
            switch (showType) {
                case MENU_GEO:
                    mMenu.findItem(R.id.menu_action_geo).setVisible(true);
                    mMenu.findItem(R.id.menu_action_start).setVisible(false);
                    mMenu.findItem(R.id.menu_action_end).setVisible(false);
                    break;
                case MENU_START:
                    mMenu.findItem(R.id.menu_action_geo).setVisible(false);
                    mMenu.findItem(R.id.menu_action_start).setVisible(true);
                    mMenu.findItem(R.id.menu_action_end).setVisible(false);
                    break;
                case MENU_END:
                    mMenu.findItem(R.id.menu_action_geo).setVisible(false);
                    mMenu.findItem(R.id.menu_action_start).setVisible(false);
                    mMenu.findItem(R.id.menu_action_end).setVisible(true);
                    break;
            }
        }
    }

    private void endRoute() {
//        destroyServise();
        lngStart = null;
        lngEnd = null;
        SharedPref.clear(this);
        llPhoto.setVisibility(View.GONE);
        changeItemMenu(MENU_GEO);
//
        if (mMyServiceIntent != null) {
            stopService(mMyServiceIntent);
            mMyServiceIntent = null;
        } else {
            destroyServise();
        }


        if (mUpdateBroadcastReceiver != null) {
            if (mUpdateBroadcastReceiver.isOrderedBroadcast())
                unregisterReceiver(mUpdateBroadcastReceiver);
        }
        isStarted = false;
    }

    private void startRoute() {
        mMyServiceIntent = new Intent(MainActivity.this, GeoIntentService.class);
        mMyServiceIntent.putExtra(EXTRA_START_LAT, lngStart.latitude);
        mMyServiceIntent.putExtra(EXTRA_START_LON, lngStart.longitude);
        mMyServiceIntent.putExtra(EXTRA_END_LAT, lngEnd.latitude);
        mMyServiceIntent.putExtra(EXTRA_END_LON, lngEnd.longitude);
        startService(mMyServiceIntent);

        mUpdateBroadcastReceiver = new UpdateBroadcastReceiver();

        // Регистрируем второй приёмник
        IntentFilter updateIntentFilter = new IntentFilter(ACTION_UPDATE);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mUpdateBroadcastReceiver, updateIntentFilter);

        isStarted = true;
        changeItemMenu(MENU_END);
    }

    private void setUpdate(int update) {
        if (lastUpdate < update) {
            lastUpdate = update;
            progressBar.setProgress(update);
            int rub = MONEY_COUNT * update / 100;
            price.setText(rub + " \u20BD");
        }
    }

    private void showOrHidePhoto(boolean isNeedPhoto) {
        if (isNeedPhoto) {
            llPhoto.setVisibility(View.VISIBLE);
        } else {
            llPhoto.setVisibility(View.GONE);
        }
    }

    public class UpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(EXTRA_KEY_UPDATE, 0);
            boolean isNeedPhoto = intent.getBooleanExtra(EXTRA_KEY_NEED_PHOTO, false);

            setUpdate(update);

            showOrHidePhoto(isNeedPhoto);
        }
    }

    private void destroyServise() {
        Intent updateIntent = new Intent();
        updateIntent.setAction(ACTION_DESTROY);
        updateIntent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(updateIntent);
    }
}
