package myos.jacek.compass;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener{

    private ImageView ivCompass;
    private ImageView ivArrow;
    private TextView tvDegree;
    private FloatingActionButton fabNavigate;
    private FloatingActionButton fabCompass;
    private CoordinatorLayout mCoordinator;
    private EditText etLat;
    private EditText etLong;

    private String userLat;
    private String userLong;

    private float currentDegreeCompass = 0f;
    private float currentDegreeArrow = 0f;
    private float currentAzimuth;
    private float currentArrowAzimuth;

    private SensorManager mSensorManager;

    private Sensor accelerometer;
    private Sensor magnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    private SharedPreferences preferences;
    private static String PREFS_KEY_SENSOR_SETUP = "pref_sensor_settings";
    private static String PREFS_KEY_LOW_PASS_FILTER = "pref_low_pass_filter";

    private ProviderLocationTracker gps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();


        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, android.R.color.transparent)));

        fabNavigate.setOnClickListener(this);
        fabCompass.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(preferences.getBoolean(PREFS_KEY_SENSOR_SETUP,false)){
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }else{
            Log.d("compass", "onResume TypeO Orientation registered");
            mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(preferences.getBoolean(PREFS_KEY_SENSOR_SETUP,false)){
            mSensorManager.unregisterListener(this, accelerometer);
            mSensorManager.unregisterListener(this, magnetometer);
        }else{
            Log.d("compass", "onResume TypeO Orientation urregistered");
            mSensorManager.unregisterListener(this);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() == R.id.action_settings){
            Intent i = new Intent(MainActivity.this,
                    PreferenceActivity.class);
            startActivityForResult(i, 1);
        }

        return super.onOptionsItemSelected(item);
    }

    private void initialize(){
        ivCompass = (ImageView) findViewById(R.id.iv_compass);
        ivArrow = (ImageView) findViewById(R.id.iv_arrow);
        tvDegree = (TextView) findViewById(R.id.tv_degree);
        fabNavigate = (FloatingActionButton) findViewById(R.id.fab_navigate);
        fabCompass = (FloatingActionButton) findViewById(R.id.fab_compass);
        mCoordinator = (CoordinatorLayout) findViewById(R.id.cl_compass);
        etLat = (EditText) findViewById(R.id.et_lat);
        etLong = (EditText) findViewById(R.id.et_long);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("compass", "onSensorChanged " + event.values.toString());
        if(preferences.getBoolean(PREFS_KEY_SENSOR_SETUP, false)){
                // 2 approach
                 Log.d("compass", "2 approach onSensorChanged " + event.values.toString());
                if (event.sensor == accelerometer) {
                    if(preferences.getBoolean(PREFS_KEY_LOW_PASS_FILTER,false)){
                        mLastAccelerometer = lowPass(event.values.clone(), mLastAccelerometer);
                    }else{
                        System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
                    }
                    mLastAccelerometerSet = true;

                } else if (event.sensor == magnetometer) {
                    if(preferences.getBoolean(PREFS_KEY_LOW_PASS_FILTER,false)){
                        mLastMagnetometer = lowPass(event.values.clone(), mLastMagnetometer);
                    }else{
                        System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
                    }
                    mLastMagnetometerSet = true;
                }
                if (mLastAccelerometerSet && mLastMagnetometerSet) {
                    SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
                    SensorManager.getOrientation(mR, mOrientation);
                    float azimuthInRadians = mOrientation[0];
                    currentAzimuth = Math.round((float) (Math.toDegrees(azimuthInRadians) + 360) % 360);
                    currentArrowAzimuth = Math.round((float) (Math.toDegrees(azimuthInRadians) + 360) % 360);
                    animateCompass(currentAzimuth);

                    if(Compass.getInstance().isNAVIGETE_BASED_ON_USER_INDICATION()){
                        currentArrowAzimuth -= bearing(userLocation.getLatitude(), userLocation.getLongitude(), Double.valueOf(userLat), Double.valueOf(userLong));

                        animateArrow(currentArrowAzimuth);

                    }else{
                        animateArrow(currentAzimuth);
                    }

                }
            }else {
                //1 approach

                    Log.d("compass", "1 approach onSensorChanged " + event.values.toString());

                    currentAzimuth = Math.round(event.values[0]);
                    currentArrowAzimuth = Math.round(event.values[0]);
                    animateCompass(currentAzimuth);

                    if(Compass.getInstance().isNAVIGETE_BASED_ON_USER_INDICATION()){
                        currentArrowAzimuth -= bearing(userLocation.getLatitude(), userLocation.getLongitude(), Double.valueOf(userLat), Double.valueOf(userLong));

                        animateArrow(currentArrowAzimuth);

                    }else{
                        animateArrow(currentAzimuth);
                    }
            }





    }

    private void animateCompass(float azimuth){
        RotateAnimation ra = new RotateAnimation(
                currentDegreeCompass,
                -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        ra.setDuration(210);

        ra.setFillAfter(true);

        ivCompass.startAnimation(ra);
        currentDegreeCompass = -azimuth;
        if(!Compass.getInstance().isNAVIGETE_BASED_ON_USER_INDICATION()) {
            tvDegree.setText("Heading: " + Float.toString(currentDegreeCompass) + " degrees from North");
        }
    }

    private void animateArrow(float azimuth){
        RotateAnimation ra = new RotateAnimation(
                -currentDegreeArrow,
                -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        ra.setDuration(210);

        ra.setFillAfter(true);

        ivArrow.startAnimation(ra);
        currentDegreeArrow = azimuth;
        if(Compass.getInstance().isNAVIGETE_BASED_ON_USER_INDICATION()) {
            tvDegree.setText("Heading: " + Float.toString(currentDegreeArrow) + " degrees from target");
        }
    }


    static final float ALPHA = 0.15f;

    private float[] lowPass(float[] input, float[] output)
    {
        if (output == null)
            return input;

        for (int i = 0; i < input.length; i++)
        {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

     private float bearing(double lat1, double long1, double lat2, double long2){
        double degToRad = Math.PI / 180.0;
        double phi1 = lat1 * degToRad;
        double phi2 = lat2 * degToRad;
        double lam1 = long1 * degToRad;
        double lam2 = long2 * degToRad;
        return (float) (Math.atan2(Math.sin(lam2-lam1)*Math.cos(phi2), Math.cos(phi1)*Math.sin(phi2) - Math.sin(phi1)*Math.cos(phi2)*Math.cos(lam2-lam1) ) * 180/Math.PI);
    }


        @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

    private float newAzimuth;
    private Location targetLocation = new Location("target");
    private Location userLocation = new Location("start");

    @Override
    public void onClick(View v) {
        if(v.getId()==fabNavigate.getId()){
            if(etLong.getText().toString().matches("")  ||  etLat.getText().toString().matches("")){
                Snackbar.make(mCoordinator, R.string.fillData, Snackbar.LENGTH_SHORT).setAction("DISMISS", null).show();
            }else {
                Compass.getInstance().setNAVIGETE_BASED_ON_USER_INDICATION(true);
                userLat = etLat.getText().toString();
                userLong = etLong.getText().toString();

                targetLocation.setLatitude(Double.valueOf(userLat));
                targetLocation.setLatitude(Double.valueOf(userLong));

                gps = new ProviderLocationTracker(this, ProviderLocationTracker.ProviderType.NETWORK);

                gps.start(new LocationTracker.LocationUpdateListener() {
                    @Override
                    public void onUpdate(Location oldLoc, long oldTime, Location newLoc, long newTime) {

                        userLocation = newLoc;


                        Log.d("compass", "new myLatitude: " + userLocation.getLatitude() + " | myLongitude: " + userLocation.getLongitude());
                        Log.d("compass", "stale myLatitude: " + gps.getPossiblyStaleLocation().getLatitude() + " | myLongitude: " + gps.getPossiblyStaleLocation().getLongitude());

                      /*  newAzimuth = calculateAzimuth(gps.getPossiblyStaleLocation().getLatitude(), gps.getPossiblyStaleLocation().getLongitude(),
                                Double.valueOf(etLat.getText().toString()), Double.valueOf(etLong.getText().toString()));
                        animateArrow(newAzimuth);*/
                    }
                });

            }
        }

        if(v.getId()==fabCompass.getId()){
            Compass.getInstance().setNAVIGETE_BASED_ON_USER_INDICATION(false);
        }

    }

   /* protected double bearing(double latitude , double longitude, double eTLati, double eTLong){


        double latitude1 = Math.toRadians(latitude);
        double latitude2 = Math.toRadians(eTLati);
        double longDiff = Math.toRadians(eTLong - longitude);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);


        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }*/
}

















