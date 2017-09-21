package world.zing.smsproxy.gateways;

import android.content.Context;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;

import java.util.HashMap;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.interfaces.IGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.repositories.GatewaySettingsRepository;

/**
 * Created by rikus on 2017/07/03.
 */

public class EmailGateway implements IGateway {

    private static EmailGateway instance;
    private HashMap<String, String> gateways;

    private EmailGateway(){}

    public static EmailGateway getInstance(){
        return instance == null ? instance = new EmailGateway() : instance;
    }

    @Override
    public void post(final Context context, final IRequest message, final ResponseListener responseListener) {
        final Sms sms = (Sms)message;
        gateways = GatewaySettingsRepository.getInstance().getGatewaySettings(context);

        try {
            BackgroundMail.newBuilder(context)
                    .withUsername(gateways.containsKey("email.account") ? gateways.get("email.account"): "")
                    .withPassword(gateways.containsKey("email.password") ? gateways.get("email.password"): "")
                    .withMailTo(gateways.containsKey("email.email") ? gateways.get("email.email"): "")
                    .withType(BackgroundMail.TYPE_PLAIN)
                    .withSubject(gateways.containsKey("email.subject") ? gateways.get("email.subject"): "This is the default subject")
                    .withBody(sms.message)
                    .withProcessVisibility(false) // hopefully remove ui aspect
                    .withOnSuccessCallback(new BackgroundMail.OnSuccessCallback() {
                        @Override
                        public void onSuccess() {
                            Response response = new Response();
                            response.request = sms;
                            response.message = "Message sent successfully via email";
                            responseListener.onSuccess(context, response);
                        }
                    })
                    .withOnFailCallback(new BackgroundMail.OnFailCallback() {
                        @Override
                        public void onFail() {
                            Response response = new Response();
                            response.message = "Message failed via email";
                            response.request = sms;
                            responseListener.onError(context, response);
                        }
                    })
                    .send();
        } catch (Exception e){
            Response response = new Response();
            response.request = sms;
            response.message = e.getMessage();
            responseListener.onError(context, response);
            e.printStackTrace();
        }


    }
}
