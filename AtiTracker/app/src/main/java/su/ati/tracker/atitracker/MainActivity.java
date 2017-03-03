package su.ati.tracker.atitracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;

import static su.ati.tracker.atitracker.GeoIntentService.ACTION_UPDATE;
import static su.ati.tracker.atitracker.GeoIntentService.EXTRA_KEY_OUT;
import static su.ati.tracker.atitracker.GeoIntentService.EXTRA_KEY_UPDATE;

public class MainActivity extends AppCompatActivity {

    public static int count = 0;
    int TAKE_PHOTO_CODE = 0;
    private AppCompatButton bStartStop;
    private AppCompatButton bPhoto;
    private AppCompatTextView status;
    private ProgressBar progressBar;
    private boolean isStarted;
    private UpdateBroadcastReceiver mUpdateBroadcastReceiver;
    private Intent mMyServiceIntent;
    private int mNumberOfIntentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bStartStop = (AppCompatButton) findViewById(R.id.b_startStop);
        bPhoto = (AppCompatButton) findViewById(R.id.b_photo);
        status = (AppCompatTextView) findViewById(R.id.tv_status);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);


        mNumberOfIntentService = 0;
        bStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStarted) {

                    mNumberOfIntentService++;
                    bStartStop.setText("Начать маршрут");

                    if (mMyServiceIntent != null) {
                        stopService(mMyServiceIntent);
                        mMyServiceIntent = null;
                    }

                    unregisterReceiver(mUpdateBroadcastReceiver);
                    isStarted = false;


                } else {


                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                            == PackageManager.PERMISSION_GRANTED) {

                        bStartStop.setText("Закончить маршрут");

                        // Запускаем свой IntentService
                        mMyServiceIntent = new Intent(MainActivity.this, GeoIntentService.class);

                        startService(mMyServiceIntent);

                        mUpdateBroadcastReceiver = new UpdateBroadcastReceiver();

                        // Регистрируем второй приёмник
                        IntentFilter updateIntentFilter = new IntentFilter(ACTION_UPDATE);
                        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                        registerReceiver(mUpdateBroadcastReceiver, updateIntentFilter);

                        isStarted = true;
                    } else {
                        // Should we show an explanation?
                        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION)) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    199);

                            // Show an explanation to the user *asynchronously* -- don't block
                            // this thread waiting for the user's response! After the user
                            // sees the explanation, try again to request the permission.

                        } else {

                            // No explanation needed, we can request the permission.

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    199);

                            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                            // app-defined int constant. The callback method gets the
                            // result of the request.
                        }
                    }


                }
            }
        });

        bPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(intent, TAKE_PHOTO_CODE);

                } else {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.CAMERA)) {

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                99);

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                99);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }


            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 99: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                    startActivityForResult(intent, TAKE_PHOTO_CODE);
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            case 199: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bStartStop.setText("Закончить маршрут");

                    // Запускаем свой IntentService
                    mMyServiceIntent = new Intent(MainActivity.this, GeoIntentService.class);

                    startService(mMyServiceIntent);

                    mUpdateBroadcastReceiver = new UpdateBroadcastReceiver();

                    // Регистрируем второй приёмник
                    IntentFilter updateIntentFilter = new IntentFilter(ACTION_UPDATE);
                    updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
                    registerReceiver(mUpdateBroadcastReceiver, updateIntentFilter);

                    isStarted = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {

            try {
                Bitmap bp = (Bitmap) data.getExtras().get("data");

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bp.compress(Bitmap.CompressFormat.PNG, 10, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

            } catch (Exception e) {
                Log.e("Camera", e.toString());
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateBroadcastReceiver != null) {
            unregisterReceiver(mUpdateBroadcastReceiver);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra(EXTRA_KEY_OUT);
            status.setText(result);
        }
    }

    public class UpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(EXTRA_KEY_UPDATE, 0);
            progressBar.setProgress(update);
        }
    }
}
