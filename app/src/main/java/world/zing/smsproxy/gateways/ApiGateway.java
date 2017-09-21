package world.zing.smsproxy.gateways;

import android.content.Context;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

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
import java.util.SimpleTimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import world.zing.smsproxy.interfaces.IGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.repositories.GatewaySettingsRepository;

/**
 * Created by rikus on 2017/07/03.
 */

public class ApiGateway implements IGateway {

    private static ApiGateway instance;
    private HashMap<String, String> gateways;

    private final int MIN_MESSAGE_LENGTH = 20;
    private final int MAX_MESSAGE_LENGTH = 160;

    private ApiGateway(){}

    public static ApiGateway getInstance(){
        return instance == null ? instance = new ApiGateway() : instance;
    }

    @Override
    public void post(final Context context, final IRequest message, final ResponseListener responseListener) {
        Sms sms = (Sms)message;
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        // start processing
        try{
            System.err.println("Sending API CALL");

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, null, null);

            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            URL url = new URL(String.format(Locale.getDefault(), "%s/sms", (gateways.containsKey("api.endpoint") ? gateways.get("api.endpoint"): "")));

            HttpURLConnection connection = null;
            if (url.toString().startsWith("https")) {
                connection = (HttpsURLConnection) url.openConnection();
            } else {
                connection = (HttpURLConnection) url.openConnection();
            }

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(true);
            connection.setReadTimeout(30000);

            OutputStream os = connection.getOutputStream();

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", gateways.containsKey("api.username") ? gateways.get("api.username"): "");
            jsonObject.put("pin", gateways.containsKey("api.pin") ? gateways.get("api.pin"): "");

            if (sms.message.length() <= MIN_MESSAGE_LENGTH){
                Response response = new Response();
                response.request = sms;
                response.code = 411; // length required
                response.message = "Message failed via api : Message too short";
                responseListener.onError(context, response);

                // break out!
                return;
            }

            jsonObject.put("message", sms.message.substring(0, Math.min(sms.message.length(), MAX_MESSAGE_LENGTH))); // cutoff
            jsonObject.put("replyTo", sms.sender);
            jsonObject.put("sendTo", gateways.containsKey("api.mynumber") ? gateways.get("api.mynumber"): "2710000001");
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());
            dateFormat.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")); // set time to UTC
            jsonObject.put("timestamp1", dateFormat.format(new Date(sms.created)));
            jsonObject.put("messageIdentifier", "smsproxy");

            // monitor sent data
            String jsonString = jsonObject.toString();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            bw.write(jsonString);
            bw.flush();
            bw.close();

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK){
                Response response = new Response();
                response.request = sms;
                response.message = "Message sent successfully via api";
                responseListener.onSuccess(context, response);
            } else {
                Response response = new Response();
                response.request = sms;
                response.message = "Message failed via api : " + connection.getResponseMessage();
                responseListener.onError(context, response);
            }

            os.close();

        } catch (Exception e){
            Response response = new Response();
            response.request = sms;
            response.message = e.getMessage();
            responseListener.onError(context, response);
            e.printStackTrace();
        }


    }
}
