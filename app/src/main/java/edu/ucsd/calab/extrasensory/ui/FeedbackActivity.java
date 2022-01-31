package edu.ucsd.calab.extrasensory.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESActivity;
import edu.ucsd.calab.extrasensory.data.ESContinuousActivity;
import edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor;
import edu.ucsd.calab.extrasensory.data.ESLabelStruct;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;

import static edu.ucsd.calab.extrasensory.data.ESDatabaseAccessor.getESDatabaseAccessor;

import androidx.annotation.RequiresApi;

/**
 * Feedback view for the user to provide the ground truth labels of what they are doing/feeling.
 * Right before starting this view you should call setFeedbackParametersBeforeStartingFeedback()
 * to inform this class what kind of feedback you need.
 * In addition, if this feedback activity is initiated from a notification
 * (or from an alert-dialog that was initiated by a notification/reminder),
 * you should add an extra with key KEY_INITIATED_BY_NOTIFICATION to the intent that starts this activity.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class FeedbackActivity extends BaseActivity {

    private static final String LOG_TAG = "[FeedbackActivity]";

    public static final int FEEDBACK_TYPE_ACTIVE = 1;
    public static final int FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY = 2;
    public static final String KEY_INITIATED_BY_NOTIFICATION = "edu.ucsd.calab.extrasensory.extra_key.initiated_by_notification";

    Button button;

    private static final String KEY_ROW_HEADER = "row header";
    private static final String KEY_ROW_DETAIL = "row detail";

    private static boolean feedbackFlag = false;
    private static final int ROW_MAIN = 0;
    private static final int ROW_SECONDARY = 1;
    private static final int ROW_MOOD = 2;
    private static final int ROW_VALID = 3;

    private static final String[] ROW_HEADERS = new String[] { "Main Activity", "Phone Position", "Location", "Valid for" };

    private ESLabelStruct _labelStruct = new ESLabelStruct();
    private String _validFor = SelectionFromListActivity.getValidForValues()[0];
    public int _validForHowManyMinutes;
    private String _historyValidFor = "";
    private boolean _presentServerGuesses;

    private ESTimestamp _timestampOpenFeedbackForm = null;
    private static FeedbackActivity theSingleSensorManager;
    
    /**
     * Get the single instance of this class
     * @return
     */
    public static FeedbackActivity getFeedbackActivity() {
        if (theSingleSensorManager == null) {
            theSingleSensorManager = new FeedbackActivity();
        }
        return theSingleSensorManager;
    }


    /**
     * This parameter type is to be used to transfer parameters to the feedback view,
     * that indicate what kind of feedback to perform and pass relevant data.
     */
    public static final class FeedbackParameters {
        private int _feedbackType;
        private ESContinuousActivity _continuousActivityToEdit;
        private ESTimestamp _timestampNotification = null;
        private ESTimestamp _timestampUserRespondToNotification = null;

        /**
         * Parameters - for doing active feedback
         * @param timestampNotification if this is active feedback initiated by notification, the timestamp of notification. Otherwise, null
         * @param timestampUserRespondToNotification if this is active feedback initiated by notification, the timestamp of user respond to notification. Otherwise, null
         */
        public FeedbackParameters(ESTimestamp timestampNotification, ESTimestamp timestampUserRespondToNotification) {
            _feedbackType = FEEDBACK_TYPE_ACTIVE;
            _continuousActivityToEdit = null;
            _timestampNotification = timestampNotification;
            _timestampUserRespondToNotification = timestampUserRespondToNotification;
        }

        /**
         * Default feedback parameters: doing active feedback that was initiated by user without notification
         */
        public FeedbackParameters() {
            this(null,null);
        }

        /**
         * Parameters for doing feedback to edit the labels of a continuous activity
         * @param continuousActivityToEdit The continuous activity whose labels to edit
         * @param timestampNotification if this is past-feedback initiated by notification, the timestamp of notification. Otherwise, null
         * @param timestampUserRespondToNotification if this is past-feedback initiated by notification, the timestamp of user respond to notification. Otherwise, null
         */
        public FeedbackParameters(ESContinuousActivity continuousActivityToEdit,ESTimestamp timestampNotification, ESTimestamp timestampUserRespondToNotification) {
            _feedbackType = FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY;
            _continuousActivityToEdit = continuousActivityToEdit;
            _timestampNotification = timestampNotification;
            _timestampUserRespondToNotification = timestampUserRespondToNotification;
        }

        /**
         * Parameters for doing feedback to edit the labels of a continuous activity from the past, initiated by user via the history.
         * @param continuousActivityToEdit The continuous activity whose labels to edit
         */
        public FeedbackParameters(ESContinuousActivity continuousActivityToEdit) {
            this(continuousActivityToEdit,null,null);
        }
    }

    private static FeedbackParameters _transientInputParameters = null;

    /**
     * Call this static function right before starting the feedback activity.
     * Set the parameters for the feedback.
     * @param inputParameters
     */
    public static void setFeedbackParametersBeforeStartingFeedback(FeedbackParameters inputParameters) {
        _transientInputParameters = inputParameters;
    }
    private FeedbackParameters _parameters = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        Log.d(LOG_TAG,"activity being created");

        _timestampOpenFeedbackForm = new ESTimestamp();
        Log.d(LOG_TAG,"Opened the form at time: " + _timestampOpenFeedbackForm +
                ". Minute: " + _timestampOpenFeedbackForm.getMinuteOfHour());

        // Check if anyone set the transient input parameters:
        if (_transientInputParameters == null) {
            // Then behave as active feedback:
            _parameters = new FeedbackParameters();
        }
        else {
            // Quickly, copy the given input parameters and save them to the current Feedback object:
            _parameters = _transientInputParameters;
            _transientInputParameters = null;
        }

        // If got an existing activity in the input parameters, copy its labels:
        if (_parameters._feedbackType == FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY) {
            _labelStruct = new ESLabelStruct();
            _labelStruct._mainActivity = _parameters._continuousActivityToEdit.mostUpToDateMainActivity();
            _labelStruct._secondaryActivities = _parameters._continuousActivityToEdit.getSecondaryActivities();
            _labelStruct._moods = _parameters._continuousActivityToEdit.getMoods();

            Date start = _parameters._continuousActivityToEdit.getStartTimestamp().getDateOfTimestamp();
            Date end = _parameters._continuousActivityToEdit.getEndTimestamp().getDateOfTimestamp();
            String timeLabelStart = new SimpleDateFormat("EEE, dd MMM, hh:mm a").format(start);
            String timeLabelEnd = new SimpleDateFormat("hh:mm a").format(end);
            _historyValidFor = timeLabelStart + " - " + timeLabelEnd;

            _presentServerGuesses = !_parameters._continuousActivityToEdit.hasUserProvidedLabels();
        }

        // Put a dummy main activity, in case it is empty:
        if (_labelStruct._mainActivity == null) {
            _labelStruct._mainActivity = getString(R.string.not_sure_dummy_label);
        }

        feedbackFlag = false;
        presentFeedbackView();

    }

    //refresh feedback list with user selections each time we open feedback page
    @Override
    protected void onStart(){
        super.onStart();
        presentFeedbackView();
    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "activity being destroyed");
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
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


        return super.onOptionsItemSelected(item);
    }

    private void presentFeedbackView() {
        switch (_parameters._feedbackType) {
            case FEEDBACK_TYPE_ACTIVE:
                Log.i(LOG_TAG,"Opening active feedback view");
                break;
            case FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY:
                Log.i(LOG_TAG,"Opening feedback view for continuous activity from history");
                Log.d(LOG_TAG,"Got continuous activity: " + _parameters._continuousActivityToEdit);
                break;
            default:
                Log.e(LOG_TAG,"Got unsupported feedback type: " + _parameters._feedbackType);
                //TODO: how to handle such a case? leave blank/default page? present active feedback? present error message? close activity and go back to previous?
        }

        ListView listView = (ListView) findViewById(R.id.listview_activity);


        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        HashMap<String,String> mainDatum = new HashMap<>(2);
        mainDatum.put(KEY_ROW_HEADER,ROW_HEADERS[ROW_MAIN]);
        mainDatum.put(KEY_ROW_DETAIL,_labelStruct._mainActivity);
        data.add(mainDatum);

        HashMap<String,String> secondaryDatum = new HashMap<>(2);
        secondaryDatum.put(KEY_ROW_HEADER,ROW_HEADERS[ROW_SECONDARY]);
        if (_presentServerGuesses) {
            secondaryDatum.put(KEY_ROW_DETAIL,joinPredictedLabels(_parameters._continuousActivityToEdit.getPredictionLabelNamesPredictedYes(),100));
        }
        else {
            secondaryDatum.put(KEY_ROW_DETAIL, joinByComma(_labelStruct._secondaryActivities));
        }
        data.add(secondaryDatum);

        HashMap<String,String> moodDatum = new HashMap<>(2);
        moodDatum.put(KEY_ROW_HEADER,ROW_HEADERS[ROW_MOOD]);
        moodDatum.put(KEY_ROW_DETAIL,joinByComma(_labelStruct._moods));
        data.add(moodDatum);

        HashMap<String,String> validDatum = new HashMap<>(2);

        String validRowHeader = _parameters._feedbackType == FEEDBACK_TYPE_ACTIVE ? ROW_HEADERS[ROW_VALID] : _historyValidFor;
        validDatum.put(KEY_ROW_HEADER,validRowHeader);
        validDatum.put(KEY_ROW_DETAIL, _parameters._feedbackType == FEEDBACK_TYPE_ACTIVE ? _validFor : "");
        data.add(validDatum);

        SimpleAdapter adapter = new SimpleAdapter(this, data,
                android.R.layout.simple_list_item_2,
                new String[] {KEY_ROW_HEADER, KEY_ROW_DETAIL},
                new int[] {android.R.id.text1,
                        android.R.id.text2});
        listView.setAdapter(adapter);

        //only create feedback button once
        if(feedbackFlag == false) {
            View sendButton = getLayoutInflater().inflate(R.layout.start_activity_button, null);
            listView.addFooterView(sendButton);
            addListenerOnButton();
            feedbackFlag = true;
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // "position" is the position/row of the item that you have clicked
                String[] frequentLabels;

                Intent intent = null;
                switch (position) {
                    case ROW_MAIN:
                        intent = new Intent(ESApplication.getTheAppContext(), SelectionFromListActivity.class);
                        intent.putExtra(SelectionFromListActivity.LIST_TYPE_KEY, SelectionFromListActivity.LIST_TYPE_MAIN_ACTIVITY);
                        intent.putExtra(SelectionFromListActivity.ADD_NOT_SURE_LABEL_KEY,true);
                        intent.putExtra(SelectionFromListActivity.PRESELECTED_LABELS_KEY,new String[] {_labelStruct._mainActivity});
                        startActivityForResult(intent, ROW_MAIN);
                        break;
                    case ROW_SECONDARY:
                        intent = new Intent(ESApplication.getTheAppContext(), SelectionFromListActivity.class);
                        intent.putExtra(SelectionFromListActivity.LIST_TYPE_KEY, SelectionFromListActivity.LIST_TYPE_SECONDARY_ACTIVITIES);
                        intent.putExtra(SelectionFromListActivity.PRESELECTED_LABELS_KEY,_labelStruct._secondaryActivities);
                        frequentLabels = ESDatabaseAccessor.getESDatabaseAccessor().getFrequentlyUsedLabels(null , ESDatabaseAccessor.ESLabelType.ES_LABEL_TYPE_SECONDARY);
                        intent.putExtra(SelectionFromListActivity.FREQUENTLY_USED_LABELS_KEY, frequentLabels);
                        if (_parameters != null && _parameters._continuousActivityToEdit != null) {
                            List<Map.Entry<String, Double>> sortedPredLabelsAndProbs = _parameters._continuousActivityToEdit.getPredictionLabelsSortedByProb();
                            intent.putStringArrayListExtra(SelectionFromListActivity.PREDICTED_LABEL_NAMES_KEY, extractLabelNames(sortedPredLabelsAndProbs));
                            intent.putExtra(SelectionFromListActivity.PREDICTED_LABEL_PROBS_KEY, extractLabelProbs(sortedPredLabelsAndProbs));
                        }

                        startActivityForResult(intent, ROW_SECONDARY);
                        break;
                    case ROW_MOOD:
                        intent = new Intent(ESApplication.getTheAppContext(), SelectionFromListActivity.class);
                        intent.putExtra(SelectionFromListActivity.LIST_TYPE_KEY, SelectionFromListActivity.LIST_TYPE_MOODS);
                        intent.putExtra(SelectionFromListActivity.PRESELECTED_LABELS_KEY,_labelStruct._moods);
                        frequentLabels = ESDatabaseAccessor.getESDatabaseAccessor().getFrequentlyUsedLabels(null , ESDatabaseAccessor.ESLabelType.ES_LABEL_TYPE_MOOD);
                        intent.putExtra(SelectionFromListActivity.FREQUENTLY_USED_LABELS_KEY,frequentLabels);
                        startActivityForResult(intent, ROW_MOOD);
                        break;
                    case ROW_VALID:
                        //Toast.makeText(getApplicationContext(), "Hello", Toast.LENGTH_LONG).show();
                        if (_parameters._feedbackType == FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY) {
                            // Then don't react to selecting this field.
                            break;
                        }
                        intent = new Intent(ESApplication.getTheAppContext(), SelectionFromListActivity.class);
                        intent.putExtra(SelectionFromListActivity.LIST_TYPE_KEY, SelectionFromListActivity.LIST_TYPE_VALID_FOR);
                        intent.putExtra(SelectionFromListActivity.PRESELECTED_LABELS_KEY,new String[] {_validFor});
                        startActivityForResult(intent, ROW_VALID);
                        break;
                }
            }
        });

    }

    public void addListenerOnButton() {
        final Context context = this;
        button = (Button) findViewById(R.id.startactivity);
        button.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View arg0) {
                ESTimestamp timestampPressSendButton = new ESTimestamp();
                boolean initiatedByNotification = getIntent().hasExtra(KEY_INITIATED_BY_NOTIFICATION);

                //user must enter some labels before submitting active feedback
                if (_parameters._feedbackType == FEEDBACK_TYPE_ACTIVE) {
                    boolean emptyMain = (_labelStruct._mainActivity == null) || (_labelStruct._mainActivity.equals(getString(R.string.not_sure_dummy_label)));
                    boolean emptySec = (_labelStruct._secondaryActivities == null) || (_labelStruct._secondaryActivities.length <= 0);
                    boolean emptyMood = (_labelStruct._moods == null) || (_labelStruct._moods.length <= 0);
                    if (emptyMain && emptySec && emptyMood) {
                        // custom dialog
                        final Dialog dialog = new Dialog(context);
                        dialog.setContentView(R.layout.activity_feedback_dialog);
                        dialog.setTitle(R.string.dialog_title);

                        // set the custom dialog components - text, image and button
                        TextView text = (TextView) dialog.findViewById(R.id.feedback_dialog_text);
                        text.setText(R.string.message_cant_report_zero_labels);

                        Button dialogButton = (Button) dialog.findViewById(R.id.feedback_dialogButtonOK);
                        // if button is clicked, close the custom dialog
                        dialogButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                            }
                        });

                        dialog.show();
                        return;
                    }
                }

                if(_parameters._feedbackType == FEEDBACK_TYPE_ACTIVE){
                    Log.d(LOG_TAG,"ACTIVE FEEDBACK");
                    ((ESApplication)getApplication()).set_userSelectedDataCollectionOn(true);
                    ((ESApplication)getApplication()).startActiveFeedback(_labelStruct, _validForHowManyMinutes, initiatedByNotification,
                            _timestampOpenFeedbackForm, timestampPressSendButton, _parameters._timestampNotification , _parameters._timestampUserRespondToNotification);
                    ESSensorManager.getForHowManyMinutes(_validForHowManyMinutes);
                    ESSensorManager.getMainActivity(_labelStruct._mainActivity);
                    ESSensorManager.getSecondaryActivity(_labelStruct._secondaryActivities);
                    ESSensorManager.getMood(_labelStruct._moods);
                    finish();
                    return;
                }

                else if(_parameters._feedbackType == FEEDBACK_TYPE_HISTORY_CONTINUOUS_ACTIVITY) {
                    Log.d(LOG_TAG,"HISTORY CONTINUOUS ACTIVITY");
                    ESContinuousActivity esContAct =_parameters._continuousActivityToEdit;
                    ESActivity [] esActivityArr = esContAct.getMinuteActivities();
                    ESActivity.ESLabelSource labelSource = initiatedByNotification ?
                            ESActivity.ESLabelSource.ES_LABEL_SOURCE_NOTIFICATION_ANSWER_NOT_EXACTLY :
                            ESActivity.ESLabelSource.ES_LABEL_SOURCE_HISTORY;
                    //for each minute activity in the continuous activity
                    //call setESActivityValues()
                    for (ESActivity minute : esActivityArr){
                        getESDatabaseAccessor().setESActivityValues(minute,
                                labelSource,
                                _labelStruct._mainActivity,
                                _labelStruct._secondaryActivities,
                                _labelStruct._moods,
                                _timestampOpenFeedbackForm,timestampPressSendButton,
                                _parameters._timestampNotification,_parameters._timestampUserRespondToNotification);
                    }

                    finish();
                    return;
                }
            }
        });
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        Log.d(LOG_TAG,"got activity result with result code: " + requestCode);
        if (resultCode != Activity.RESULT_OK) {
            Log.i(LOG_TAG,"requested task was canceled.");
            return;
        }

        if (data == null) {
            Log.e(LOG_TAG,"Output from selection had null data");
            return;
        }
        if (!data.hasExtra(SelectionFromListActivity.SELECTED_LABELS_OUTPUT_KEY)) {
            Log.e(LOG_TAG,"Output from selection is missing the selected labels");
            return;
        }

        String[] selected = data.getStringArrayExtra(SelectionFromListActivity.SELECTED_LABELS_OUTPUT_KEY);
        String presentableString = joinByComma(selected);
        Log.d(LOG_TAG,"selected: " + presentableString);

        switch (requestCode) {
            case ROW_MAIN:
                _labelStruct._mainActivity = selected[0];
                break;
            case ROW_SECONDARY:
                _labelStruct._secondaryActivities = selected;
                _presentServerGuesses = false;
                break;
            case ROW_MOOD:
                _labelStruct._moods = selected;
                break;
            case ROW_VALID:
                //_parameters._feedbackType == FEEDBACK_TYPE_ACTIVE){
                _validFor = selected[0];
                String[] splited = _validFor.split("\\s+");
                _validForHowManyMinutes = Integer.parseInt(splited[0]);
                break;
        }
    }

    public int timevalidFor(int x) {
        _validForHowManyMinutes = x;
    return _validForHowManyMinutes;
    }

    private static String joinPredictedLabels(String[] labels,int maxChars) {
        String joint = joinByDelimiter(labels,"? ") + "?";
        if (joint.length() > maxChars) {
            joint = joint.substring(0,maxChars) + "...";
        }
        return joint;
    }

    private static String joinByComma(String[] labels) {
        return joinByDelimiter(labels,",");
    }

    private static String joinByDelimiter(String[] labels,String delimiter) {
        if (labels == null || labels.length <= 0) {
            return "";
        }

        String singleString = labels[0];
        for (int i=1; i < labels.length; i ++) {
            singleString += delimiter + labels[i];
        }

        return singleString;
    }

    private static ArrayList<String> extractLabelNames(List<Map.Entry<String,Double>> labelNamesAndProbs) {
        if (labelNamesAndProbs == null) {
            return new ArrayList<>();
        }

        ArrayList<String> labelNames = new ArrayList<>(labelNamesAndProbs.size());
        for (int i = 0; i < labelNamesAndProbs.size(); i ++) {
            labelNames.add(labelNamesAndProbs.get(i).getKey());
        }
        return labelNames;
    }

    private static double[] extractLabelProbs(List<Map.Entry<String,Double>> labelNamesAndProbs) {
        if (labelNamesAndProbs == null) {
            return new double[0];
        }

        double[] labelProbs = new double[labelNamesAndProbs.size()];
        for (int i = 0; i < labelNamesAndProbs.size(); i ++) {
            labelProbs[i] = labelNamesAndProbs.get(i).getValue();
        }
        return labelProbs;
    }

}
