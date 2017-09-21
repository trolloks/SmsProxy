package world.zing.smsproxy.gateways;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import world.zing.smsproxy.interfaces.IGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.repositories.GatewaySettingsRepository;

/**
 * Created by rikus on 2017/07/11.
 */

public class PingGateway implements IGateway {

    private static PingGateway instance;
    private HashMap<String, String> gateways;

    private PingGateway(){}

    public static PingGateway getInstance(){
        return instance == null ? instance = new PingGateway() : instance;
    }

    @Override
    public void post(final Context context, final IRequest message, final ResponseListener responseListener) {
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        // start processing
        try{
            System.err.println("Sending API CALL");

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, null, null);

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            URL url = new URL(String.format(Locale.getDefault(), "%s/proxydevice/ping", (gateways.containsKey("api.endpoint") ? gateways.get("api.endpoint"): "")));

            HttpURLConnection connection = null;
            if (url.toString().startsWith("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(true);
            connection.setReadTimeout(30000);

            OutputStream os = connection.getOutputStream();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", gateways.containsKey("api.username") ? gateways.get("api.username"): "");
            jsonObject.put("pin", gateways.containsKey("api.pin") ? gateways.get("api.pin"): "");
            jsonObject.put("deviceName", gateways.containsKey("api.devicename") ? gateways.get("api.devicename"): "");

            // monitor sent data
            String jsonString = jsonObject.toString();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(jsonString);
            bw.flush();
            bw.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK){
                Response response = new Response();
                response.request = message;
                response.message = "Message sent successfully via api";
                responseListener.onSuccess(context, response);
            } else {
                Response response = new Response();
                response.request = message;
                response.message = "Message failed via api : " + connection.getResponseMessage();
                responseListener.onError(context, response);
            }

            os.close();

        } catch (Exception e){
            Response response = new Response();
            response.request = message;
            response.message = e.getMessage();
            responseListener.onError(context, response);
            e.printStackTrace();
        }


    }
}