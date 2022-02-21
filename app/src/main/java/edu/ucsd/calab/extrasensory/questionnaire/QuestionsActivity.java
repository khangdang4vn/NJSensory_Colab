package edu.ucsd.calab.extrasensory.questionnaire;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.data.ESDataFilesAccessor;
import edu.ucsd.calab.extrasensory.data.ESSettings;
import edu.ucsd.calab.extrasensory.data.ESTimestamp;
import edu.ucsd.calab.extrasensory.network.ESNetworkAccessor;
import edu.ucsd.calab.extrasensory.questionnaire.adapters.ViewPagerAdapter;
import edu.ucsd.calab.extrasensory.questionnaire.database.AppDatabase;
import edu.ucsd.calab.extrasensory.questionnaire.fragments.CheckBoxesFragment;
import edu.ucsd.calab.extrasensory.questionnaire.fragments.RadioBoxesFragment;
import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionEntity;
import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionWithChoicesEntity;
import edu.ucsd.calab.extrasensory.questionnaire.questionmodels.AnswerOptions;
import edu.ucsd.calab.extrasensory.questionnaire.questionmodels.QuestionDataModel;
import edu.ucsd.calab.extrasensory.questionnaire.questionmodels.QuestionsItem;
import edu.ucsd.calab.extrasensory.sensors.ESSensorManager;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * This is the base for the UI classes:
 * all the UI pages have some things in common - they have the action bar, and they should display the "red light" whenever sensor-recording is active.
 *
 * Created by Yonatan on 2/24/2015.
 * ========================================
 * The ExtraSensory App
 * @author Yonatan Vaizman yvaizman@ucsd.edu
 * Please see ExtraSensory App website for details and citation requirements:
 * http://extrasensory.ucsd.edu/ExtraSensoryApp
 * ========================================
 */
public class QuestionsActivity extends AppCompatActivity {

    final ArrayList<Fragment> fragmentArrayList = new ArrayList<>();
    List<QuestionsItem> questionsItems = new ArrayList<>();
    private AppDatabase appDatabase;
    //private TextView questionToolbarTitle;
    private TextView questionPositionTV;
    private String totalQuestions = "1";
    private Gson gson;
    private ViewPager questionsViewPager;

    List<QuestionEntity> questionsList = new ArrayList<>();
    List<QuestionWithChoicesEntity> questionsWithAllChoicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questionnaire_question);

        toolBarInit();

        appDatabase = AppDatabase.getAppDatabase(QuestionsActivity.this);
        gson = new Gson();

        if (getIntent().getExtras() != null)
        {
            Bundle bundle = getIntent().getExtras();
            parsingData(bundle);
        }
    }

    private void toolBarInit()
    {
        Toolbar questionToolbar = findViewById(R.id.questionToolbar);
        questionToolbar.setNavigationIcon(R.drawable.ic_questionnaire_arrow_back);
        questionToolbar.setNavigationOnClickListener(v -> onBackPressed());

        //questionToolbarTitle = questionToolbar.findViewById(R.id.questionToolbarTitle);
        questionPositionTV = questionToolbar.findViewById(R.id.questionPositionTV);

        //questionToolbarTitle.setText("Questions");
    }

    /*This method decides how many Question-Screen(s) will be created and
    what kind of (Multiple/Single choices) each Screen will be.*/
    private void parsingData(Bundle bundle)
    {
        QuestionDataModel questionDataModel = new QuestionDataModel();

        questionDataModel = gson.fromJson(bundle.getString("json_questions"), QuestionDataModel.class);

        questionsItems = questionDataModel.getData().getQuestions();

        totalQuestions = String.valueOf(questionsItems.size());
        String questionPosition = "1/" + totalQuestions;
        setTextWithSpan(questionPosition);

        preparingQuestionInsertionInDb(questionsItems);
        preparingInsertionInDb(questionsItems);

        for (int i = 0; i < questionsItems.size(); i++)
        {
            QuestionsItem question = questionsItems.get(i);

            if (question.getQuestionTypeName().equals("CheckBox"))
            {
                CheckBoxesFragment checkBoxesFragment = new CheckBoxesFragment();
                Bundle checkBoxBundle = new Bundle();
                checkBoxBundle.putParcelable("question", question);
                checkBoxBundle.putInt("page_position", i);
                checkBoxesFragment.setArguments(checkBoxBundle);
                fragmentArrayList.add(checkBoxesFragment);
            }

            if (question.getQuestionTypeName().equals("Radio"))
            {
                RadioBoxesFragment radioBoxesFragment = new RadioBoxesFragment();
                Bundle radioButtonBundle = new Bundle();
                radioButtonBundle.putParcelable("question", question);
                radioButtonBundle.putInt("page_position", i);
                radioBoxesFragment.setArguments(radioButtonBundle);
                fragmentArrayList.add(radioBoxesFragment);
            }
        }

        questionsViewPager = findViewById(R.id.pager);
        questionsViewPager.setOffscreenPageLimit(1);
        ViewPagerAdapter mPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), fragmentArrayList);
        questionsViewPager.setAdapter(mPagerAdapter);
    }

    public void nextQuestion()
    {
        int item = questionsViewPager.getCurrentItem() + 1;
        questionsViewPager.setCurrentItem(item);

        String currentQuestionPosition = String.valueOf(item + 1);

        String questionPosition = currentQuestionPosition + "/" + totalQuestions;
        setTextWithSpan(questionPosition);
    }

    public int getTotalQuestionsSize()
    {
        return questionsItems.size();
    }

    private void preparingQuestionInsertionInDb(List<QuestionsItem> questionsItems)
    {
        List<QuestionEntity> questionEntities = new ArrayList<>();

        for (int i = 0; i < questionsItems.size(); i++)
        {
            QuestionEntity questionEntity = new QuestionEntity();
            questionEntity.setQuestionId(questionsItems.get(i).getId());
            questionEntity.setQuestion(questionsItems.get(i).getQuestionName());

            questionEntities.add(questionEntity);
        }
        insertQuestionInDatabase(questionEntities);
    }

    private void insertQuestionInDatabase(List<QuestionEntity> questionEntities)
    {
        Observable.just(questionEntities)
                .map(this::insertingQuestionInDb)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /*First, clear the table, if any previous data saved in it. Otherwise, we get repeated data.*/
    private String insertingQuestionInDb(List<QuestionEntity> questionEntities)
    {
        appDatabase.getQuestionDao().deleteAllQuestions();
        appDatabase.getQuestionDao().insertAllQuestions(questionEntities);
        return "";
    }

    private void preparingInsertionInDb(List<QuestionsItem> questionsItems)
    {
        ArrayList<QuestionWithChoicesEntity> questionWithChoicesEntities = new ArrayList<>();

        for (int i = 0; i < questionsItems.size(); i++)
        {
            List<AnswerOptions> answerOptions = questionsItems.get(i).getAnswerOptions();

            for (int j = 0; j < answerOptions.size(); j++)
            {
                QuestionWithChoicesEntity questionWithChoicesEntity = new QuestionWithChoicesEntity();
                questionWithChoicesEntity.setQuestionId(String.valueOf(questionsItems.get(i).getId()));
                questionWithChoicesEntity.setAnswerChoice(answerOptions.get(j).getName());
                questionWithChoicesEntity.setAnswerChoicePosition(String.valueOf(j));
                questionWithChoicesEntity.setAnswerChoiceId(answerOptions.get(j).getAnswerId());
                questionWithChoicesEntity.setAnswerChoiceState("0");

                questionWithChoicesEntities.add(questionWithChoicesEntity);
            }
        }

        insertQuestionWithChoicesInDatabase(questionWithChoicesEntities);
    }

    private void insertQuestionWithChoicesInDatabase(List<QuestionWithChoicesEntity> questionWithChoicesEntities)
    {
        Observable.just(questionWithChoicesEntities)
                .map(this::insertingQuestionWithChoicesInDb)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    /*First, clear the table, if any previous data saved in it. Otherwise, we get repeated data.*/
    private String insertingQuestionWithChoicesInDb(List<QuestionWithChoicesEntity> questionWithChoicesEntities)
    {
        appDatabase.getQuestionChoicesDao().deleteAllChoicesOfQuestion();
        appDatabase.getQuestionChoicesDao().insertAllChoicesOfQuestion(questionWithChoicesEntities);
        return "";
    }

    @Override
    public void onBackPressed()
    {
        if (questionsViewPager.getCurrentItem() == 0)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this).
                    setIcon(R.drawable.ic_launcher).setMessage("Please complete the questionnaire").
                    setTitle("NJSensory").setNegativeButton(R.string.ok_button_text,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        } else
        {
            int item = questionsViewPager.getCurrentItem() - 1;
            questionsViewPager.setCurrentItem(item);

            String currentQuestionPosition = String.valueOf(item + 1);

            String questionPosition = currentQuestionPosition + "/" + totalQuestions;
            setTextWithSpan(questionPosition);
        }
    }

    private void setTextWithSpan(String questionPosition)
    {
        int slashPosition = questionPosition.indexOf("/");

        Spannable spanText = new SpannableString(questionPosition);
        spanText.setSpan(new RelativeSizeSpan(0.7f), slashPosition, questionPosition.length(), 0);
        questionPositionTV.setText(spanText);
    }


    @Override
    protected void onStop() {
        super.onStop();
        getResultFromDatabase();
    }

    /*After, getting all result you can/must delete the saved results
    although we are clearing the Tables as soon we start the QuestionActivity.*/
    private void getResultFromDatabase()
    {
        Completable.fromAction(() -> {
            questionsList = appDatabase.getQuestionDao().getAllQuestions();
            questionsWithAllChoicesList = appDatabase.getQuestionChoicesDao().getAllQuestionsWithChoices("1");
        }).subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver()
                {
                    @Override
                    public void onSubscribe(Disposable d)
                    {

                    }

                    @Override
                    public void onComplete()
                    {
                        makeJsonDataToMakeResultView();
                    }

                    @Override
                    public void onError(Throwable e)
                    {

                    }
                });
    }

    /*Here, JSON got created and send to make Result View as per Project requirement.
     * Alternatively, in your case, you make Network-call to send the result to back-end.*/
    private void makeJsonDataToMakeResultView()
    {
        try
        {
            JSONArray questionAndAnswerArray = new JSONArray();
            int questionsSize = questionsList.size();
            if (questionsSize > 0)
            {
                for (int i = 0; i < questionsSize; i++)
                {
                    JSONObject questionName = new JSONObject();
                    questionName.put("question", questionsList.get(i).getQuestion());
                    //questionName.put("question_id", String.valueOf(questionsList.get(i).getQuestionId()));
                    String questionId = String.valueOf(questionsList.get(i).getQuestionId());

                    JSONArray answerChoicesList = new JSONArray();
                    int selectedChoicesSize = questionsWithAllChoicesList.size();
                    for (int k = 0; k < selectedChoicesSize; k++)
                    {
                        String questionIdOfChoice = questionsWithAllChoicesList.get(k).getQuestionId();
                        if (questionId.equals(questionIdOfChoice))
                        {
                            JSONObject selectedChoice = new JSONObject();
                            selectedChoice.put("answer_choice", questionsWithAllChoicesList.get(k).getAnswerChoice());
                            //selectedChoice.put("answer_id", questionsWithAllChoicesList.get(k).getAnswerChoiceId());
                            answerChoicesList.put(selectedChoice);
                        }
                    }
                    questionName.put("selected_answer", answerChoicesList);

                    questionAndAnswerArray.put(questionName);
                }
            }
            // Save data to file:
            String dataStr = questionAndAnswerArray.toString();
            writeFile(dataStr);

        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void writeFile(String content) {
        FileOutputStream fos;
        try {
            File outFile = new File(ESDataFilesAccessor.getQuestionnaireDir(), "questionnaire-" + currentZipFilename() + ".json");
            fos = new FileOutputStream(outFile);
            fos.write(content.getBytes());
            fos.close();
            new ESNetworkAccessor.S3UploadFileTask().execute(outFile);
        } catch (IOException e) {
            Log.e("AnswerActivity", e.getMessage());
        }
    }

    private static String getZipFilename(long timestamp) {
        return timestamp + "-" + ESSettings.uuid();
    }

    private String currentZipFilename() {
        long unixTime = System.currentTimeMillis() / 1000L;
        return getZipFilename(unixTime);
    }

}
