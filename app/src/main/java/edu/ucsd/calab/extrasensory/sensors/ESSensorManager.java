package edu.ucsd.calab.extrasensory.sensors;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.data.ESDataFilesAccessor;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity;
import edu.ucsd.calab.extrasensory.ui.FeedbackActivity;
import edu.ucsd.calab.extrasensory.ui.HomeFragment;

/**
 * This class is to handle the activation of sensors for the recording period,
 * collecting the measured data and bundling it together.
 *
 * This class is designed as a singleton (maximum of 1 instance will be created),
 * in order to avoid collisions and to make sure only a single thread uses the sensors at any time.
 *
 * Created by Yonatan on 1/15/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESSensorManager extends Context
        implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    public static final String BROADCAST_RECORDING_STATE_CHANGED = "edu.ucsd.calab.extrasensory.broadcast.recording_state";

    // Static part of the class:
    private static ESSensorManager theSingleSensorManager;
    private static final String LOG_TAG = "[ESSensorManager]";

    private static final int LOW_FREQ_SAMPLE_PERIOD_MICROSECONDS = 1000000;
    private static final int SAMPLE_PERIOD_MICROSECONDS = 25000;
    private static int validForHowManyMinutespublic;
    private static String mainActivitypublic;
    private static String[] secondaryActivitypublic;
    private static String[] moodpublic;
    private final int NUM_SAMPLES_IN_SESSION = 1800;
    private static final double NANOSECONDS_IN_SECOND = 1e9f;
    private static final double NANOSECONDS_IN_MILLISECOND = 1e6f;
    private static final double MILLISECONDS_IN_SECOND = 1000;
    private static final long LOCATION_UPDATE_INTERVAL_MILLIS = 500;
    private static final long LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS = 50;
    private static final float LOCATION_BUBBLE_RADIUS_METERS = 500.0f;
    private static final String HIGH_FREQ_DATA_FILENAME = ".json";
    private static final int MAX_TIME_RECORDING_IN_SECONDS = 20000;// ESApplication.validForHowManyMinutespublic - 30;

    // Raw motion sensors:
    private static final String RAW_ACC_X = "raw_acc_x";
    private static final String RAW_ACC_Y = "raw_acc_y";
    private static final String RAW_ACC_Z = "raw_acc_z";
    private static final String RAW_ACC_UNIX_TIME = "raw_acc_unix_time";
    private static final String RAW_ACC_TIME_S = "raw_acc_timeref_s";
    private static final String RAW_ACC_TIME_MS = "raw_acc_timeref_ms";

    private static final String RAW_MAGNET_X = "raw_magnet_x";
    private static final String RAW_MAGNET_Y = "raw_magnet_y";
    private static final String RAW_MAGNET_Z = "raw_magnet_z";
    private static final String RAW_MAGNET_BIAS_X = "raw_magnet_bias_x";
    private static final String RAW_MAGNET_BIAS_Y = "raw_magnet_bias_y";
    private static final String RAW_MAGNET_BIAS_Z = "raw_magnet_bias_z";
    private static final String RAW_MAGNET_UNIX_TIME = "raw_magnet_unix_time";
    private static final String RAW_MAGNET_TIME_S = "raw_magnet_timeref_s";
    private static final String RAW_MAGNET_TIME_MS = "raw_magnet_timeref_ms";

    private static final String RAW_GYRO_X = "raw_gyro_x";
    private static final String RAW_GYRO_Y = "raw_gyro_y";
    private static final String RAW_GYRO_Z = "raw_gyro_z";
    private static final String RAW_GYRO_DRIFT_X = "raw_gyro_drift_x";
    private static final String RAW_GYRO_DRIFT_Y = "raw_gyro_drift_y";
    private static final String RAW_GYRO_DRIFT_Z = "raw_gyro_drift_z";
    private static final String RAW_GYRO_UNIX_TIME = "raw_gyro_unix_time";
    private static final String RAW_GYRO_TIME_S = "raw_gyro_timeref_s";
    private static final String RAW_GYRO_TIME_MS = "raw_gyro_timeref_ms";

    // Processed motion sensors (software "sensors"):
    private static final String PROC_ACC_X = "processed_user_acc_x";
    private static final String PROC_ACC_Y = "processed_user_acc_y";
    private static final String PROC_ACC_Z = "processed_user_acc_z";
    private static final String PROC_ACC_UNIX_TIME = "processed_user_acc_unix_time";
    private static final String PROC_ACC_TIME_S = "processed_user_acc_timeref_s";
    private static final String PROC_ACC_TIME_MS = "processed_user_acc_timeref_ms";

    private static final String PROC_GRAV_X = "processed_gravity_x";
    private static final String PROC_GRAV_Y = "processed_gravity_y";
    private static final String PROC_GRAV_Z = "processed_gravity_z";
    private static final String PROC_GRAV_UNIX_TIME = "processed_gravity_unix_time";
    private static final String PROC_GRAV_TIME_S = "processed_gravity_timeref_s";
    private static final String PROC_GRAV_TIME_MS = "processed_gravity_timeref_ms";

    private static final String PROC_MAGNET_X = "processed_magnet_x";
    private static final String PROC_MAGNET_Y = "processed_magnet_y";
    private static final String PROC_MAGNET_Z = "processed_magnet_z";
    private static final String PROC_MAGNET_UNIX_TIME = "processed_magnet_unix_time";
    private static final String PROC_MAGNET_TIME_S = "processed_magnet_timeref_s";
    private static final String PROC_MAGNET_TIME_MS = "processed_magnet_timeref_ms";

    private static final String PROC_GYRO_X = "processed_gyro_x";
    private static final String PROC_GYRO_Y = "processed_gyro_y";
    private static final String PROC_GYRO_Z = "processed_gyro_z";
    private static final String PROC_GYRO_UNIX_TIME = "processed_gyro_unix_time";
    private static final String PROC_GYRO_TIME_S = "processed_gyro_timeref_s";
    private static final String PROC_GYRO_TIME_MS = "processed_gyro_timeref_ms";

    private static final String PROC_ROTATION_X = "processed_rotation_vector_x";
    private static final String PROC_ROTATION_Y = "processed_rotation_vector_y";
    private static final String PROC_ROTATION_Z = "processed_rotation_vector_z";
    private static final String PROC_ROTATION_COS = "processed_rotation_vector_cosine";
    private static final String PROC_ROTATION_ACCURACY = "processed_rotation_vector_accuracy";
    private static final String PROC_ROTATION_UNIX_TIME = "processed_rotation_vector_unix_time";
    private static final String PROC_ROTATION_TIME_S = "processed_rotation_vector_timeref_s";
    private static final String PROC_ROTATION_TIME_MS = "processed_rotation_vector_timeref_ms";

    // Location sensors:
    private static final String LOC_LAT = "location_latitude";
    private static final String LOC_LONG = "location_longitude";
    private static final String LOC_ALT = "location_altitude";
    private static final String LOC_SPEED = "location_speed";
    private static final String LOC_HOR_ACCURACY = "location_horizontal_accuracy";
    private static final String LOC_BEARING = "location_bearing";
    private static final String LOC_TIME = "location_timeref";

    private static final double LOC_ACCURACY_UNAVAILABLE = -1;
    private static final double LOC_ALT_UNAVAILABLE = -1000000;
    private static final double LOC_BEARING_UNAVAILABLE = -1;
    private static final double LOC_SPEED_UNAVAILABLE = -1;
    private static final double LOC_LAT_HIDDEN = -1000;
    private static final double LOC_LONG_HIDDEN = -1000;

    private static final String LOCATION_QUICK_FEATURES = "location_quick_features";
    private static final String LOCATION_FEATURE_STD_LAT = "std_lat";
    private static final String LOCATION_FEATURE_STD_LONG = "std_long";
    private static final String LOCATION_FEATURE_LAT_CHANGE = "lat_change";
    private static final String LOCATION_FEATURE_LONG_CHANGE = "long_change";
    private static final String LOCATION_FEATURE_LAT_DERIV = "mean_abs_lat_deriv";
    private static final String LOCATION_FEATURE_LONG_DERIV = "mean_abs_long_deriv";

    // Low frequency measurements:
    private static final String LOW_FREQ = "low_frequency";

    private static final String TEMPERATURE_AMBIENT = "temperature_ambient";
    private static final String LIGHT = "light";
    private static final String PRESSURE = "pressure";
    private static final String PROXIMITY = "proximity_cm";
    private static final String HUMIDITY = "relative_humidity";

    private static final String WIFI_STATUS = "wifi_status";
    private static final String APP_STATE = "app_state";
    private static final String ON_THE_PHONE = "on_the_phone";
    private static final String BATTERY_LEVEL = "battery_level";
    private static final String BATTERY_STATE = "battery_state";
    private static final String BATTERY_PLUGGED = "battery_plugged";
    private static final String SCREEN_BRIGHT = "screen_brightness";
    private static final String RINGER_MODE = "ringer_mode";

    private static final String HOUR_OF_DAY = "hour_of_day";
    private static final String MINUTE_IN_HOUR = "minute_in_hour";
    private static final String TIMEZONE_LONG_NAME = "timezone_long_name";
    private static final String DATE_TIME_ISO8601 = "date_time_iso8601";
    private static final String UNIX_TIME = "unix_time";

    // Values of discrete properties:
    private static final String MISSING_VALUE_STR = "missing";

    private static final String BATTERY_STATUS_CHARGING_STR = "charging";
    private static final String BATTERY_STATUS_DISCHARGING_STR = "discharging";
    private static final String BATTERY_STATUS_FULL_STR = "full";
    private static final String BATTERY_STATUS_NOT_CHARGING_STR = "not_charging";
    private static final String BATTERY_STATUS_UNKNOWN_STR = "unknown";


    private static String getStringValueForBatteryStatus(int batteryStatus) {
        switch (batteryStatus) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return BATTERY_STATUS_CHARGING_STR;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return BATTERY_STATUS_DISCHARGING_STR;
            case BatteryManager.BATTERY_STATUS_FULL:
                return BATTERY_STATUS_FULL_STR;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return BATTERY_STATUS_NOT_CHARGING_STR;
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return BATTERY_STATUS_UNKNOWN_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static final String BATTERY_PLUGGED_AC_STR = "ac";
    private static final String BATTERY_PLUGGED_USB_STR = "usb";
    private static final String BATTERY_PLUGGED_WIRELESS_STR = "wireless";

    private static String getStringValueForBatteryPlugged(int batteryPlugged) {
        switch (batteryPlugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return BATTERY_PLUGGED_AC_STR;
            case BatteryManager.BATTERY_PLUGGED_USB:
                return BATTERY_PLUGGED_USB_STR;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return BATTERY_PLUGGED_WIRELESS_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static final String RINGER_MODE_NORMAL_STR = "normal";
    private static final String RINGER_MODE_SILENT_STR = "silent_no_vibrate";
    private static final String RINGER_MODE_VIBRATE_STR = "silent_with_vibrate";

    private static String getStringValueForRingerMode(int ringerMode) {
        switch (ringerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                return RINGER_MODE_NORMAL_STR;
            case AudioManager.RINGER_MODE_SILENT:
                return RINGER_MODE_SILENT_STR;
            case AudioManager.RINGER_MODE_VIBRATE:
                return RINGER_MODE_VIBRATE_STR;
            default:
                return MISSING_VALUE_STR;
        }
    }

    private static String getSensorLeadingMeasurementKey(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                return RAW_ACC_X;
            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                return RAW_MAGNET_X;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                return RAW_GYRO_X;
            case Sensor.TYPE_GRAVITY:
                return PROC_GRAV_X;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                return PROC_ACC_X;
            case Sensor.TYPE_MAGNETIC_FIELD:
                return PROC_MAGNET_X;
            case Sensor.TYPE_GYROSCOPE:
                return PROC_GYRO_X;
            case Sensor.TYPE_ROTATION_VECTOR:
                return PROC_ROTATION_X;
            default:
                throw new UnknownError("Requested measurement key for unknown sensor, with type: " + sensorType + " with name: " + getESSensorManager().getSensorNiceName(sensorType));
        }
    }

    /**
     * Get the single instance of this class
     * @return
     */
    public static ESSensorManager getESSensorManager() {
        if (theSingleSensorManager == null) {
            theSingleSensorManager = new ESSensorManager();
        }

        return theSingleSensorManager;
    }


    // Non static part:
    private final SensorManager _sensorManager;
    private final GoogleApiClient _googleApiClient;
    private final PolarActivity _polarProcessor;
    private final ESApplication _esApplication;
    private final HomeFragment _homefragment;
    private final FeedbackActivity _feedbackActivity;

    private HashMap<String, ArrayList<Double>> _highFreqData;
    private HashMap<String, ArrayList<Double>> _locationCoordinatesData;
    private JSONObject _lowFreqData;
    private ESTimestamp _timestamp;
    private final ArrayList<Sensor> _hiFreqSensors;
    private final ArrayList<String> _hiFreqSensorFeatureKeys;
    private ArrayList<String> _sensorKeysThatShouldGetEnoughSamples;
    private final ArrayList<Sensor> _lowFreqSensors;
    private final ArrayList<String> _lowFreqSensorFeatureKeys;
    private final Map<Integer, String> _sensorTypeToNiceName;

    private boolean _recordingRightNow = false;

    private boolean debugSensorSimulationMode() {
        return ESApplication.debugMode();
    }

    public boolean is_recordingRightNow() {
        return _recordingRightNow;
    }

    public String getSensorNiceName(int sensorType) {
        Integer type = new Integer(sensorType);
        if (_sensorTypeToNiceName.containsKey(type)) {
            return _sensorTypeToNiceName.get(type);
        } else {
            return "" + sensorType;
        }
    }

    public static void getForHowManyMinutes(int validForHowManyMinutes) {
        validForHowManyMinutespublic = validForHowManyMinutes;
    }
    public static void getMainActivity(String mainActivity) {
        mainActivitypublic = mainActivity;
    }
    public static void getSecondaryActivity(String[] secondaryActivity) {
        secondaryActivitypublic = secondaryActivity;
    }
    public static void getMood(String[] mood) {
        moodpublic = mood;
    }

    private void set_recordingRightNow(boolean recordingRightNow) {
        _recordingRightNow = recordingRightNow;
        Intent broadcast = new Intent(BROADCAST_RECORDING_STATE_CHANGED);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(ESApplication.getTheAppContext());
        manager.sendBroadcast(broadcast);
    }

    /**
     * Making the constructor private, in order to make this class a singleton
     */
    private ESSensorManager() {
        _feedbackActivity = FeedbackActivity.getFeedbackActivity();
        _homefragment = new HomeFragment();
        _esApplication = new ESApplication();
        _googleApiClient = new GoogleApiClient.Builder(ESApplication.getTheAppContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        _sensorManager = (SensorManager) ESApplication.getTheAppContext().getSystemService(Context.SENSOR_SERVICE);
        // Initialize the sensors:
        _hiFreqSensors = new ArrayList<>(10);
        _hiFreqSensorFeatureKeys = new ArrayList<>(10);
        _lowFreqSensors = new ArrayList<>(10);
        _lowFreqSensorFeatureKeys = new ArrayList<>(10);
        _timestamp = new ESTimestamp(0);
        _sensorTypeToNiceName = new HashMap<>(10);


        // Polar processor:
        _polarProcessor = PolarActivity.getPolarProcessor();

        // Add raw motion sensors:
        if (!tryToAddSensor(Sensor.TYPE_ACCELEROMETER, true, "raw accelerometer", RAW_ACC_X)) {
            Log.e(LOG_TAG, "There is no accelerometer. Canceling recording.");
            return;
        }
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, true, "raw magnetometer", RAW_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, true, "raw gyroscope", RAW_GYRO_X);
        // Add processed motion sensors:
        tryToAddSensor(Sensor.TYPE_GRAVITY, true, "gravity", PROC_GRAV_X);
        tryToAddSensor(Sensor.TYPE_LINEAR_ACCELERATION, true, "linear acceleration", PROC_ACC_X);
        tryToAddSensor(Sensor.TYPE_MAGNETIC_FIELD, true, "calibrated magnetometer", PROC_MAGNET_X);
        tryToAddSensor(Sensor.TYPE_GYROSCOPE, true, "calibrated gyroscope", PROC_GYRO_X);
        tryToAddSensor(Sensor.TYPE_ROTATION_VECTOR, true, "rotation vector", PROC_ROTATION_X);

        // Add low frequency sensors:
        tryToAddSensor(Sensor.TYPE_AMBIENT_TEMPERATURE, false, "ambient temperature", TEMPERATURE_AMBIENT);
        tryToAddSensor(Sensor.TYPE_LIGHT, false, "light", LIGHT);
        tryToAddSensor(Sensor.TYPE_PRESSURE, false, "pressure", PRESSURE);
        tryToAddSensor(Sensor.TYPE_PROXIMITY, false, "proximity", PROXIMITY);
        tryToAddSensor(Sensor.TYPE_RELATIVE_HUMIDITY, false, "relative humidity", HUMIDITY);

        // This list can be prepared at every recording session, according to the sensors that should be recorded

        Log.v(LOG_TAG, "An instance of ESSensorManager was created.");
    }

    @Override
    public AssetManager getAssets() {
        return null;
    }

    @Override
    public Resources getResources() {
        return null;
    }

    @Override
    public PackageManager getPackageManager() {
        return null;
    }

    @Override
    public ContentResolver getContentResolver() {
        return null;
    }

    @Override
    public Looper getMainLooper() {
        return null;
    }

    @Override
    public Context getApplicationContext() {
        return null;
    }

    @Override
    public void setTheme(int resid) {

    }

    @Override
    public Resources.Theme getTheme() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;
    }

    @Override
    public String getPackageName() {
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return null;
    }

    @Override
    public String getPackageResourcePath() {
        return null;
    }

    @Override
    public String getPackageCodePath() {
        return null;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        return null;
    }

    @Override
    public boolean moveSharedPreferencesFrom(Context sourceContext, String name) {
        return false;
    }

    @Override
    public boolean deleteSharedPreferences(String name) {
        return false;
    }

    @Override
    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return null;
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return null;
    }

    @Override
    public boolean deleteFile(String name) {
        return false;
    }

    @Override
    public File getFileStreamPath(String name) {
        return null;
    }

    @Override
    public File getDataDir() {
        return null;
    }

    @Override
    public File getFilesDir() {
        return null;
    }

    @Override
    public File getNoBackupFilesDir() {
        return null;
    }

    @Nullable
    @Override
    public File getExternalFilesDir(@Nullable String type) {
        return null;
    }

    @Override
    public File[] getExternalFilesDirs(String type) {
        return new File[0];
    }

    @Override
    public File getObbDir() {
        return null;
    }

    @Override
    public File[] getObbDirs() {
        return new File[0];
    }

    @Override
    public File getCacheDir() {
        return null;
    }

    @Override
    public File getCodeCacheDir() {
        return null;
    }

    @Nullable
    @Override
    public File getExternalCacheDir() {
        return null;
    }

    @Override
    public File[] getExternalCacheDirs() {
        return new File[0];
    }

    @Override
    public File[] getExternalMediaDirs() {
        return new File[0];
    }

    @Override
    public String[] fileList() {
        return new String[0];
    }

    @Override
    public File getDir(String name, int mode) {
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        return null;
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, @Nullable DatabaseErrorHandler errorHandler) {
        return null;
    }

    @Override
    public boolean moveDatabaseFrom(Context sourceContext, String name) {
        return false;
    }

    @Override
    public boolean deleteDatabase(String name) {
        return false;
    }

    @Override
    public File getDatabasePath(String name) {
        return null;
    }

    @Override
    public String[] databaseList() {
        return new String[0];
    }

    @Override
    public Drawable getWallpaper() {
        return null;
    }

    @Override
    public Drawable peekWallpaper() {
        return null;
    }

    @Override
    public int getWallpaperDesiredMinimumWidth() {
        return 0;
    }

    @Override
    public int getWallpaperDesiredMinimumHeight() {
        return 0;
    }

    @Override
    public void setWallpaper(Bitmap bitmap) throws IOException {

    }

    @Override
    public void setWallpaper(InputStream data) throws IOException {

    }

    @Override
    public void clearWallpaper() throws IOException {

    }

    @Override
    public void startActivity(Intent intent) {

    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {

    }

    @Override
    public void startActivities(Intent[] intents) {

    }

    @Override
    public void startActivities(Intent[] intents, Bundle options) {

    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags) throws IntentSender.SendIntentException {

    }

    @Override
    public void startIntentSender(IntentSender intent, @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags, @Nullable Bundle options) throws IntentSender.SendIntentException {

    }

    @Override
    public void sendBroadcast(Intent intent) {

    }

    @Override
    public void sendBroadcast(Intent intent, @Nullable String receiverPermission) {

    }

    @Override
    public void sendOrderedBroadcast(Intent intent, @Nullable String receiverPermission) {

    }

    @Override
    public void sendOrderedBroadcast(@NonNull Intent intent, @Nullable String receiverPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Override
    public void sendBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission) {

    }

    @Override
    public void sendOrderedBroadcastAsUser(Intent intent, UserHandle user, @Nullable String receiverPermission, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

    }

    @Override
    public void sendStickyBroadcast(Intent intent) {

    }

    @Override
    public void sendStickyOrderedBroadcast(Intent intent, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

    }

    @Override
    public void removeStickyBroadcast(Intent intent) {

    }

    @Override
    public void sendStickyBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Override
    public void sendStickyOrderedBroadcastAsUser(Intent intent, UserHandle user, BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

    }

    @Override
    public void removeStickyBroadcastAsUser(Intent intent, UserHandle user) {

    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        return null;
    }

    @Nullable
    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter, int flags) {
        return null;
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler) {
        return null;
    }

    @Nullable
    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter, @Nullable String broadcastPermission, @Nullable Handler scheduler, int flags) {
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {

    }

    @Nullable
    @Override
    public ComponentName startService(Intent service) {
        return null;
    }

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        return null;
    }

    @Override
    public boolean stopService(Intent service) {
        return false;
    }

    @Override
    public boolean bindService(Intent service, @NonNull ServiceConnection conn, int flags) {
        return false;
    }

    @Override
    public void unbindService(@NonNull ServiceConnection conn) {

    }

    @Override
    public boolean startInstrumentation(@NonNull ComponentName className, @Nullable String profileFile, @Nullable Bundle arguments) {
        return false;
    }

    @Override
    public Object getSystemService(@NonNull String name) {
        return null;
    }

    @Nullable
    @Override
    public String getSystemServiceName(@NonNull Class<?> serviceClass) {
        return null;
    }

    @Override
    public int checkPermission(@NonNull String permission, int pid, int uid) {
        return 0;
    }

    @Override
    public int checkCallingPermission(@NonNull String permission) {
        return 0;
    }

    @Override
    public int checkCallingOrSelfPermission(@NonNull String permission) {
        return 0;
    }

    @Override
    public int checkSelfPermission(@NonNull String permission) {
        return 0;
    }

    @Override
    public void enforcePermission(@NonNull String permission, int pid, int uid, @Nullable String message) {

    }

    @Override
    public void enforceCallingPermission(@NonNull String permission, @Nullable String message) {

    }

    @Override
    public void enforceCallingOrSelfPermission(@NonNull String permission, @Nullable String message) {

    }

    @Override
    public void grantUriPermission(String toPackage, Uri uri, int modeFlags) {

    }

    @Override
    public void revokeUriPermission(Uri uri, int modeFlags) {

    }

    @Override
    public void revokeUriPermission(String toPackage, Uri uri, int modeFlags) {

    }

    @Override
    public int checkUriPermission(Uri uri, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public int checkCallingUriPermission(Uri uri, int modeFlags) {
        return 0;
    }

    @Override
    public int checkCallingOrSelfUriPermission(Uri uri, int modeFlags) {
        return 0;
    }

    @Override
    public int checkUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags) {
        return 0;
    }

    @Override
    public void enforceUriPermission(Uri uri, int pid, int uid, int modeFlags, String message) {

    }

    @Override
    public void enforceCallingUriPermission(Uri uri, int modeFlags, String message) {

    }

    @Override
    public void enforceCallingOrSelfUriPermission(Uri uri, int modeFlags, String message) {

    }

    @Override
    public void enforceUriPermission(@Nullable Uri uri, @Nullable String readPermission, @Nullable String writePermission, int pid, int uid, int modeFlags, @Nullable String message) {

    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        return null;
    }

    @Override
    public Context createContextForSplit(String splitName) throws PackageManager.NameNotFoundException {
        return null;
    }

    @Override
    public Context createConfigurationContext(@NonNull Configuration overrideConfiguration) {
        return null;
    }

    @Override
    public Context createDisplayContext(@NonNull Display display) {
        return null;
    }

    @Override
    public Context createDeviceProtectedStorageContext() {
        return null;
    }

    @Override
    public boolean isDeviceProtectedStorage() {
        return false;
    }

    private boolean tryToAddSensor(int sensorType, boolean isHighFreqSensor, String niceName, String featureKey) {
        Sensor sensor = _sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            Log.i(LOG_TAG, "No available sensor: " + niceName);
            return false;
        } else {
            _sensorTypeToNiceName.put(sensorType, niceName);
            if (isHighFreqSensor) {
                Log.i(LOG_TAG, "Adding hi-freq sensor: " + niceName);
                _hiFreqSensors.add(sensor);
                _hiFreqSensorFeatureKeys.add(featureKey);
            } else {
                Log.i(LOG_TAG, "Adding low-freq sensor: " + niceName);
                _lowFreqSensors.add(sensor);
                _lowFreqSensorFeatureKeys.add(featureKey);
            }
            return true;
        }
    }

    private ArrayList<Integer> getSensorTypesFromSensors(ArrayList<Sensor> sensors) {
        if (sensors == null) {
            return new ArrayList<Integer>(10);
        }
        ArrayList<Integer> sensorTypes = new ArrayList<>(sensors.size());
        for (Sensor sensor : sensors) {
            sensorTypes.add(new Integer(sensor.getType()));
        }
        return sensorTypes;
    }

    public ArrayList<Integer> getRegisteredHighFreqSensorTypes() {
        return getSensorTypesFromSensors(_hiFreqSensors);
    }

    public ArrayList<Integer> getRegisteredLowFreqSensorTypes() {
        return getSensorTypesFromSensors(_lowFreqSensors);
    }

    /**
     * Start a recording session from the sensors,
     * and initiate sending the collected measurements to the server.
     *
     * @param timestamp This recording session's identifying timestamp
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void startRecordingSensors(ESTimestamp timestamp) {
        Log.i(LOG_TAG, "Starting recording for timestamp: " + timestamp.toString());
        clearRecordingSession(true);
        set_recordingRightNow(true);
        // Set the new timestamp:
        _timestamp = timestamp;
        /////////////////////////
        // This is just for debugging. With the simulator (that doesn't produce actual sensor events):
      //  if (debugSensorSimulationMode()) {
       //     simulateRecordingSession();
       //     return;
       // }
        /////////////////////////

        // Start recording location:
        if (ESSettings.shouldRecordLocation()) {
            int googleServicesResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ESApplication.getTheAppContext());
            if (googleServicesResult == ConnectionResult.SUCCESS) {
                Log.i(LOG_TAG, "We have google play services");
                _googleApiClient.connect();
            } else {
                Log.i(LOG_TAG, "We don't have required google play services, so not using location services.");
            }
        } else {
            Log.d(LOG_TAG, "As requested: not recording location.");
        }

        // Start recording Polar:
       // if (_polarProcessor.isPolarConnected()) {
            PolarActivity.startHRBackground();
      //  }
        //else {
     //       Log.d(LOG_TAG,"Not recording from Polar.");
       // }


        // Start recording hi-frequency sensors:
        ArrayList<Integer> hfSensorTypesToRecord = ESSettings.highFreqSensorTypesToRecord();
        prepareListOfMeasurementsShouldGetEnoughSamples(hfSensorTypesToRecord);
        for (Sensor sensor : _hiFreqSensors) {
            if (hfSensorTypesToRecord.contains(Integer.valueOf(sensor.getType()))) {
                _sensorManager.registerListener(this, sensor, SAMPLE_PERIOD_MICROSECONDS);
                Log.d(LOG_TAG, "== Registering for recording HF sensor: " + getSensorNiceName(sensor.getType()));
            } else {
                Log.d(LOG_TAG, "As requested: not recording HF sensor: " + getSensorNiceName(sensor.getType()));
            }
        }

        // Start low-frequency sensors:
        ArrayList<Integer> lfSensorTypesToRecord = ESSettings.lowFreqSensorTypesToRecord();
        for (Sensor sensor : _lowFreqSensors) {
            if (lfSensorTypesToRecord.contains(Integer.valueOf(sensor.getType()))) {
                _sensorManager.registerListener(this, sensor, LOW_FREQ_SAMPLE_PERIOD_MICROSECONDS);
            } else {
                Log.d(LOG_TAG, "As requested: not recording LF sensor: " + getSensorNiceName(sensor.getType()));
            }
        }

        // Get phone-state measurements:
        collectPhoneStateMeasurements();

        // Maybe the session is already done:
        finishSessionIfReady();
    }

    private void prepareListOfMeasurementsShouldGetEnoughSamples(ArrayList<Integer> hfSensorTypesToRecord) {
        _sensorKeysThatShouldGetEnoughSamples = new ArrayList<>(10);
        // In case there are no high frequency sensors to record,
        // we shouldn't wait for any measurement-key to fill up with enough samples.
        if (hfSensorTypesToRecord.size() <= 0) {
            return;
        }

        // Otherwise, let's use the policy of having a single leading sensor (or several) that will determine when to stop the recording
        // (whenever that sensor contributed enough samples for its leading measurement key).
        // It is possible that accelerometer will reach the desired number of samples (e.g. 800) while gyroscope
        // will only reach 600 samples. This is because the sensor-sampling systems of Android are not aligned
        // among the sensors, and the sampling rates are not completely stable.
        Integer[] leadingSensorPriority = new Integer[]{
                Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_GRAVITY,
                Sensor.TYPE_MAGNETIC_FIELD, Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED
        };
        // Avoiding waiting for sensors that tend to be slow samplers, like gyroscope.
        for (Integer sensorTypeInteger : leadingSensorPriority) {
            if (hfSensorTypesToRecord.contains(sensorTypeInteger)) {
                // Then mark this single sensor as the one to wait for to get enough samples:
                Log.d(LOG_TAG, "Marking the leading sensor (the one from which we'll wait to get enough measurements): " + getSensorNiceName(sensorTypeInteger));
                _sensorKeysThatShouldGetEnoughSamples.add(getSensorLeadingMeasurementKey(sensorTypeInteger));
                return;
            }
        }
        // If we reached here, we have a risk:
        // all the high frequency sensors to be recorded are those that we do not trust to sample quickly enough to reach
        // the full number of samples. This can result in a situation where it will take more than a minute
        // before the sensor gets enough samples, and then the new recording session will begin.
        // To avoid this problem, we add an additional max-time-based mechanism to determine when to stop recording.
        Log.w(LOG_TAG, "!!! We have no sensor to tell us when to stop recording.");
    }

    private void simulateRecordingSession() {
        for (int i = 0; i < NUM_SAMPLES_IN_SESSION*validForHowManyMinutespublic; i++) {
            addHighFrequencyMeasurement(RAW_MAGNET_X, (double) 0);
            addHighFrequencyMeasurement(RAW_MAGNET_Y, (double) 0);
            addHighFrequencyMeasurement(RAW_MAGNET_Z, (double) 0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X, (double) 0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y, (double) 0);
            addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z, (double) 0);

            addHighFrequencyMeasurement(RAW_GYRO_X, (double) 0);
            addHighFrequencyMeasurement(RAW_GYRO_Y, (double) 0);
            addHighFrequencyMeasurement(RAW_GYRO_Z, (double) 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X, (double) 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y, (double) 0);
            addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z, (double) 0);

            addHighFrequencyMeasurement(PROC_GRAV_X, (double) 0);
            addHighFrequencyMeasurement(PROC_GRAV_Y, (double) 0);
            addHighFrequencyMeasurement(PROC_GRAV_Z, (double) 0);

            addHighFrequencyMeasurement(PROC_ACC_X, (double) 0);
            addHighFrequencyMeasurement(PROC_ACC_Y, (double) 0);
            addHighFrequencyMeasurement(PROC_ACC_Z, (double) 0);

            addHighFrequencyMeasurement(PROC_MAGNET_X, (double) 0);
            addHighFrequencyMeasurement(PROC_MAGNET_Y, (double) 0);
            addHighFrequencyMeasurement(PROC_MAGNET_Z, (double) 0);

            addHighFrequencyMeasurement(PROC_GYRO_X, (double) 0);
            addHighFrequencyMeasurement(PROC_GYRO_Y, (double) 0);
            addHighFrequencyMeasurement(PROC_GYRO_Z, (double) 0);

            addHighFrequencyMeasurement(PROC_ROTATION_X, (double) 0);
            addHighFrequencyMeasurement(PROC_ROTATION_Y, (double) 0);
            addHighFrequencyMeasurement(PROC_ROTATION_Z, (double) 0);

            addHighFrequencyMeasurement(RAW_ACC_X, (double) 0);
            addHighFrequencyMeasurement(RAW_ACC_Y, (double) 1);
            addHighFrequencyMeasurement(RAW_ACC_Z, (double) 2);
         //   if (addHighFrequencyMeasurement(RAW_ACC_TIME_S, (double) 111)) {
                finishSessionIfReady();
         //   }
        }
    }

    private void clearRecordingSession(boolean clearBeforeStart) {
        // Clear the high frequency map:
        _highFreqData = new HashMap<>(20);

        _locationCoordinatesData = new HashMap<>(2);
        _locationCoordinatesData.put(LOC_LAT, new ArrayList<Double>(10));
        _locationCoordinatesData.put(LOC_LONG, new ArrayList<Double>(10));

        _lowFreqData = new JSONObject();
        // Clear temporary data files:
        ESApplication.getTheAppContext().deleteFile(currentZipFilename());


        // Clear Polar data:
        if (!clearBeforeStart && _polarProcessor.isPolarConnected()) {
            _polarProcessor.cleanPolarMeasurements();
        }
        _polarProcessor.cleanPolarMeasurements();
    }

    /**
     * Stop any recording session, if any is active,
     * and clear any data that was collected from the sensors during the session.
     */
    public void stopRecordingSensors() {
        Log.i(LOG_TAG, "Stopping recording.");
        // Stop listening:
        _sensorManager.unregisterListener(this);
        _googleApiClient.disconnect();

        clearRecordingSession(false);
        set_recordingRightNow(false);
    }


    /**
     * Add another numeric value to a growing vector of measurements from a sensor.
     *
     * @param key The key of the specific measurement type
     * @param measurement The sampled measurement to be added to the vector
     * @return Did this key collect enough samples in this session?
     */
    private boolean addHighFrequencyMeasurement(String key, Double measurement) {
        if (_highFreqData == null) {
            Log.e(LOG_TAG, "Can't add measurement. HF data bundle is null");
        }

        // Check if the vector for this key was already initialized:
        if (!_highFreqData.containsKey(key)) {
            _highFreqData.put(key, new ArrayList<Double>());
        }

        Objects.requireNonNull(_highFreqData.get(key)).add(measurement);

        //if (RAW_ACC_X.equals(key) && (_highFreqData.get(key).size() % 100) == 0) {
        if ((Objects.requireNonNull(_highFreqData.get(key)).size() % 100) == 0) {
            logCurrentSampleSize();
        }

        return false;
    }


    private void logCurrentSampleSize() {
        int accSize = 0;
        if (_highFreqData.containsKey(RAW_ACC_X)) {
            accSize = Objects.requireNonNull(_highFreqData.get(RAW_ACC_X)).size();
        }
        int magnetSize = 0;
        if (_highFreqData.containsKey(PROC_MAGNET_X)) {
            magnetSize = Objects.requireNonNull(_highFreqData.get(PROC_MAGNET_X)).size();
        }
        int gyroSize = 0;
        if (_highFreqData.containsKey(PROC_GYRO_X)) {
            gyroSize = Objects.requireNonNull(_highFreqData.get(PROC_GYRO_X)).size();
        }

        Log.i(LOG_TAG, "Collected acc:" + accSize + ",magnet:" + magnetSize + ",gyro:" + gyroSize);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void finishIfTooMuchTimeRecording() throws JSONException {
        ESTimestamp now = new ESTimestamp();
        int timeRecording = now.differenceInSeconds(_timestamp);
        if (timeRecording >= (validForHowManyMinutespublic*60)) {
            Log.d(LOG_TAG, "Finishing this recording because it is already too long, num seconds: " + timeRecording);
            finishSession();
        }
    }

    private void finishSessionIfReady() {
        if (checkIfShouldFinishSession()) {
            //finishSession();

            // Because finishing a session can take some resources (mainly for calculating audio MFCC features),
            // the "finishSession" operation should be done in a background thread (to keep UI thread from stalling).
            Log.d(LOG_TAG, "Before call to background finishSession");
            new FinishSessionInBackground().execute();
            Log.d(LOG_TAG, "After call to background finishSession");
        }
    }

    private class FinishSessionInBackground extends AsyncTask<Void, Void, Void> {
        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        protected Void doInBackground(Void... params) {
            try {
                finishSession();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void finishSession() throws JSONException {
        Log.i(LOG_TAG, "Finishing recording session.");
        //LocationServices.FusedLocationApi.removeLocationUpdates(_googleApiClient,this);
        _googleApiClient.disconnect();


        // Finish Polar recording:
        HashMap<String, ArrayList<Integer>> polarhrMeasurements = null;
        if (_polarProcessor.isPolarConnected()) {
            _polarProcessor.completeHRBroadcast();
            polarhrMeasurements = _polarProcessor.getPolarhrMeasurements();
        }

        //PolarActivity mActivity = new getPolarhrMeasurements();
        //polarhrMeasurements = PolarActivity.polarhrMeasurements;
        //if (_polarProcessor.isPolarConnected()) {
            /*_polarProcessor.onPause();*/
         //   polarhrMeasurements = _polarProcessor.getPolarhrMeasurements();
       // }
      /*  Map<String,ArrayList<Integer>> polaraccxMeasurements;
        polaraccxMeasurements = _polarProcessor.getPolaraccxMeasurements();
        Map<String,ArrayList<Integer>> polaraccyMeasurements;
        polaraccyMeasurements = _polarProcessor.getPolaraccyMeasurements();
        Map<String,ArrayList<Integer>> polaracczMeasurements;
        polaracczMeasurements = _polarProcessor.getPolaracczMeasurements();
        Map<String,ArrayList<Float>> polargyroxMeasurements = null;
        polargyroxMeasurements = _polarProcessor.getPolargyroxMeasurements();
        Map<String,ArrayList<Float>> polargyroyMeasurements;
        polargyroyMeasurements = _polarProcessor.getPolargyroyMeasurements();
        Map<String,ArrayList<Float>> polargyrozMeasurements = null;
        polargyrozMeasurements = _polarProcessor.getPolargyrozMeasurements();
        Map<String,ArrayList<Float>> polarmagnetxMeasurements = null;
        polarmagnetxMeasurements = _polarProcessor.getPolarmagnetxMeasurements();
        Map<String,ArrayList<Float>> polarmagnetyMeasurements = null;
        polarmagnetyMeasurements = _polarProcessor.getPolarmagnetyMeasurements();
        Map<String,ArrayList<Float>> polarmagnetzMeasurements = null;
        polarmagnetzMeasurements = _polarProcessor.getPolarmagnetzMeasurements();
        Map<String,ArrayList<Integer>> polarppg0Measurements = null;
        polarppg0Measurements = _polarProcessor.getPolarppg0Measurements();
        Map<String,ArrayList<Integer>> polarppg1Measurements = null;
        polarppg1Measurements = _polarProcessor.getPolarppg1Measurements();
        Map<String,ArrayList<Integer>> polarppg2Measurements = null;
        polarppg2Measurements = _polarProcessor.getPolarppg2Measurements();
        Map<String,ArrayList<Integer>> polarppgambientMeasurements = null;
        polarppgambientMeasurements = _polarProcessor.getPolarppgambientMeasurements();
        Map<String,ArrayList<Integer>> polarppiMeasurements = null;
        polarppiMeasurements = _polarProcessor.getPolarppiMeasurements();
        Map<String,ArrayList<Boolean>> polarppiblockerMeasurements = null;
        polarppiblockerMeasurements = _polarProcessor.getPolarppiblockerMeasurements();
        Map<String,ArrayList<Integer>> polarppierrorestimateMeasurements;
        polarppierrorestimateMeasurements = _polarProcessor.getPolarppierrorestimateMeasurements(); */

        // Finish any leftover phone sensors:
        _sensorManager.unregisterListener(this);

        set_recordingRightNow(false);

        // Construct an object with all the data:
        JSONObject data = new JSONObject();

        // Add label
        Set<String> labels;
        labels = ESDataFilesAccessor.writeUserLabels(
                mainActivitypublic,secondaryActivitypublic,moodpublic);
        data.put("labels", labels);

        JSONArray labelsJsonArray = new JSONArray();
        String[] userLabels = new String[labels.size()];
        labels.toArray(userLabels);
        for (String userLabel : userLabels) {
            labelsJsonArray.put(userLabel);
        }
        try {
            data.put("labels",labelsJsonArray);
        }
        catch (JSONException e) {
            Log.e(LOG_TAG,"JSON: failed putting label " + ". Message: " + e.getMessage());
        }

        // Add high-frequency data:
        for (String key : _highFreqData.keySet()) {
            JSONArray samples = new JSONArray(_highFreqData.get(key));
            try {
                data.put(key, samples);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON: failed putting key " + key + ". Message: " + e.getMessage());
            }
        }

        // Add Polar data:
        if (polarhrMeasurements != null) {
        for (String key : polarhrMeasurements.keySet()) {
            JSONArray samples = new JSONArray(polarhrMeasurements.get(key));
            try {
                data.put(key,samples);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG,"JSON: failed putting polar hr key " + key + ". Message: " + e.getMessage());
            }
        }}

        /*    if (polaraccxMeasurements != null) {
            for (String key : polaraccxMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polaraccxMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar accx key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polaraccyMeasurements != null) {
            for (String key : polaraccyMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polaraccyMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar accy key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polaracczMeasurements != null) {
            for (String key : polaracczMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polaracczMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar accz key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polargyroxMeasurements != null) {
            for (String key : polargyroxMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polargyroxMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar gyrox key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polargyroyMeasurements != null) {
            for (String key : polargyroyMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polargyroyMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar gyroy key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polargyrozMeasurements != null) {
            for (String key : polargyrozMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polargyrozMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar gyroz key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarmagnetxMeasurements != null) {
            for (String key : polarmagnetxMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarmagnetxMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar magnetx key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarmagnetyMeasurements != null) {
            for (String key : polarmagnetyMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarmagnetyMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar magnety key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarmagnetzMeasurements != null) {
            for (String key : polarmagnetzMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarmagnetzMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar magnetz key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppg0Measurements != null) {
            for (String key : polarppg0Measurements.keySet()) {
                JSONArray samples = new JSONArray(polarppg0Measurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppg0 key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppg1Measurements != null) {
            for (String key : polarppg1Measurements.keySet()) {
                JSONArray samples = new JSONArray(polarppg1Measurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppg1 key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppg2Measurements != null) {
            for (String key : polarppg2Measurements.keySet()) {
                JSONArray samples = new JSONArray(polarppg2Measurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppg2 key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppgambientMeasurements != null) {
            for (String key : polarppgambientMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarppgambientMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppgambient key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppiMeasurements != null) {
            for (String key : polarppiMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarppiMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppi key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppiblockerMeasurements != null) {
            for (String key : polarppiblockerMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarppiblockerMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppiblocker key " + key + ". Message: " + e.getMessage());
                }
            }
        }
        if (polarppierrorestimateMeasurements != null) {
            for (String key : polarppierrorestimateMeasurements.keySet()) {
                JSONArray samples = new JSONArray(polarppierrorestimateMeasurements.get(key));
                try {
                    data.put(key,samples);
                }
                catch (JSONException e) {
                    Log.e(LOG_TAG,"JSON: failed putting polar ppierrorestimate key " + key + ". Message: " + e.getMessage());
                }
            }
        } */

        // Add low-frequency data:
        try {
            data.put(LOW_FREQ, _lowFreqData);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON: failed putting low frequency data. Message: " + e.getMessage());
        }

        // Add location quick features:
        JSONObject locationQuickFeatures = calcLocationQuickFeatures();
        try {
            data.put(LOCATION_QUICK_FEATURES, locationQuickFeatures);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON: failed putting location quick features. Message: " + e.getMessage());
        }

        // Save data to file:
        String dataStr = data.toString();
        writeFile(dataStr);

        // Zip the files:
   //     String zipFilename = createZipFile(dataStr);
   //     Log.i(LOG_TAG, "Created zip file: " + zipFilename);

        // Add this zip file to the network queue:
//        if (zipFilename != null) {
//            ESNetworkAccessor.getESNetworkAccessor().addToUploadQueue(zipFilename);
//        }
        _esApplication.set_userSelectedDataCollectionOn(false);
    }

    private JSONObject calcLocationQuickFeatures() {
        ArrayList<Double> latVals = _locationCoordinatesData.get(LOC_LAT);
        ArrayList<Double> longVals = _locationCoordinatesData.get(LOC_LONG);
        ArrayList<Double> timerefs = _highFreqData.get(LOC_TIME);

        int n = latVals.size();
        if (longVals == null || latVals == null) {
            Log.e(LOG_TAG, "Missing the internally-saved location coordinates.");
            return null;
        }
        if (timerefs == null) {
            Log.e(LOG_TAG, "Missing the time reference of location updates.");
            return null;
        }

        if (longVals.size() != n || timerefs.size() != n) {
            Log.e(LOG_TAG, "Number of longitude values or location timerefs doesn't match number of latitude values");
            return null;
        }
        if (n == 0) {
            return null;
        }

        double sumLat = 0, sumLong = 0, sumSqLat = 0, sumSqLong = 0, sumAbsLatDeriv = 0, sumAbsLongDeriv = 0;
        for (int i = 0; i < n; i++) {
            sumLat += latVals.get(i);
            sumLong += longVals.get(i);
            sumSqLat += Math.pow(latVals.get(i), 2);
            sumSqLong += Math.pow(longVals.get(i), 2);
            if (i > 0) {
                double timeDiff = timerefs.get(i) - timerefs.get(i - 1);
                if (timeDiff > 0) {
                    sumAbsLatDeriv += Math.abs(latVals.get(i) - latVals.get(i - 1)) / timeDiff;
                    sumAbsLongDeriv += Math.abs(longVals.get(i) - longVals.get(i - 1)) / timeDiff;
                } else {
                    sumAbsLatDeriv = -1;
                    sumAbsLongDeriv = -1;
                    break;
                }
            }
        }

        double meanLat = sumLat / n;
        double meanLong = sumLong / n;
        double meanSqLat = sumSqLat / n;
        double meanSqLong = sumSqLong / n;
        double varLat = meanSqLat - Math.pow(meanLat, 2);
        double varLong = meanSqLong - Math.pow(meanLong, 2);

        double meanAbsLatDeriv = (sumAbsLatDeriv < 0) ? -1 : (n > 1 ? sumAbsLatDeriv / (n - 1) : 0);
        double meanAbsLongDeriv = (sumAbsLongDeriv < 0) ? -1 : (n > 1 ? sumAbsLongDeriv / (n - 1) : 0);

        double latStd = Math.sqrt(varLat);
        double longStd = Math.sqrt(varLong);
        double latChange = latVals.get(n - 1) - latVals.get(0);
        double longChange = longVals.get(n - 1) - longVals.get(0);

        Log.d(LOG_TAG, String.format("Calculated location quick features: latChange %f. longChange %f. latStd %f. longStd %f. latDeriv %f. longDerig %f",
                latChange, longChange, latStd, longStd, meanAbsLatDeriv, meanAbsLongDeriv));

        JSONObject locationQuickFeatures = new JSONObject();
        try {
            locationQuickFeatures.put(LOCATION_FEATURE_LAT_CHANGE, latChange);
            locationQuickFeatures.put(LOCATION_FEATURE_LONG_CHANGE, longChange);
            locationQuickFeatures.put(LOCATION_FEATURE_STD_LAT, latStd);
            locationQuickFeatures.put(LOCATION_FEATURE_STD_LONG, longStd);
            locationQuickFeatures.put(LOCATION_FEATURE_LAT_DERIV, meanAbsLatDeriv);
            locationQuickFeatures.put(LOCATION_FEATURE_LONG_DERIV, meanAbsLongDeriv);

            return locationQuickFeatures;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON: failed putting feature into location quick features. Message: " + e.getMessage());
            return null;
        }
    }


    private String createZipFile(String highFreqDataStr) {
        String zipFilename = currentZipFilename();
        try {
            File zipFile = new File(ESDataFilesAccessor.getLabelFilesDir(), zipFilename);
            OutputStream os = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));

            // Add the data files:
            // The high frequency measurements data:
            zos.putNextEntry(new ZipEntry(currentZipFilename()+HIGH_FREQ_DATA_FILENAME));
            zos.write(highFreqDataStr.getBytes());
            zos.closeEntry();

            // Close the zip:
            zos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }

        return zipFilename;
    }

    private void writeFile(String content) {
        FileOutputStream fos;
        try {
            File outFile = new File(ESDataFilesAccessor.getLabelFilesDir(), currentZipFilename() + ESSensorManager.HIGH_FREQ_DATA_FILENAME);
            fos = new FileOutputStream(outFile);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

    }

    /**
     * Get a File object for the (possibly non-existing) zip file referring to the record with the given timestamp.
     * @param timestamp The timestamp identifying the record of interest
     * @return
     */
    public static File getZipFileForRecord(ESTimestamp timestamp) throws IOException {
        return new File(ESDataFilesAccessor.getLabelFilesDir(), getZipFilename(timestamp));
    }

    public static String getZipFilename(ESTimestamp timestamp) {
        return timestamp.toString() + "-" + ESSettings.uuid();
    }

    private String currentZipFilename() {
        return getZipFilename(_timestamp);
    }

    private boolean checkIfShouldFinishSession() {
        ESTimestamp now = new ESTimestamp();
        int timeRecording = now.differenceInSeconds(_timestamp);
        if (timeRecording >= (validForHowManyMinutespublic*60)) {
            Log.d(LOG_TAG, "Finishing this recording because it is already too long, num seconds: " + timeRecording);
            return true;
        }
        return false;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    private void collectPhoneStateMeasurements() {
        // Wifi connectivity:
        try {
            _lowFreqData.put(WIFI_STATUS, ESNetworkAccessor.getESNetworkAccessor().isThereWiFiConnectivity());
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        // On-the-phone:
        TelephonyManager telephonyManager = (TelephonyManager) ESApplication.getTheAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        boolean onThePhone = (telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE);
        try {
            _lowFreqData.put(ON_THE_PHONE, onThePhone);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        // Battery:
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = ESApplication.getTheAppContext().registerReceiver(null, intentFilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        try {
            _lowFreqData.put(BATTERY_STATE, getStringValueForBatteryStatus(status));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        try {
            _lowFreqData.put(BATTERY_PLUGGED, getStringValueForBatteryPlugged(plugged));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        int batteryLevelInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int batteryScaleInt = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (batteryLevelInt != -1 && batteryScaleInt != -1) {
            double batteryLevel = (double) batteryLevelInt / (double) batteryScaleInt;
            try {
                _lowFreqData.put(BATTERY_LEVEL, batteryLevel);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        // Ringer:
        AudioManager audioManager = (AudioManager) ESApplication.getTheAppContext().getSystemService(Context.AUDIO_SERVICE);
        try {
            _lowFreqData.put(RINGER_MODE, getStringValueForRingerMode(audioManager.getRingerMode()));
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        // Time:
        try {
            String timeZoneName = TimeZone.getDefault().getDisplayName(false, TimeZone.LONG);
            // Date&Time in ISO8601 format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);
            String formattedDate = sdf.format(new Date());

            int hour = _timestamp.getHourOfDayOutOf24();
            int minute = _timestamp.getMinuteOfHour();

            long unixTime = System.currentTimeMillis();

            _lowFreqData.put(HOUR_OF_DAY, hour);
            _lowFreqData.put(MINUTE_IN_HOUR, minute);
            _lowFreqData.put(TIMEZONE_LONG_NAME, timeZoneName);
            _lowFreqData.put(DATE_TIME_ISO8601, formattedDate);
            _lowFreqData.put(UNIX_TIME, unixTime);

            Log.d(LOG_TAG, "== Timestamp " + _timestamp + ": " + _timestamp.infoString());
            Log.d(LOG_TAG, "== Timestamp hour: " + hour + ". minute: " + minute);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    // Implementing the SensorEventListener interface:
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Sanity check: we shouldn't be recording now:
        if (!is_recordingRightNow()) {
            Log.e(LOG_TAG, "!!! We're not in a recording session (maybe finished recently) but got a sensor event for: " + event.sensor.getName());
            return;
        }
        boolean sensorCollectedEnough = false;
        double timestampSeconds = ((double) event.timestamp) / NANOSECONDS_IN_SECOND;
        double timestampMilliSeconds = ((double) event.timestamp) / NANOSECONDS_IN_MILLISECOND;
        long unixTime = System.currentTimeMillis();

        try {

            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    if (_highFreqData == null) {
                        Log.e(LOG_TAG, "Add null values");
                        addHighFrequencyMeasurement(RAW_ACC_X, null);
                        addHighFrequencyMeasurement(RAW_ACC_Y, null);
                        addHighFrequencyMeasurement(RAW_ACC_Z, null);
                        addHighFrequencyMeasurement(RAW_ACC_UNIX_TIME, null);
                        addHighFrequencyMeasurement(RAW_ACC_TIME_MS, null);
                        addHighFrequencyMeasurement(RAW_ACC_TIME_S, null);
                    } else {
                    addHighFrequencyMeasurement(RAW_ACC_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(RAW_ACC_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(RAW_ACC_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(RAW_ACC_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(RAW_ACC_TIME_MS, timestampMilliSeconds);
                    //sensorCollectedEnough = addHighFrequencyMeasurement(RAW_ACC_TIME_S, timestampSeconds);
                        addHighFrequencyMeasurement(RAW_ACC_TIME_S, timestampSeconds);
                    }
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    if (_highFreqData == null) {
                        Log.e(LOG_TAG, "Add null values");
                        addHighFrequencyMeasurement(RAW_MAGNET_X, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_Y, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_Z, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_UNIX_TIME, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_TIME_MS, null);
                        addHighFrequencyMeasurement(RAW_MAGNET_TIME_S, null);
                    } else {
                    addHighFrequencyMeasurement(RAW_MAGNET_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(RAW_MAGNET_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(RAW_MAGNET_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_X, (double) event.values[3]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Y, (double) event.values[4]);
                    addHighFrequencyMeasurement(RAW_MAGNET_BIAS_Z, (double) event.values[5]);
                    addHighFrequencyMeasurement(RAW_MAGNET_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(RAW_MAGNET_TIME_MS, timestampMilliSeconds);
                    //sensorCollectedEnough = addHighFrequencyMeasurement(RAW_MAGNET_TIME_S, timestampSeconds);
                        addHighFrequencyMeasurement(RAW_MAGNET_TIME_S, timestampSeconds);}
                    break;
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    if (_highFreqData == null) {
                        Log.e(LOG_TAG, "Add null values");
                        addHighFrequencyMeasurement(RAW_GYRO_X, null);
                        addHighFrequencyMeasurement(RAW_GYRO_Y, null);
                        addHighFrequencyMeasurement(RAW_GYRO_Z, null);
                        addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X, null);
                        addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y, null);
                        addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z, null);
                        addHighFrequencyMeasurement(RAW_GYRO_UNIX_TIME, null);
                        addHighFrequencyMeasurement(RAW_GYRO_TIME_MS, null);
                        addHighFrequencyMeasurement(RAW_GYRO_TIME_S, null);
                    } else {
                    addHighFrequencyMeasurement(RAW_GYRO_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(RAW_GYRO_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(RAW_GYRO_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_X, (double) event.values[3]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Y, (double) event.values[4]);
                    addHighFrequencyMeasurement(RAW_GYRO_DRIFT_Z, (double) event.values[5]);
                    addHighFrequencyMeasurement(RAW_GYRO_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(RAW_GYRO_TIME_MS, timestampMilliSeconds);
                    //sensorCollectedEnough = addHighFrequencyMeasurement(RAW_GYRO_TIME_S, timestampSeconds);}
                    addHighFrequencyMeasurement(RAW_GYRO_TIME_S, timestampSeconds);}
                    break;
                case Sensor.TYPE_GRAVITY:
                    addHighFrequencyMeasurement(PROC_GRAV_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(PROC_GRAV_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(PROC_GRAV_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(PROC_GRAV_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(PROC_GRAV_TIME_MS, timestampMilliSeconds);
                   // sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GRAV_TIME_S, timestampSeconds);
                    addHighFrequencyMeasurement(PROC_GRAV_TIME_S, timestampSeconds);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    addHighFrequencyMeasurement(PROC_ACC_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(PROC_ACC_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(PROC_ACC_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(PROC_ACC_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(PROC_ACC_TIME_MS, timestampMilliSeconds);
                    //sensorCollectedEnough = addHighFrequencyMeasurement(PROC_ACC_TIME_S, timestampSeconds);
                    addHighFrequencyMeasurement(PROC_ACC_TIME_S, timestampSeconds);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    addHighFrequencyMeasurement(PROC_MAGNET_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(PROC_MAGNET_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(PROC_MAGNET_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(PROC_MAGNET_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(PROC_MAGNET_TIME_MS, timestampMilliSeconds);
                  //  sensorCollectedEnough = addHighFrequencyMeasurement(PROC_MAGNET_TIME_S, timestampSeconds);
                    addHighFrequencyMeasurement(PROC_MAGNET_TIME_S, timestampSeconds);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    addHighFrequencyMeasurement(PROC_GYRO_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(PROC_GYRO_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(PROC_GYRO_Z, (double) event.values[2]);
                    addHighFrequencyMeasurement(PROC_GYRO_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(PROC_GYRO_TIME_MS, timestampMilliSeconds);
                   // sensorCollectedEnough = addHighFrequencyMeasurement(PROC_GYRO_TIME_S, timestampSeconds);
                    addHighFrequencyMeasurement(PROC_GYRO_TIME_S, timestampSeconds);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    addHighFrequencyMeasurement(PROC_ROTATION_X, (double) event.values[0]);
                    addHighFrequencyMeasurement(PROC_ROTATION_Y, (double) event.values[1]);
                    addHighFrequencyMeasurement(PROC_ROTATION_Z, (double) event.values[2]);
//                addHighFrequencyMeasurement(PROC_ROTATION_COS,event.values[4]);
//                addHighFrequencyMeasurement(PROC_ROTATION_ACCURACY,event.values[5]);
                    addHighFrequencyMeasurement(PROC_ROTATION_UNIX_TIME, (double) unixTime);
                    addHighFrequencyMeasurement(PROC_ROTATION_TIME_MS, timestampMilliSeconds);
                 //   sensorCollectedEnough = addHighFrequencyMeasurement(PROC_ROTATION_TIME_S, timestampSeconds);
                    addHighFrequencyMeasurement(PROC_ROTATION_TIME_S, timestampSeconds);
                    break;
                // Low frequency (one-time) sensors:
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    _lowFreqData.put(TEMPERATURE_AMBIENT, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_LIGHT:
                    _lowFreqData.put(LIGHT, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_PRESSURE:
                    _lowFreqData.put(PRESSURE, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_PROXIMITY:
                    _lowFreqData.put(PROXIMITY, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    _lowFreqData.put(HUMIDITY, event.values[0]);
                    sensorCollectedEnough = true;
                    break;
                default:
                    Log.e(LOG_TAG, "Got event from unsupported sensor with type " + event.sensor.getType());
            }
         /*   ESTimestamp now = new ESTimestamp();
            int timeRecording = now.differenceInSeconds(_timestamp);
            if (timeRecording >= (validForHowManyMinutespublic*60)) {
                Log.d(LOG_TAG, "Finishing this recording because it is already too long, num seconds: " + timeRecording);
                finishSession();
            }*/
            finishIfTooMuchTimeRecording();
            if (sensorCollectedEnough) {
                // Then we've collected enough samples from accelerometer,
                // and we can stop listening to it.
                Log.d(LOG_TAG, "=========== unregistering sensor: " + event.sensor.getName());
                _sensorManager.unregisterListener(this, event.sensor);
                finishSessionIfReady();
            }

        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem adding sensor measurement to json object. " + event.sensor.toString());
            e.printStackTrace();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(LOG_TAG, "google api: connected");
        @SuppressLint("RestrictedApi") LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_MILLIS);
        locationRequest.setFastestInterval(LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(_googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG,"google api connection suspended. " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG,"google api connection failed. result: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        double timerefSeconds;
        timerefSeconds = ((double) location.getElapsedRealtimeNanos()) / NANOSECONDS_IN_SECOND;
        Log.d(LOG_TAG,"got location update with time reference: " + timerefSeconds);

        addHighFrequencyMeasurement(LOC_TIME, timerefSeconds);
        // Should we send the exact coordinates?
        if ((!ESSettings.shouldUseLocationBubble()) ||
                (ESSettings.locationBubbleCenter() == null) ||
                (ESSettings.locationBubbleCenter().distanceTo(location) > LOCATION_BUBBLE_RADIUS_METERS)) {
            Log.i(LOG_TAG, "Sending location coordinates");
            addHighFrequencyMeasurement(LOC_LAT, location.getLatitude());
            addHighFrequencyMeasurement(LOC_LONG,location.getLongitude());
        }
        else {
            Log.i(LOG_TAG,"Hiding location coordinates (sending invalid coordinates). We're in the bubble.");
            addHighFrequencyMeasurement(LOC_LAT,LOC_LAT_HIDDEN);
            addHighFrequencyMeasurement(LOC_LONG,LOC_LONG_HIDDEN);
        }
        // Anyway, store the location coordinates separately:
        _locationCoordinatesData.get(LOC_LAT).add(location.getLatitude());
        _locationCoordinatesData.get(LOC_LONG).add(location.getLongitude());

        addHighFrequencyMeasurement(LOC_HOR_ACCURACY,location.hasAccuracy() ? location.getAccuracy() : LOC_ACCURACY_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_ALT,location.hasAltitude() ? location.getAltitude() : LOC_ALT_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_SPEED,location.hasSpeed() ? location.getSpeed() : LOC_SPEED_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_BEARING,location.hasBearing() ? location.getBearing() : LOC_BEARING_UNAVAILABLE);
    }
}
