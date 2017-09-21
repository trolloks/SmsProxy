package world.zing.smsproxy.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import world.zing.smsproxy.interfaces.IRepository;
import world.zing.smsproxy.models.Sms;

/**
 * Created by rikus on 2017/07/04.
 */

public class ErrorQueueRepository implements IRepository {

    private static ErrorQueueRepository instance;

    private ArrayList<Sms> smsQueue;

    // persistence
    public static final String PREFS_NAME = "ErrorQueue";

    private ErrorQueueRepository(){
    }

    public static ErrorQueueRepository getInstance(){
        return instance == null ? instance = new ErrorQueueRepository() : instance;
    }

    public void init(Context context){
        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String json = settings.getString("errqueue", null);

        if (json == null)
            smsQueue = new ArrayList<>();
        else {
            Gson gson = new Gson();
            try {
                smsQueue = new ArrayList<>();
                smsQueue = gson.fromJson(json, new TypeToken<ArrayList<Sms>>() {}.getType());
            } catch (Exception e){
                e.printStackTrace();
                smsQueue = new ArrayList<>();
            }
        }
    }

    public void clear(Context context){
        smsQueue = new ArrayList<>();
        persist(context, smsQueue);
    }

    @Override
    public <T> void persist(Context context, T data) {
        Gson gson = new Gson();
        String json = gson.toJson(data, new TypeToken<ArrayList<Sms>>() {}.getType());

        // set to cache
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("errqueue", json);
        editor.commit();
    }

    public void add(Context context, Sms sms){
        if (smsQueue == null){
            init(context);
        }

        smsQueue.add(sms);
        persist(context, smsQueue);
    }

    public void remove(Context context, Sms sms){
        if (smsQueue == null){
            init(context);
        }

        smsQueue.remove(sms);
        persist(context, smsQueue);
    }

    public ArrayList<Sms> list(Context context){
        if (smsQueue == null){
            init(context);
        }

        return smsQueue;
    }

    public Sms take(Context context) throws Exception{
        if (smsQueue == null){
            init(context);
        }

        Sms sms = smsQueue.get(smsQueue.size() - 1);
        smsQueue.remove(smsQueue.size() - 1);
        persist(context, smsQueue);
        return sms;
    }

}
