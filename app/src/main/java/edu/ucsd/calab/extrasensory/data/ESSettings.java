package edu.ucsd.calab.extrasensory.data;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.InputMismatchException;

import edu.ucsd.calab.extrasensory.ESApplication;

/**
 * This class represents the global settings for the app,
 * some of which have to be fixed (e.g. UUID is set once and stays fixed) and some are mutable by the user interface.
 *
 * Created by Yonatan on 2/4/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class ESSettings {

    private static final String LOG_TAG = "[ESSettings]";

    private String _uuid;
    private int _maxStoredExamples;
    private boolean _useNearPastNotifications;
    private boolean _useNearFutureNotifications;
    private int _notificationIntervalInSeconds;
    private int _numExamplesStoreBeforeSend;
    private boolean _allowCellular;
    private boolean _locationBubbleUsed;
    private Location _locationBubbleCenter;
    private String _classifierType;
    private String _classifierName;
    private boolean _recordAudio;
    private boolean _recordLocation;
    private boolean _recordWatch;
    private ArrayList<Integer> _hfSensorTypesToRecord;
    private ArrayList<Integer> _lfSensorTypesToRecord;
    private boolean _savePredictionFiles;
    private boolean _saveUserLabelsFiles;
    private int _historyTimeUnitInMinutes;

    ESSettings(String uuid,int maxStoredExamples,
               boolean useNearPastNotifications,boolean useNearFutureNotifications,int notificationIntervalInSeconds,
               int numExamplesStoreBeforeSend,
               boolean allowCellular,
               boolean locationBubbleUsed, Location locationBubbleCenter,
               String classifierType,String classifierName,
               boolean recordAudio,boolean recordLocation,boolean recordWatch,
               ArrayList<Integer> hfSensorTypesToRecord,ArrayList<Integer> lfSensorTypesToRecord,
               boolean savePredictionFiles,boolean saveUserLabelsFiles,
               int historyTimeUnitInMinutes) {
        _uuid = uuid;
        _maxStoredExamples = maxStoredExamples;
        _useNearPastNotifications = useNearPastNotifications;
        _useNearFutureNotifications = useNearFutureNotifications;
        _notificationIntervalInSeconds = notificationIntervalInSeconds;
        _numExamplesStoreBeforeSend = numExamplesStoreBeforeSend;
        _allowCellular = allowCellular;
        _locationBubbleUsed = locationBubbleUsed;
        _locationBubbleCenter = locationBubbleCenter;
        _classifierType = classifierType != null ? classifierType : "";
        _classifierName = classifierName != null ? classifierName : "";
        _recordAudio = recordAudio;
        _recordLocation = recordLocation;
        _recordWatch = recordWatch;
        _hfSensorTypesToRecord = hfSensorTypesToRecord != null ? hfSensorTypesToRecord : new ArrayList<Integer>();
        _lfSensorTypesToRecord = lfSensorTypesToRecord != null ? lfSensorTypesToRecord : new ArrayList<Integer>();
        _savePredictionFiles = savePredictionFiles;
        _saveUserLabelsFiles = saveUserLabelsFiles;
        _historyTimeUnitInMinutes = historyTimeUnitInMinutes;
    }

    private static ESSettings _settings = null;

    private static ESSettings getTheSettings() {
        if (_settings == null) {
            _settings = getTheDBAccessor().getTheSettings();
        }

        return _settings;
    }

    /**
     * Get the UUID (unique user identifier) of the application.
     * @return The UUID of the application
     */
    public static String uuid() {
        return getTheSettings()._uuid;
    }

    /**
     * Get the maximum allowed number of stored examples.
     * @return The maximum allowed number of stored examples
     */
    public static int maxStoredExamples() {
        return getTheSettings()._maxStoredExamples;
    }

    /**
     * Should the app use the notification mechanism for near-past context?
     * @return Use near-past notifications or not
     */
    public static boolean useNearPastNotifications() { return getTheSettings()._useNearPastNotifications; }

    /**
     * Should the app use the notification mechanism for near-future context?
     * @return Use near-future notifications or not
     */
    public static boolean useNearFutureNotifications() { return getTheSettings()._useNearFutureNotifications; }

    public static boolean useAnyTypeOfNotifications() {
        ESSettings theSettings = getTheSettings();
        return (theSettings._useNearPastNotifications || theSettings._useNearFutureNotifications);
    }
    /**
     * Get the notification interval - the time interval between scheduled notification checks.
     * (in each check, if necessary a notification will pop to the user).
     * The interval is given in seconds.
     *
     * @return Notification interval (seconds).
     */
    public static int notificationIntervalInSeconds() {
        return getTheSettings()._notificationIntervalInSeconds;
    }

    /**
     * Get the number of examples to store before initiating a sequence of uploading to the server.
     *
     * @return The number of examples to store before sending them all in a short time.
     */
    public static int numExamplesStoreBeforeSend() {
        return getTheSettings()._numExamplesStoreBeforeSend;
    }

    /**
     * Does the user allow using cellular network communication (as opposed to "just WiFi")?
     * @return
     */
    public static boolean isCellularAllowed() { return getTheSettings()._allowCellular; }

    /**
     * Should we use a location bubble?
     * @return
     */
    public static boolean shouldUseLocationBubble() { return getTheSettings()._locationBubbleUsed; }

    /**
     * Get the location bubble center - representing the home of the user,
     * a location that within a ball around it we shouldn't expose actual latitude/longitute values through the network.
     *
     * @return The location of the bubble center.
     */
    public static Location locationBubbleCenter() {
        return getTheSettings()._locationBubbleCenter;
    }

    /**
     * Get the classifier type. This signals to the server what type of classifier to use when getting the sensor measurements.
     * @return
     */
    public static String classifierType() { return getTheSettings()._classifierType; }

    /**
     * Get the classifier name. This signals to the server which trained model to use when getting the sensor measurements.
     * @return
     */
    public static String classifierName() { return getTheSettings()._classifierName; }

    /**
     * Should the app (attempt to) record audio?
     * @return
     */
    public static boolean shouldRecordAudio() { return  getTheSettings()._recordAudio; }

    /**
     * Should the app (attempt to) record location?
     * @return
     */
    public static boolean shouldRecordLocation() { return  getTheSettings()._recordLocation; }

    /**
     * Should the app (attempt to) record from the Watch?
     * @return
     */
    public static boolean shouldRecordWatch() { return  getTheSettings()._recordWatch; }

    /**
     * What are the sensor types of the high-frequency sensors that the app should attempt to record?
     * @return
     */
    public static ArrayList<Integer> highFreqSensorTypesToRecord() { return getTheSettings()._hfSensorTypesToRecord; }

    /**
     * What are the sensor types of the low-frequency sensors that the app should attempt to record?
     * @return
     */
    public static ArrayList<Integer> lowFreqSensorTypesToRecord() { return  getTheSettings()._lfSensorTypesToRecord; }

    /**
     * Should the app save to files the predictions it gets from the server.
     * These files will be available for other apps to read.
     * @return Save prediction files or not?
     */
    public static boolean savePredictionFiles() { return getTheSettings()._savePredictionFiles; }

    /**
     * Should the app save to files the user-reported labels.
     * These files will be available for other apps to read.
     * @return Save user-labels files or not?
     */
    public static boolean saveUserLabelsFiles() { return getTheSettings()._saveUserLabelsFiles; }

    /**
     * What is the basic time unit to present the events in the history
     * @return basic time unit (in minutes)
     */
    public static int historyTimeUnitInMinutes() { return getTheSettings()._historyTimeUnitInMinutes; }

    // Setters:

    /**
     * Set the maximum allowed number of stored examples.
     *
     * @param maxStoredExamples The maximum allowed number of stored examples.
     */
    public static void setMaxStoredExamples(int maxStoredExamples) {
        _settings = getTheDBAccessor().setSettingsMaxStoredExamples(maxStoredExamples);
    }

    /**
     * Set should the app use the near-past notification mechanism
     * @param useNearPastNotifications Use near-past notifications or not?
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setUseNearPastNotifications(boolean useNearPastNotifications) {
        _settings = getTheDBAccessor().setSettingsUseNearPastNotifications(useNearPastNotifications);
        ((ESApplication)ESApplication.getTheAppContext()).checkShouldWeCollectDataAndManageAppropriately();
    }

    /**
     * Set should the app use the near-future notification mechanism
     * @param useNearFutureNotifications Use near-future notifications or not?
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setUseNearFutureNotifications(boolean useNearFutureNotifications) {
        _settings = getTheDBAccessor().setSettingsUseNearFutureNotifications(useNearFutureNotifications);
        ((ESApplication)ESApplication.getTheAppContext()).checkShouldWeCollectDataAndManageAppropriately();
    }

    /**
     * Set the time interval (in seconds) between scheduled notification checks
     * (when checking if a user notification is needed).
     *
     * @param notificationIntervalInSeconds The notification interval (in seconds)
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setNotificationIntervalInSeconds(int notificationIntervalInSeconds) {
        _settings = getTheDBAccessor().setSettingsNotificationInterval(notificationIntervalInSeconds);
        ((ESApplication)ESApplication.getTheAppContext()).checkShouldWeCollectDataAndManageAppropriately();
    }

    /**
     * Set the number of examples to store before sending them to the server.
     * @param numExamplesStoreBeforeSend
     */
    public static void setNumExamplesStoreBeforeSend(int numExamplesStoreBeforeSend) {
        _settings = getTheDBAccessor().setSettingsNumExamplesStoredBeforeSend(numExamplesStoreBeforeSend);
    }

    /**
     * Set does the user allow using cellular communication.
     * @param allowCellular
     */
    public static void setAllowCellular(boolean allowCellular) {
        _settings = getTheDBAccessor().setSettingsAllowCellular(allowCellular);
    }

    /**
     * Set should we use a location bubble
     * @param useLocationBubble
     */
    public static void setLocationBubbleUsed(boolean useLocationBubble) {
        _settings = getTheDBAccessor().setSettingsUseLocationBubble(useLocationBubble);
    }

    /**
     * Set the location bubble center by providing the coordinates of the center
     * @param locationBubbleCenterLat The latitude coordinate
     * @param locationBubbleCenterLong The longitude coordinate
     */
    public static void setLocationBubbleCenter(double locationBubbleCenterLat, double locationBubbleCenterLong) {
        _settings = getTheDBAccessor().setSettingsLocationBubbleCenterCoordinates(locationBubbleCenterLat,locationBubbleCenterLong);
        Log.i(LOG_TAG,String.format("Changed location bubble center to: <lat=%f,long=%f>",locationBubbleCenterLat,locationBubbleCenterLong));
    }

    /**
     * Set the location of the bubble center
     * @param locationBubbleCenter
     */
    public static void setLocationBubbleCenter(Location locationBubbleCenter) {
        _settings = getTheDBAccessor().setSettingsLocationBubbleCenter(locationBubbleCenter);
    }

    /**
     * Set the classifier settings. This will signal to the server what type of classifier and what is the name of the
     * trained classifier model to use when it gets sensor measurements, to classify the example.
     * @param classifierType
     * @param classifierName
     */
    public static void setClassifierSettings(String classifierType,String classifierName) {
        _settings = getTheDBAccessor().setClassifierSettings(classifierType,classifierName);
    }

    /**
     * Set should the app record audio
     * @param shouldRecordAudio
     */
    public static void setShouldRecordAudio(boolean shouldRecordAudio) {
        _settings = getTheDBAccessor().setRecordAudio(shouldRecordAudio);
    }

    /**
     * Set should the app record location
     * @param shouldRecordLocation
     */
    public static void setShouldRecordLocation(boolean shouldRecordLocation) {
        _settings = getTheDBAccessor().setRecordLocation(shouldRecordLocation);
    }

    /**
     * Set should the app record from the WATCH
     * @param shouldRecordWatch
     */
    public static void setShouldRecordWatch(boolean shouldRecordWatch) {
        _settings = getTheDBAccessor().setRecordWatch(shouldRecordWatch);
    }

    public static void setShouldRecordSensor(Integer sensorTypeInteger,boolean shouldRecord,boolean hf1lf0) {
        // Get the current list of sensors to record:
        ArrayList<Integer> sensorsToRecord = hf1lf0 ? highFreqSensorTypesToRecord() : lowFreqSensorTypesToRecord();
        boolean sensorAlreadyInList = sensorsToRecord.contains(sensorTypeInteger);

        // Make necessary changes, if needed:
        if (shouldRecord && !sensorAlreadyInList) {
            sensorsToRecord.add(sensorTypeInteger);
        }
        if (!shouldRecord && sensorAlreadyInList) {
            sensorsToRecord.remove(sensorTypeInteger);
        }

        // Update the new list:
        if (hf1lf0) {
            _settings = getTheDBAccessor().setHighFreqSensorsToRecord(sensorsToRecord);
        }
        else {
            _settings = getTheDBAccessor().setLowFreqSensorsToRecord(sensorsToRecord);
        }
    }

    /**
     * Set whether or not the app should save to files the predictions it gets from the server.
     * These files can be accessed by other apps.
     * @param savePredictionFiles Save predictions to files or not?
     */
    public static void setSavePredictionFiles(boolean savePredictionFiles) {
        _settings = getTheDBAccessor().setSettingsSavePredictionFiles(savePredictionFiles);
    }

    /**
     * Set whether or not the app should save to files the user-reported labels.
     * These files can be accessed by other apps.
     * @param saveUserLabelsFiles Save user-labels to files or not?
     */
    public static void setSaveUserLabelsFiles(boolean saveUserLabelsFiles) {
        _settings = getTheDBAccessor().setSettingsSaveUserLabelsFiles(saveUserLabelsFiles);
    }

    public static void setHistoryTimeUnitInMinutes(int historyTimeUnitInMinutes) {
        _settings = getTheDBAccessor().setHistoryTimeUnitInMinutes(historyTimeUnitInMinutes);
    }


    private static ESDatabaseAccessor getTheDBAccessor() {
        return ESDatabaseAccessor.getESDatabaseAccessor();
    }

}
