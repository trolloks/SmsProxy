package world.zing.smsproxy.repositories;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

import world.zing.smsproxy.interfaces.IRepository;
import world.zing.smsproxy.models.Sms;

/**
 * Created by rikus on 2017/07/06.
 */

public class GatewaySettingsRepository implements IRepository {
    private static GatewaySettingsRepository instance;

    // persistence
    public static final String PREFS_NAME = "GatewaySettings";

    private HashMap<String, String> gatewaySettings;

    private GatewaySettingsRepository(){
    }

    public static GatewaySettingsRepository getInstance(){
        return instance == null ? instance = new GatewaySettingsRepository() : instance;
    }

    public void init(Context context){
        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String json = settings.getString("settings", null);

        if (json == null)
            gatewaySettings = new HashMap<>();
        else {
            Gson gson = new Gson();
            try {
                gatewaySettings = new HashMap<>();
                gatewaySettings = gson.fromJson(json, new TypeToken<HashMap<String, String>>() {}.getType());
            } catch (Exception e){
                e.printStackTrace();
                gatewaySettings = new HashMap<>();
            }
        }
    }

    @Override
    public <T> void persist(Context context, T data) {
        Gson gson = new Gson();
        String json = gson.toJson(data, new TypeToken<HashMap<String, String>>() {}.getType());

        // set to cache
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("settings", json);
        editor.commit();
    }

    public HashMap<String, String> getGatewaySettings(Context context){
        init(context);

        return gatewaySettings;
    }

}
