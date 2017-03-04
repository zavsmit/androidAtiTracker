package su.ati.tracker.atitracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;

import static su.ati.tracker.atitracker.GeoIntentService.ACTION_UPDATE;
import static su.ati.tracker.atitracker.GeoIntentService.EXTRA_KEY_UPDATE;

public class MainActivity extends AppCompatActivity {

    private final static int PERMISSION_PHOTO_CODE = 99;
    private final static int PERMISSION_GEO_CODE = 199;
    private int TAKE_PHOTO_CODE = 55;
    private AppCompatTextView price;
    private ProgressBar progressBar;
    private boolean isStarted;
    private Menu mMenu;
    private UpdateBroadcastReceiver mUpdateBroadcastReceiver;
    private Intent mMyServiceIntent;

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
                    startRoute();
                }
            }
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
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenu = menu;

        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem start = menu.findItem(R.id.menu_action_start);
        MenuItem end = menu.findItem(R.id.menu_action_end);

        if (isStarted) {
            start.setVisible(true);
        } else {
            end.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public void changeItemMenu(boolean isShowEnd) {
        if (mMenu != null) {
            mMenu.findItem(R.id.menu_action_start).setVisible(!isShowEnd);
            mMenu.findItem(R.id.menu_action_end).setVisible(isShowEnd);
        }
    }

    private void endRoute() {
        changeItemMenu(false);

        if (mMyServiceIntent != null) {
            stopService(mMyServiceIntent);
            mMyServiceIntent = null;
        }

        unregisterReceiver(mUpdateBroadcastReceiver);
        isStarted = false;
    }

    private void startRoute() {
        mMyServiceIntent = new Intent(MainActivity.this, GeoIntentService.class);
        changeItemMenu(true);
        startService(mMyServiceIntent);

        mUpdateBroadcastReceiver = new UpdateBroadcastReceiver();

        // Регистрируем второй приёмник
        IntentFilter updateIntentFilter = new IntentFilter(ACTION_UPDATE);
        updateIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(mUpdateBroadcastReceiver, updateIntentFilter);

        isStarted = true;
    }

    public class UpdateBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int update = intent.getIntExtra(EXTRA_KEY_UPDATE, 0);
            progressBar.setProgress(update);
            price.setText(update + " \u20BD");
        }
    }

}
