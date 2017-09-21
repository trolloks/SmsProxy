package world.zing.smsproxy.receivers;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Date;

import rx.Observer;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.MainActivity;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.services.SmsService;
import world.zing.smsproxy.utils.SmsUtil;

/**
 * Created by rikus on 2017/07/04.
 */

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent.getAction().equals("android.provider.Telephony.SMS_DELIVER")){
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            Object[] pdus = null;
            String msg_from;
            if (bundle != null){
                try{

                    if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                        msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                    } else {
                        pdus = (Object[]) bundle.get("pdus");
                        if (pdus == null)
                            return;
                        msgs = new SmsMessage[pdus.length];
                    }

                    for (int i = 0; i < msgs.length; i++){

                        if (Build.VERSION.SDK_INT < 19) {
                            if (pdus == null)
                                return;
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }

                        msg_from = msgs[i].getOriginatingAddress();
                        final String msgBody = msgs[i].getMessageBody();
                        final String msgFrom = msg_from;

                        Sms sms = new Sms();

                        sms.sender = msgFrom;
                        sms.message = msgBody;
                        sms.created = msgs[i].getTimestampMillis();
                        sms.id = sms.sender + "|" + sms.created;

                        PublishSubject<Sms> smsEmitter = PublishSubject.create();
                        smsEmitter
                                .observeOn(SmsService.getInstance().getProducerScheduler())
                                .subscribe(new Observer<Sms>() {
                                    @Override
                                    public void onCompleted() {}

                                    @Override
                                    public void onError(Throwable e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onNext(Sms sms) {
                                        SmsService.getInstance().produce(context.getApplicationContext(), sms);
                                    }
                                });

                        // emit the value on io thread
                        smsEmitter.onNext(sms);

                        // start consuming
                        PublishSubject<Context> processEmitter = PublishSubject.create();
                        processEmitter
                                .observeOn(SmsService.getInstance().getConsumerScheduler())
                                .subscribe(new Observer<Context>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                    }

                                    @Override
                                    public void onNext(Context context) {
                                        SmsService.getInstance().consume(context);
                                        SmsService.getInstance().close();
                                    }
                                });

                        // emit the value on io thread
                        processEmitter.onNext(context);
                    }
                } catch(Exception e){
                    // handle error here!
                    e.printStackTrace();
                }
            }
        }
    }
}