package edu.ucsd.calab.extrasensory.questionnaire.questionmodels;

import com.google.gson.annotations.SerializedName;

import edu.ucsd.calab.extrasensory.questionnaire.questionmodels.Data;

public class QuestionDataModel
{
    @SerializedName("data")
    private Data data;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private boolean status;

    public Data getData()
    {
        return data;
    }

    public void setData(Data data)
    {
        this.data = data;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public boolean isStatus()
    {
        return status;
    }

    public void setStatus(boolean status)
    {
        this.status = status;
    }
}