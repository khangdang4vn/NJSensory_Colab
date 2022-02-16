package edu.ucsd.calab.extrasensory.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTabHost;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.conversationbackup.ArchivesActivity;
import edu.ucsd.calab.extrasensory.conversationbackup.ConversationBackupActivity;
import edu.ucsd.calab.extrasensory.questionnaire.AlarmReceiver;
import edu.ucsd.calab.extrasensory.questionnaire.QuestionActivity;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity;

/**
 * The MainActivity handles the "main page" of the UI (which includes several tab-fragments).
 *
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class MainActivity extends BaseActivity {

    private static final String LOG_TAG = "[MainActivity]";

    private final int MULTIPLE_PERMISSIONS_REQUEST_CODE = 1;

    private static int alarm_code = 1;

    private static final int QUESTIONNAIRE_REQUEST = 2018;

    private AlarmManager _alarmManager;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG,"Creating main activity");
        setContentView(R.layout.activity_main);

        //Button buttonRequest = findViewById(R.id.button);
        //buttonRequest.setOnClickListener(new View.OnClickListener() {
            //@Override
            //public void onClick(View v) {
        if ((ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &
              //  (ContextCompat.checkSelfPermission(MainActivity.this,
              //          Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) &
                (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(MainActivity.this, "You have already granted all permissions!",
                    Toast.LENGTH_SHORT).show();
        } else {
            requestAllPermissions();
        }
        if (!PolarActivity.isPolarConnected()) {
            Log.i(LOG_TAG,"Polar is not connected");
            AlertDialog.Builder builder = new AlertDialog.Builder(this).
                    setIcon(R.drawable.ic_launcher).setMessage("Please connect to a Polar device.").
                    setTitle("NJSensory").setNegativeButton(R.string.ok_button_text, (dialog, which)
                    -> dialog.dismiss());
            builder.create().show();
        }
            //}
        //});

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // Check if current time is smaller than cal time
        // otherwise notifications will come same day.
        //if (System.currentTimeMillis() <= calendar.getTimeInMillis()) {
            if (alarm_code == 1) {
            AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
            Intent questions;
            PendingIntent pendingIntentlongsurvey = PendingIntent.getBroadcast(getApplicationContext(),1, questions = new Intent(getApplicationContext(), QuestionActivity.class),PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntentlongsurvey);
            //you have to pass as an extra the json string.
            questions.putExtra("json_questions", loadQuestionnaireJson());
            startActivityForResult(questions, QUESTIONNAIRE_REQUEST);
            //SyncAlarm(context, calendar.getTimeInMillis());
                alarm_code += 1;
    //    }
    }

        AlarmManager alarmManager=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(getApplicationContext(), AlarmReceiver.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(getApplicationContext(),1,intent,PendingIntent.FLAG_IMMUTABLE);
        if (Build.VERSION.SDK_INT >= 23) {
            // Wakes up the device in Doze Mode
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, 6000, // time in millis
                    pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // Wakes up the device in Idle Mode
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, 6000, pendingIntent);
        } else {
            // Old APIs
            alarmManager.set(AlarmManager.RTC_WAKEUP, 6000, pendingIntent);
        }

        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,6000,300000,pendingIntent);

        //Intent questions = new Intent(getApplicationContext(), QuestionActivity.class);

        FragmentTabHost _fragmentTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        _fragmentTabHost.setup(getApplicationContext(), getSupportFragmentManager(), android.R.id.tabcontent);

        // Add the tabs:
        _fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_home_tag)).setIndicator(getString(R.string.tab_home_indicator)),
                HomeFragment.class, null);
        //_fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_history_tag)).setIndicator(getString(R.string.tab_history_indicator)),
         //       HistoryFragment.class, null);
        _fragmentTabHost.addTab(_fragmentTabHost.newTabSpec(getString(R.string.tab_summary_tag)).setIndicator(getString(R.string.tab_summary_indicator)),
                SummaryFragment.class,null);

        // Set the tab host to respond to tab presses:
        _fragmentTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {

                if (getString(R.string.tab_home_tag).equals(tabId)) {
                    Log.i(LOG_TAG, "User switched to Home tab");
                    HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(tabId);
                    assert homeFragment != null;
                    homeFragment.onResume();
                }
            }
        });
    }

   /* public void startService(View v) {
        String input = editTextInput.getText().toString();
        Intent serviceIntent = new Intent(this, ExampleService.class);
        serviceIntent.putExtra("inputExtra", input);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    public void stopService(View v) {
        Intent serviceIntent = new Intent(this, ExampleService.class);
        stopService(serviceIntent);
    }*/

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestAllPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Arrays.toString(new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                //Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.VIBRATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN}))) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", (dialog, which) -> ActivityCompat.requestPermissions(MainActivity.this,
                            new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    //Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                    Manifest.permission.VIBRATE,
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT,
                                    Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_ADMIN}, MULTIPLE_PERMISSIONS_REQUEST_CODE))
                    .setNegativeButton("cancel", (dialog, which) -> dialog.dismiss())
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            //Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.VIBRATE,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN}, MULTIPLE_PERMISSIONS_REQUEST_CODE);
        }
    }

    /*private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE);
        }
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MULTIPLE_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                //Toast.makeText(this, "Permissions DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG,"onStart");
        displayPastFeedbackAlertIfNeeded(false);
        checkGooglePlay();
    }

    private void checkGooglePlay() {
        int googleServicesResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(ESApplication.getTheAppContext());
        if (googleServicesResult == ConnectionResult.SUCCESS) {
            Log.i(LOG_TAG, "We have google play services");
        }
        else {
            Log.i(LOG_TAG,"We don't have required google play services");
            final PendingIntent pendingIntent = GooglePlayServicesUtil.getErrorPendingIntent(googleServicesResult,this,0);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.need_google_play_services_title);
            builder.setMessage(R.string.need_google_play_services_message);
            builder.setPositiveButton(R.string.set_google_play_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.v(LOG_TAG,"User clicked to go to Google Play Services to update required services");
                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(LOG_TAG,"Failed redirecting user to Google Play");
                        e.printStackTrace();
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.i(LOG_TAG,"User clicked 'cancel' for Google Play");
                }
            });

            Dialog dialog = builder.create();
            dialog.show();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        _optionsMenu = menu;

        checkRecordingStateAndSetRedLight();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.app_home:
                startActivity(new Intent(getApplicationContext(), ConversationBackupActivity.class));
                return true;
          /*  case R.id.app_archives:
                startActivity(new Intent(getApplicationContext(), ArchivesActivity.class));
                return true;*/
          /*  case R.id.polar_device:
                startActivity(new Intent(getApplicationContext(), PolarActivity.class));
                return true;*/
            case R.id.action_settings:
                intent = new Intent(getApplicationContext(),SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_active_feedback:
                // Check should we collect data now:
                if (!getTheESApplication().shouldDataCollectionBeOn()) {
                    Log.i(LOG_TAG,"Active feedback pressed, but data-collection is off");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setIcon(R.drawable.ic_launcher).setMessage(R.string.alert_for_active_feedback_while_data_collection_off).
                            setTitle("NJSensory").setNegativeButton(R.string.ok_button_text,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    break;
                }
                if (!PolarActivity.isPolarConnected()) {
                    Log.i(LOG_TAG,"Active feedback pressed, but Polar device is not connected");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setIcon(R.drawable.ic_launcher).setMessage(R.string.alert_for_active_feedback_while_Polar_disconnected).
                            setTitle("NJSensory").setNegativeButton(R.string.ok_button_text,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    break;
                }
                if (ESSensorManager.getESSensorManager().is_recordingRightNow()) {
                    Log.i(LOG_TAG,"Active feedback pressed, but a recording session is ongoing");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setIcon(R.drawable.ic_launcher).setMessage(R.string.alert_for_active_feedback_while_recording_ongoing).
                            setTitle("NJSensory").setNegativeButton(R.string.ok_button_text,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    break;
                    }
               /* if (PolarActivity.batterylevel < 100) {
                    Log.i(LOG_TAG,"Active feedback pressed, but a recording session is ongoing");
                    AlertDialog.Builder builder = new AlertDialog.Builder(this).
                            setIcon(R.drawable.ic_launcher).setMessage("Your Polar device has a low battery level under 5%. Please charge.").
                            setTitle("NJSensory").setNegativeButton(R.string.ok_button_text,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                    break;*/
                if (PolarActivity.batterylevel < 5) {
                    Toast toast = Toast.makeText(this, "Your Polar device has a low battery level under 5%. Please charge.",Toast.LENGTH_SHORT);
                    toast.show();
                }
                intent = new Intent(getApplicationContext(),FeedbackActivity.class);
                FeedbackActivity.setFeedbackParametersBeforeStartingFeedback(new FeedbackActivity.FeedbackParameters());
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        Intent setIntent = new Intent(Intent.ACTION_MAIN);
        setIntent.addCategory(Intent.CATEGORY_HOME);
        setIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(setIntent);
        if (!PolarActivity.isPolarConnected()) {
            Log.i(LOG_TAG,"Polar is not connected");
            AlertDialog.Builder builder = new AlertDialog.Builder(this).
                    setIcon(R.drawable.ic_launcher).setMessage(R.string.alert_if_Polar_disconnected).
                    setTitle("NJSensory").setNegativeButton(R.string.ok_button_text, (dialog, which)
                    -> dialog.dismiss());
            builder.create().show();
        }
    }

    //json stored in the assets folder. but you can get it from wherever you like.
    private String loadQuestionnaireJson()
    {
        try
        {
            InputStream is = Objects.requireNonNull(getApplicationContext().getAssets().open("questions_longsurvey.json"));
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }


}
