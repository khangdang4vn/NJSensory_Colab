package edu.ucsd.calab.extrasensory.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import edu.ucsd.calab.extrasensory.ESApplication;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.questionnaire.QuestionActivity;
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity;

/**
 * This class is for the "home page", which acts as a dashboard for the user to know what is going on and to help debug problems.
 *
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class HomeFragment extends BaseTabFragment {

    private static final String LOG_TAG = "[ES-HomeFragment]";
    private static final String NO_AVAILABLE_NETWORK_FOR_SENDING = "There's no available network now to send the data to the server.";
    private static final String ALERT_BUTTON_TEXT_OK = "o.k.";
    private static final int QUESTIONNAIRE_REQUEST = 2018;

    private ESApplication getESApplication()  {
        MainActivity mainActivity = (MainActivity) getActivity();
        //_polarProcessor = PolarActivity.getPolarProcessor();
        assert mainActivity != null;
        return (ESApplication)mainActivity.getApplication();
    }

    private final PolarActivity _polarProcessor;
    public HomeFragment() {
        // Required empty public constructor
        _polarProcessor = PolarActivity.getPolarProcessor();
    }

    private RadioGroup _dataCollectionRadioGroup = null;
//    private TextView _storedExamplesCount = null;
//    private TextView _feedbackQueueCount = null;

    @SuppressLint("NonConstantResourceId")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View homeView = inflater.inflate(R.layout.fragment_home, container, false);
        _dataCollectionRadioGroup = (RadioGroup)homeView.findViewById(R.id.radio_group_data_collection);
        _dataCollectionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Intent intent;
            switch (checkedId) {
                case R.id.radio_data_collection_off:
                    Log.i(LOG_TAG,"User turned data collection off");
                    getESApplication().set_userSelectedDataCollectionOn(false);
                    break;
                case R.id.radio_data_collection_on:
                    Log.i(LOG_TAG,"User turned data collection on");
                    getESApplication().set_userSelectedDataCollectionOn(true);
                    break;
                default:
                    Log.e(LOG_TAG,"Unrecognized action for data collection radio button group: " + checkedId);
                    // Do nothing
            }
            //_dataCollectionRadioGroup.check(R.id.radio_data_collection_off);
        }
        );

        ImageView _armbandIcon = homeView.findViewById(R.id.imagebutton_armband_icon);
        _armbandIcon.setOnClickListener(v -> startActivity(new Intent(Objects.requireNonNull(getActivity()).getApplicationContext(), PolarActivity.class)));

//        _storedExamplesCount = homeView.findViewById(R.id.text_zip_file_count);
//        presentNumStoredExamples();
//
//        _feedbackQueueCount = homeView.findViewById(R.id.text_feedback_count);
//        presentFeedbackQueueCount();

        Button _sendStoredExamplesButton = homeView.findViewById(R.id.button_send_stored_examples);
        _sendStoredExamplesButton.setOnClickListener(v -> {
            if (!ESNetworkAccessor.getESNetworkAccessor().canWeUseNetworkNow()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setIcon(R.drawable.ic_launcher).setMessage(NO_AVAILABLE_NETWORK_FOR_SENDING);
                builder.setPositiveButton(ALERT_BUTTON_TEXT_OK,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();

                return;
            }
            // If we can use network to send the data, go for it:
//            ESNetworkAccessor.getESNetworkAccessor().uploadWhatYouHave();
//                ESNetworkAccessor.getESNetworkAccessor().uploadDirectory2S3(ESNetworkAccessor.rawDir);
            new ESNetworkAccessor.S3UploadAllDataTask().execute(ESNetworkAccessor.rawDir);
        });

      /*  Button questionnaireButton = homeView.findViewById(R.id.questionnaireButton);
        questionnaireButton.setOnClickListener(v -> {
            Intent questions = new Intent(Objects.requireNonNull(getActivity()).getApplicationContext(), QuestionActivity.class);
            //you have to pass as an extra the json string.
            questions.putExtra("json_questions", loadQuestionnaireJson());
            startActivityForResult(questions, QUESTIONNAIRE_REQUEST);

        });*/


        return homeView;
    }

    //json stored in the assets folder. but you can get it from wherever you like.
    private String loadQuestionnaireJson()
    {
        try
        {

            InputStream is = Objects.requireNonNull(getActivity()).getApplicationContext().getAssets().open("questions_longsurvey.json");
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

//    private void presentNumStoredExamples() {
//        int num = ESNetworkAccessor.getESNetworkAccessor().uploadQueueSize();
//        _storedExamplesCount.setText("" + num);
//    }
//
//    private void presentFeedbackQueueCount() {
//        int num = ESNetworkAccessor.getESNetworkAccessor().feedbackQueueSize();
//        _feedbackQueueCount.setText("" + num);
//    }
 /*   @Override
    public void onPause() {
        super.onPause();

        if (_dataCollectionRadioGroup == null) {
            Log.e(LOG_TAG,"radio group of data collection is null");
        }
        if (getESApplication().is_userSelectedDataCollectionOn()) {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_on);
        }
        else {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_off);
        }
    }*/

  /*  public void clickturnoff() {
        LayoutInflater inflater = null;
        assert false;
        View homeView = inflater.inflate(R.layout.fragment_home, null, false);
        _dataCollectionRadioGroup = (RadioGroup)homeView.findViewById(R.id.radio_group_data_collection);
        _dataCollectionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_off);
        });
    }*/

    @Override
    public void onResume() {
        super.onResume();

        if (_dataCollectionRadioGroup == null) {
            Log.e(LOG_TAG,"radio group of data collection is null");
        }
        if (getESApplication().is_userSelectedDataCollectionOn()) {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_on);
        }
        else {
            _dataCollectionRadioGroup.check(R.id.radio_data_collection_off);
        }
    }

    @Override
    protected void reactToRecordsUpdatedEvent() {
        super.reactToRecordsUpdatedEvent();
        Log.d(LOG_TAG,"reacting to records-update");
        //TODO: redraw the relevant image to the latest activity
    }

//    @Override
//    protected void reactToNetworkQueueSizeChangedEvent() {
//        super.reactToNetworkQueueSizeChangedEvent();
//        presentNumStoredExamples();
//    }
//
//    @Override
//    protected void reactToFeedbackQueueSizeChangedEvent() {
//        super.reactToFeedbackQueueSizeChangedEvent();
//        presentFeedbackQueueCount();
//    }

}
