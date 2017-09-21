package world.zing.smsproxy.gateways;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import world.zing.smsproxy.interfaces.IGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.repositories.GatewaySettingsRepository;

/**
 * Created by rikus on 2017/07/06.
 */

public class GatewayRouter implements IGateway{

    private static GatewayRouter instance;

    private HashMap<String, String> gateways;

    private GatewayRouter(){
    }

    public static GatewayRouter getInstance(){
        return instance == null ? instance = new GatewayRouter() : instance;
    }

    public void enableGateway (Context context, String gatewayName, boolean enable){
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        gateways.put(gatewayName, String.valueOf(enable));
        GatewaySettingsRepository.getInstance().persist(context, gateways);
    }

    public boolean isGatewayEnabled (Context context, String gatewayName){
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);

        if (gateways.containsKey(gatewayName))
            return Boolean.parseBoolean(gateways.get(gatewayName));
        else
            return false;
    }

    public void putSetting(Context context, String key, String value){
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        gateways.put(key, value);
        GatewaySettingsRepository.getInstance().persist(context, gateways);
    }

    public String getSetting(Context context, String key, String defaultValue){
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        return gateways.containsKey(key) ? gateways.get(key): defaultValue;
    }


    @Override
    public void post(Context context, IRequest message, final ResponseListener responseListener) {
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        Iterator<String> itr = gateways.keySet().iterator();
        while (itr.hasNext()){
            String key = itr.next();
            if (key.equals("api") && Boolean.parseBoolean(gateways.get(key))){
                ApiGateway.getInstance().post(context, message, responseListener);
                return;
            }

            if (key.equals("email") && Boolean.parseBoolean(gateways.get(key))){
                EmailGateway.getInstance().post(context, message, responseListener);
                return;
            }
        }

        Response response = new Response();
        response.request = message;
        response.code = 404;
        response.message = "There are no active gateways";
        responseListener.onError(context, response);
    }
}
