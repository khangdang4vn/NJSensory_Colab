package edu.ucsd.calab.extrasensory.questionnaire.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionChoicesDao;
import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionDao;
import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionEntity;
import edu.ucsd.calab.extrasensory.questionnaire.qdb.QuestionWithChoicesEntity;

@Database(entities = {QuestionWithChoicesEntity.class, QuestionEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase
{
    private static final String DB_NAME = "question_db";

    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase getAppDatabase(Context context)
    {
        if (INSTANCE == null)
        {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME)

                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }

    public abstract QuestionChoicesDao getQuestionChoicesDao();
    public abstract QuestionDao getQuestionDao();
}
