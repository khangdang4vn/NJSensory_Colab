package edu.ucsd.calab.extrasensory;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity;
import edu.ucsd.calab.extrasensory.ui.FeedbackActivity;
import edu.ucsd.calab.extrasensory.ui.MainActivity;

/**
 * This class serves as a central location to manage various aspects of the application,
 * and as a shared place to be reached from all the components of the application.
 *
 * Created by Yonatan on 1/15/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESApplication extends Application {

    public static final long MILLISECONDS_IN_MINUTE = 1000*60;
    public static final String ACTION_ALERT_ACTIVE_FEEDBACK = "edu.ucsd.calab.extrasensory.action.ALERT_ACTIVE_FEEDBACK";
    public static final String ACTION_ALERT_PAST_FEEDBACK = "edu.ucsd.calab.extrasensory.action.ALERT_PAST_FEEDBACK";
    public static final String NOTIFICATION_TEXT_NO_VERIFIED = "Can you please report what you are doing?";

    public static final String KEY_TIMEINSECONDS_NOTIFICATION = "edu.ucsd.calab.extrasensory.extra_key.notification_timestamp";
    public static final String KEY_USER_RESPOND_TO_NOTIFICATION_TIMESTAPMS = "edu.ucsd.calab.extrasensory.extra_key.user_respond_to_notification_timestamp";


    private static final String LOG_TAG = "[ESApplication]";
    private static final long RECENT_TIME_PERIOD_IN_MILLIS = 20*ESApplication.MILLISECONDS_IN_MINUTE;
    private static final int NOTIFICATION_ID = 2;
    private static final String NOTIFICATION_TITLE = "NJSensory";
    private static final long WAIT_BEFORE_START_FIRST_RECORDING_MILLIS = 4000;
    private static final long RECORDING_SESSIONS_INTERVAL_MILLIS = 1000L;
    public static int validForHowManyMinutespublic;
    private static final String ZIP_DIR_NAME = "zip";
    private static final String DATA_DIR_NAME = "data";
    private static final String FEEDBACK_DIR_NAME = "feedback";

    public static final String CHANNEL_ID = "BackgroundServiceChannel";

    //private static final String ZIP_DIR_NAME = "extrasensory.zip." + ESSettings.uuid().substring(0,8);

    private static Context _appContext;

    public static Context getTheAppContext() {
        return _appContext;
    }

    public static class PredeterminedLabels {
        private ESLabelStruct _labels = null;
        private boolean _initiatedByNotification = false;
        private boolean _startedFirstActivityRecording = false;
        private ESTimestamp _validUntil = new ESTimestamp(0);
        private ESTimestamp _timestampOpenFeedbackForm = null;
        private ESTimestamp _timestampPressSendButton = null;
        private ESTimestamp _timestampNotification = null;
        private ESTimestamp _timestampUserRespondToNotification = null;

        private void clearLabels() {
            _labels = null;
            _initiatedByNotification = false;
            _startedFirstActivityRecording = false;
            _validUntil = new ESTimestamp(0);

            _timestampOpenFeedbackForm = null;
            _timestampPressSendButton = null;
            _timestampNotification = null;
            _timestampUserRespondToNotification = null;
        }

        public ESLabelStruct getLabels() {
            if (_labels == null || _validUntil == null || _validUntil.isEarlierThan(new ESTimestamp())) {
                // Then there are no predetermined labels, or those that are here are no longer valid:
                clearLabels();
            }
            return _labels;
        }

        boolean is_initiatedByNotification() { return _initiatedByNotification; }

        boolean is_startedFirstActivityRecording() {
            return _startedFirstActivityRecording;
        }

        ESTimestamp get_timestampOpenFeedbackForm() { return _timestampOpenFeedbackForm; }
        ESTimestamp get_timestampPressSendButton() { return _timestampPressSendButton; }
        ESTimestamp get_timestampNotification() { return _timestampNotification; }
        ESTimestamp get_timestampUserRespondToNotification() { return _timestampUserRespondToNotification; }

        void set_startedFirstActivityRecording(boolean startedFirstActivityRecording) {
            _startedFirstActivityRecording = startedFirstActivityRecording;
        }

        private void setPredeterminedLabels(ESLabelStruct labels,int validForHowManyMinutes,boolean initiatedByNotification,
                                            ESTimestamp timestampOpenFeedbackForm, ESTimestamp timestampPressSendButton,
                                            ESTimestamp timestampNotification, ESTimestamp timestampUserRespondToNotification) {
            _labels = new ESLabelStruct(labels);
            _initiatedByNotification = initiatedByNotification;
            _startedFirstActivityRecording = false;

            long gracePeriod = (long)(1.5*MILLISECONDS_IN_MINUTE);
            Date validUntilDate = new Date(new Date().getTime() + validForHowManyMinutes*MILLISECONDS_IN_MINUTE + gracePeriod);
            _validUntil = new ESTimestamp(validUntilDate);

            _timestampOpenFeedbackForm = timestampOpenFeedbackForm;
            _timestampPressSendButton = timestampPressSendButton;
            _timestampNotification = timestampNotification;
            _timestampUserRespondToNotification = timestampUserRespondToNotification;
        }
    }

    static PredeterminedLabels _predeterminedLabels = new PredeterminedLabels();

    public static class DataForAlertForPastFeedback {
        private ESActivity _latestVerifiedActivity;
        private ESTimestamp _untilTimestamp;
        private String _question;

        private DataForAlertForPastFeedback(ESActivity latestVerifiedActivity,ESTimestamp untilTimestamp,String question) {
            _latestVerifiedActivity = latestVerifiedActivity;
            _untilTimestamp = untilTimestamp;
            _question = question;
        }
        public ESActivity get_latestVerifiedActivity() {
            return _latestVerifiedActivity;
        }
        public ESTimestamp get_untilTimestamp() {
            return _untilTimestamp;
        }
        public String get_question() {
            return _question;
        }
    }

    public static File getZipDir() {
        return getTheAppContext().getDir(ZIP_DIR_NAME, Context.MODE_PRIVATE);
    }

    //private static final Context CONTEXT = ESApplication.getTheAppContext();
    //public static File getZipDir() throws IOException {
      //  String state = Environment.getExternalStorageState();
        //if (!Environment.MEDIA_MOUNTED.equals(state)) {
          //  Log.e(LOG_TAG,"!!! External storage is not mounted.");
            //throw new IOException("External storage is not mounted.");
        //}

        //File dataFilesDir = new File(CONTEXT.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),ZIP_DIR_NAME);
//        File dataFilesDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),LABEL_DATA_DIRNAME);
        //if (!dataFilesDir.exists()) {
            // Create the directory:
          //  if (!dataFilesDir.mkdirs()) {
            //    Log.e(LOG_TAG,"!!! Failed creating directory: " + dataFilesDir.getPath());
              //  throw new IOException("Failed creating directory: " + dataFilesDir.getPath());
            //}
        //}

        //return dataFilesDir;
    //}

    public static File get_DataDir() {
        return getTheAppContext().getDir(DATA_DIR_NAME, Context.MODE_PRIVATE);
    }

    public static File getFeedbackDir() {
        return getTheAppContext().getDir(FEEDBACK_DIR_NAME, Context.MODE_PRIVATE);
    }

    private ESSensorManager _sensorManager;
    private PolarActivity _polarProcessor;
    private AlarmManager _alarmManager;
    private boolean _userSelectedDataCollectionOn = false;
    private ESLifeCycleCallback _lifeCycleMonitor = new ESLifeCycleCallback();
    private DataForAlertForPastFeedback _dataForAlertForPastFeedback;

    public DataForAlertForPastFeedback get_dataForAlertForPastFeedback() {
        return _dataForAlertForPastFeedback;
    }
    public void clearDataForAlertForPastFeedback() {
        _dataForAlertForPastFeedback = null;
    }

    private BroadcastReceiver _broadcastReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED.equals(intent.getAction())) {
                // Check if we're on the limit of storage capacity: we may need to turn on/off data collection:
                int qsize = ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize();
                if (qsize >= ESSettings.maxStoredExamples()-1) {
                    checkShouldWeCollectDataAndManageAppropriately();
                }
            }
        }
    };

    /**
     * Is data collection "on" now, according to the user decision (not according to storage limiation)
     * @return
     */
    public boolean is_userSelectedDataCollectionOn() {
        return _userSelectedDataCollectionOn;
    }

    /**
     * Mark the user input for allowing/stopping data collection now.
     * @param userSelectedDataCollectionOn
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void set_userSelectedDataCollectionOn(boolean userSelectedDataCollectionOn) {
        if (_userSelectedDataCollectionOn == userSelectedDataCollectionOn) {
            // Then there is nothing to do:
            return;
        }

        _userSelectedDataCollectionOn = userSelectedDataCollectionOn;
        checkShouldWeCollectDataAndManageAppropriately();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void turnDataCollectionOff() {
        stopCurrentRecordingAndRecordingSchedule();
        stopNotificationSchedule();
        if (_polarProcessor.isPolarConnected()) {
            Log.d(LOG_TAG, "Polar device connected, so calling pause Polar activity");
           // _polarProcessor.onPause();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void turnDataCollectionOn() {
        Log.i(LOG_TAG, "Turning data collection on (resume polar activity if applicable).");
        if (_polarProcessor.isPolarConnected()) {
            Log.d(LOG_TAG, "Polar device connected, so calling resume Polar activity");
           // _polarProcessor.onResume();
        }
        startRecordingSchedule();
        startNotificationSchedule();
    }

    public boolean shouldDataCollectionBeOn() {
        if (!_userSelectedDataCollectionOn) {
            // Then user doesn't allow data collection right now:
            return false;
        }
        if (ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize() >= ESSettings.maxStoredExamples()) {
            // Then we're already storing max capacity and we shouldn't collect more data:
            return false;
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkShouldWeCollectDataAndManageAppropriately() {
        if (shouldDataCollectionBeOn()) {
            turnDataCollectionOn();
        }
        else {
            turnDataCollectionOff();
        }
    }

    public static boolean debugMode() { return false; }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "Application being created.");
        _appContext = getApplicationContext();
        registerActivityLifecycleCallbacks(_lifeCycleMonitor);

        _sensorManager = ESSensorManager.getESSensorManager();
        _alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        _polarProcessor = PolarActivity.getPolarProcessor();
        assert _polarProcessor != null;
        _polarProcessor.setTheESApplicationReference(this);
        if (_polarProcessor.isPolarConnected()) {
          //  _polarProcessor.onResume();
        }

        LocalBroadcastManager.getInstance(_appContext).registerReceiver(_broadcastReceiver,new IntentFilter(ESNetworkAccessor.BROADCAST_NETWORK_QUEUE_SIZE_CHANGED));

        // Start the scheduling of periodic recordings:
        startRecordingSchedule();
        // Start the notification schedule:
        startNotificationSchedule();
        // Create the notification schedule:
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "NJSensory is running in the background",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    /**
     * Start user initiated recording (and schedule).
     * This function first stops any current recording (if there is one), and stops the recording schedule,
     * then starts a new recording schedule.
     * The given labels are used for the started recording from now until the end time specified by the user.
     *  @param labelsToAssign - The labels to assign to the following activities
     * @param validForHowManyMinutes - How many minutes should the given labels be automatically assigned?
     * @param initiatedByNotification - Was this active feedback initiated by a notification/reminder?
     * @param timestampOpenFeedbackForm The timestamp of the time the user opened the feedback form for this activity (and actually sent feedback), or null in case this feedback did not involve the feedback form (e.g. confirmation-notification)
     * @param timestampPressSendButton The timestamp of the time the user pressed the "send feedback" button for this activity, or null in case this feedback did not involve the feedback form (e.g. confirmation-notification)
     * @param timestampNotification The timestamp of the time the notification showed, which eventually yielded this activity's update, or null if this feedback was not initiated by notification
     * @param timestampUserRespondToNotification The timestamp of the time the user responded to the notification by pressing an answer button, which yielded this activity's update, or null if this feedback was not initiated by notification
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void startActiveFeedback(ESLabelStruct labelsToAssign, int validForHowManyMinutes, boolean initiatedByNotification,
                                    ESTimestamp timestampOpenFeedbackForm, ESTimestamp timestampPressSendButton,
                                    ESTimestamp timestampNotification, ESTimestamp timestampUserRespondToNotification) {
        _predeterminedLabels.setPredeterminedLabels(labelsToAssign, validForHowManyMinutes,initiatedByNotification,
                timestampOpenFeedbackForm,timestampPressSendButton,timestampNotification,timestampUserRespondToNotification);
        stopCurrentRecordingAndRecordingSchedule();
        startRecordingSchedule();
    }

    /**
     * Start a repeating schedule of recording sessions (every 1 minute).
     *
     //* @param millisWaitBeforeStart - time to wait (milliseconds) before the first recording session in this schedule.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startRecordingSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntent = createESPendingIntent(ESIntentService.ACTION_START_RECORDING);
        // Before registering the repeating alarm, make sure that any similar alarm is canceled:
        _alarmManager.cancel(pendingIntent);

        Log.i(LOG_TAG,"Scheduling the recording sessions with no interval.");
        _alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startNotificationSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntent = createESPendingIntent(ESIntentService.ACTION_NOTIFICATION_CHECKUP);
        // Before registering the repeating alarm, make sure that any similar alarm is canceled:
        _alarmManager.cancel(pendingIntent);
        // Check whether we should start a new notification schedule or simply leave it canceled:
        if (!ESSettings.useAnyTypeOfNotifications()) {
            Log.d(LOG_TAG, "Requested to not use notifications, so not starting notification schedule.");
            return;
        }

        int notificationIntervalSeconds = ESSettings.notificationIntervalInSeconds();
        long notificationIntervalMillis = 1000*7200;
        Log.i(LOG_TAG,"Scheduling the notification schedule with interval " + notificationIntervalSeconds + " seconds.");
        _alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + notificationIntervalMillis,
                notificationIntervalMillis,
                pendingIntent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private PendingIntent createESPendingIntent(String action) {
        Intent intent = new Intent(getApplicationContext(),ESIntentService.class);
        intent.setAction(action);

        return PendingIntent.getService(getApplicationContext(),0,intent,PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Stop the current recording session (if one exists),
     * and cancel the periodic schedule of recording sessions.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void stopCurrentRecordingAndRecordingSchedule() {
        stopRecordingSchedule();
        stopCurrentRecording();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void stopRecordingSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntentReference = createESPendingIntent(ESIntentService.ACTION_START_RECORDING);
        _alarmManager.cancel(pendingIntentReference);
        Log.i(LOG_TAG,"Stopped the repeated recording schedule.");
    }

    private void stopCurrentRecording() {
        Log.i(LOG_TAG,"Stopping current recording session.");
        _sensorManager.stopRecordingSensors();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void stopNotificationSchedule() {
        if (_alarmManager == null) {
            Log.e(LOG_TAG,"Alarm manager is null");
            return;
        }

        PendingIntent pendingIntentReference = createESPendingIntent(ESIntentService.ACTION_NOTIFICATION_CHECKUP);
        _alarmManager.cancel(pendingIntentReference);
        Log.i(LOG_TAG,"Stopped the repeated notification schedule.");
    }

    private boolean isAppInForeground() {
        if (_lifeCycleMonitor == null) {
            return false;
        }
        else {
            return _lifeCycleMonitor.isAppInForeground();
        }
    }

    /**
     * Perform a checkup to see if it's time for user notification.
     * If it's time, trigger the notification.
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    public void notificationCheckup() {

        if (!shouldDataCollectionBeOn()) {
            Log.i(LOG_TAG,"Notification: data collection should be off. Not doing notification.");
            checkShouldWeCollectDataAndManageAppropriately();
            return;
        }

        Log.i(LOG_TAG,"Notification: checkup.");
        Date now = new Date();
        Date recentTimeAgo = new Date(now.getTime() - RECENT_TIME_PERIOD_IN_MILLIS);
        ESTimestamp lookBackFrom = new ESTimestamp(recentTimeAgo);

        // check if there are currently valid predetermined labels:
        ESTimestamp oneMinuteFromNow = new ESTimestamp(new ESTimestamp().get_secondsSinceEpoch() + 60);
        if (_predeterminedLabels._validUntil.isLaterThan(oneMinuteFromNow)) {
            Log.i(LOG_TAG,"We already have valid labels provided by the user for the near future. So no need to nag user now.");
            return;
        }

        ESActivity latestVerifiedActivity = ESDatabaseAccessor.getESDatabaseAccessor().getLatestVerifiedActivity(lookBackFrom);



//        if (latestVerifiedActivity == null || SelectionFromListActivity.NOT_SURE.equals(latestVerifiedActivity.get_mainActivityUserCorrection())) {
        if (latestVerifiedActivity == null || !latestVerifiedActivity.hasAnyUserReportedLabelsNotDummyLabel()) {
            // Then there hasn't been a verified activity in a long time. Need to call for active feedback
            Log.i(LOG_TAG,"Notification: Latest activity was too long ago. Need to prompt for active feedback.");
            if (!ESSettings.useNearFutureNotifications()) {
                Log.d(LOG_TAG,"Notification: near-future notifications are disabled.");
                return;
            }
            ESTimestamp timestampNotification = new ESTimestamp();
            if (isAppInForeground()) {
                // Don't deal with notifications. Send broadcast to show alert:
                Intent broadcast = new Intent(ACTION_ALERT_ACTIVE_FEEDBACK);
                broadcast.putExtra(KEY_TIMEINSECONDS_NOTIFICATION,timestampNotification.get_secondsSinceEpoch());
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
                manager.sendBroadcast(broadcast);
                return;
            }

            // Then we're in the background and we need to raise user's attention with notification:
            Intent defaultActionIntent = new Intent(this, FeedbackActivity.class);
            defaultActionIntent.putExtra(FeedbackActivity.KEY_INITIATED_BY_NOTIFICATION,true);
            defaultActionIntent.putExtra(KEY_TIMEINSECONDS_NOTIFICATION,timestampNotification.get_secondsSinceEpoch());
            PendingIntent defaultActionPendingIntent = PendingIntent.getActivity(this, 0, defaultActionIntent, 0);

            Notification notification = createNotification(NOTIFICATION_TEXT_NO_VERIFIED,defaultActionPendingIntent);
            Log.d(LOG_TAG,"Created notification: " + notification);
            Log.d(LOG_TAG,String.format("Notification. light: %d. vibrate len: %d",notification.ledARGB,notification.vibrate==null?0:notification.vibrate.length));
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
        else {
            // Then use this verified activity's labels to ask if still doing the same
            Log.i(LOG_TAG,"Notification: Found latest verified activity. Need to ask user if was doing the same until now.");
            if (!ESSettings.useNearPastNotifications()) {
                Log.d(LOG_TAG,"Notification: near-past notifications are disabled.");
                return;
            }
            ESTimestamp nowTimestamp = new ESTimestamp(now);
            long millisPassed = now.getTime() - latestVerifiedActivity.get_timestamp().getDateOfTimestamp().getTime();
            int minutesPassed = 120;
            String question = getAlertQuestion(latestVerifiedActivity,minutesPassed);

            // Prepare the data required for the relevant alert dialog:
            _dataForAlertForPastFeedback = new DataForAlertForPastFeedback(latestVerifiedActivity,nowTimestamp,question);

            // Prepare an intent to be used either inside notification or now in a broadcast.
            Intent intent = new Intent();

            if (isAppInForeground()) {
                // No need to use notification. Send broadcast to display an alert dialog:
                intent.setAction(ACTION_ALERT_PAST_FEEDBACK);
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
                manager.sendBroadcast(intent);
                return;
            }

            // Then we're in the background and we need to raise user's attention with notification:
            intent.setClass(this,MainActivity.class);
            intent.putExtra(KEY_TIMEINSECONDS_NOTIFICATION,nowTimestamp.get_secondsSinceEpoch());
            PendingIntent defaultActionPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

            Notification notification = createNotification(question,defaultActionPendingIntent);
            Log.d(LOG_TAG,"Created notification: " + notification);
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID,notification);
        }
    }

    private static Notification createNotification(String contentText,PendingIntent contentIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getTheAppContext());
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle(NOTIFICATION_TITLE);
        builder.setContentText(contentText);
        builder.setPriority(Notification.PRIORITY_HIGH);
        builder.setCategory(Notification.CATEGORY_EVENT);
        builder.setAutoCancel(true);
        builder.setVibrate(getNotificationVibratePattern());
        builder.setLights(Color.argb(255, 200, 0, 255), 200, 200);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        builder.setContentIntent(contentIntent);

        Notification notification = builder.build();

        return notification;
    }

    private static long[] getNotificationVibratePattern() {
        long beatDur = 100; // Milliseconds
        int[] patternOneCycle = new int[]{1,1,1,1,1,5,1,5};
        int numCycles = 4;

        long[] vibPattern = new long[numCycles*patternOneCycle.length];
        for (int cycle = 0; cycle < numCycles; cycle ++) {
            for (int i = 0; i < patternOneCycle.length; i ++) {
                int pos = cycle*patternOneCycle.length + i;
                vibPattern[pos] = beatDur * patternOneCycle[i];
            }
        }

        return vibPattern;
    }

    private static String getAlertQuestion(ESActivity latestVerifiedActivity,int minutesPassed) {
        String mainActivityStr = latestVerifiedActivity.hasUserReportedNondummyMainLabel() ? latestVerifiedActivity.get_mainActivityUserCorrection() : "";
        String question = "In the past 2 hours were you still " + mainActivityStr;

        String[] secondaries = latestVerifiedActivity.get_secondaryActivities();
        if (secondaries != null && secondaries.length > 0) {
            question += " (" + secondaries[0];
            for (int i = 1; i < secondaries.length; i ++) {
                question += ", " + secondaries[i];
            }
            question += ")";
        }

        String[] moods = latestVerifiedActivity.get_moods();
        if (moods != null && moods.length > 0) {
            question += " feeling " + moods[0];
            for (int i = 1; i < moods.length; i ++) {
                question += ", " + moods[i];
            }
        }

        question += "?";
        return question;
    }


    private static class ESLifeCycleCallback implements ActivityLifecycleCallbacks {

        private static final String LOG_TAG_LIFE_CYCLE = LOG_TAG + "[lifeCycle]";

        private HashMap<String,Integer> _startedActivityCounts = new HashMap<>(3);

        private boolean isAppInForeground() {
            for (String activityName : _startedActivityCounts.keySet()) {
                if (_startedActivityCounts.get(activityName) > 0) {
                    // Then we found an activity that is started, so the app is in foreground:
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @SuppressLint("LongLogTag")
        @Override
        public void onActivityStarted(Activity activity) {
            // Check if app is already in foreground:
            boolean foregroundBeforeActivityStarted = isAppInForeground();

            String activityName = activity.getLocalClassName();
            if (_startedActivityCounts.containsKey(activityName)) {
                _startedActivityCounts.put(activityName,_startedActivityCounts.get(activityName) + 1);
            }
            else {
                _startedActivityCounts.put(activityName,1);
            }

            // Did the app just switch to foreground:
            if (!foregroundBeforeActivityStarted) {
                // Then the app just now switched to foreground with this started activity:
                Log.i(LOG_TAG_LIFE_CYCLE,String.format("App switched to foreground (%s just started)",activityName));
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @SuppressLint("LongLogTag")
        @Override
        public void onActivityStopped(Activity activity) {
            String activityName = activity.getLocalClassName();
            if (!_startedActivityCounts.containsKey(activityName) || _startedActivityCounts.get(activityName) <= 0) {
                Log.e(LOG_TAG_LIFE_CYCLE,"Activity " + activityName + " stopped, but wasn't registered as started.");
                return;
            }

            _startedActivityCounts.put(activityName,_startedActivityCounts.get(activityName) - 1);

            // Is the app now in the background:
            if (!isAppInForeground()) {
                // Then probably the app just now moved to the background, when this activity was stopped:
                Log.i(LOG_TAG_LIFE_CYCLE,String.format("Now background (%s just stopped)",activityName));
//                Notification notification = createNotification("bella",PendingIntent.getActivity(getTheAppContext(),13,new Intent("testing.calab.ucsd.edu"),0));
//                ((NotificationManager)getTheAppContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notification);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }


}
