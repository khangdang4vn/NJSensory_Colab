package edu.ucsd.calab.extrasensory;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;
import edu.ucsd.calab.extrasensory.ui.MainActivity;

import static edu.ucsd.calab.extrasensory.ESApplication.CHANNEL_ID;

/**
 * An {@link JobIntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 *
 * This class is to be used to initiate services for the application.
 * These services requests can be handled using alarms that call this class.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESIntentService extends IntentService {

    private static final String LOG_TAG = "[ESApplication]";
    private static final int QUESTIONNAIRE_REQUEST = 2018;

      // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS

    //private static final int NOTIF_ID = 1;

    public static final String ACTION_START_RECORDING = "edu.ucsd.calab.extrasensory.action.START_RECORDING";
    public static final String ACTION_NOTIFICATION_CHECKUP = "edu.ucsd.calab.extrasensory.action.NOTIFICATION_CHECKUP";

    public ESIntentService() {
        super("NJSensory");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("NJSensory is running in the background")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        if (intent == null) {
            Log.e(LOG_TAG,"Got null intent.");
        }

        final String action = intent.getAction();
        if (action == null) {
            Log.e(LOG_TAG,"Got intent with null action.");
        }

        Log.v(LOG_TAG,"Got intent with action: " + action);
        if (ACTION_START_RECORDING.equals(action)) {
            ESActivity newActivity = ESDatabaseAccessor.getESDatabaseAccessor().createNewActivity();
            if (newActivity == null) {
                Log.e(LOG_TAG,"Tried to create new activity but got null");
            }
            ESTimestamp timestamp = newActivity.get_timestamp();
            Log.v(LOG_TAG,"Created new activity record with timestamp: " + timestamp);

            // Check if there are predetermined labels:
            ESLabelStruct predeterminedLabels = ESApplication._predeterminedLabels.getLabels();
            if (predeterminedLabels != null) {
                // Is this the first activity in a sequence initiated by active feedback?
                ESActivity.ESLabelSource labelSource;
                if (ESApplication._predeterminedLabels.is_startedFirstActivityRecording()) {
                    labelSource = ESApplication._predeterminedLabels.is_initiatedByNotification() ?
                            ESActivity.ESLabelSource.ES_LABEL_SOURCE_NOTIFICATION_BLANK :
                            ESActivity.ESLabelSource.ES_LABEL_SOURCE_ACTIVE_START;
                    ESApplication._predeterminedLabels.set_startedFirstActivityRecording(false);
                    Log.i(LOG_TAG,"This new activity is the start of user-initiated activity (active feedback).");
                }
                else {
                    labelSource = ESActivity.ESLabelSource.ES_LABEL_SOURCE_ACTIVE_CONTINUE;
                    Log.i(LOG_TAG,"This new activity is the continue of active feedback.");
                }
                // Set the labels for the newly created activity:
                ESDatabaseAccessor.getESDatabaseAccessor().setESActivityUserCorrectedValuesAndPossiblySendFeedback(
                        newActivity,labelSource,
                        predeterminedLabels._mainActivity,predeterminedLabels._secondaryActivities,
                        predeterminedLabels._moods,
                        ESApplication._predeterminedLabels.get_timestampOpenFeedbackForm(),
                        ESApplication._predeterminedLabels.get_timestampPressSendButton(),
                        ESApplication._predeterminedLabels.get_timestampNotification(),
                        ESApplication._predeterminedLabels.get_timestampUserRespondToNotification(),
                        false);
                Log.i(LOG_TAG,"Applied predetermined labels to new activity.");
                // Now start recording:
                ESSensorManager.getESSensorManager().startRecordingSensors(timestamp);
            }
            else {
                Log.i(LOG_TAG,"This new activity has no predetermined labels.");
            }

            // Now start recording:
            //ESSensorManager.getESSensorManager().startRecordingSensors(timestamp);
        }
        else if (ACTION_NOTIFICATION_CHECKUP.equals(action)) {
            Log.i(LOG_TAG, "Got intent for notification checkup");
            ((ESApplication)getApplication()).notificationCheckup();
        }
        else {
            Log.e(LOG_TAG,"Got intent for unsupported action: " + action);
        }

        return START_NOT_STICKY;
    }

    protected void onHandleIntent(Intent intent) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
