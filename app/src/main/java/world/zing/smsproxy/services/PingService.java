package world.zing.smsproxy.services;


import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.SettingsActivity;
import world.zing.smsproxy.gateways.PingGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Log;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.repositories.GatewaySettingsRepository;

/**
 * Created by rikus on 2017/07/04.
 */

public class PingService {

    private static PingService instance;

    private ArrayList<SmsService.LogListener> logListeners;

    private PublishSubject<Log> logSubject;
    private Subscription logSubscription;

    // TIMERS
    private TimerTask task;
    private Timer timer;
    private long pollingTime = Long.MAX_VALUE;

    private PingService(){
        logListeners = new ArrayList<>();

        logSubject = PublishSubject.create();
        logSubscription = logSubject
                .buffer(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Log>>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(List<Log> logs) {
                        for (Log log : logs) {
                            for (SmsService.LogListener logListener : logListeners) {
                                logListener.log(log);
                            }
                        }
                    }
                });

    }

    public static PingService getInstance(){
        return instance == null ? instance = new PingService() : instance;
    }


    public void start(final Context context){
        stopTimer();
        HashMap<String, String> gateway = GatewaySettingsRepository.getInstance().getGatewaySettings(context);
        pollingTime = gateway.containsKey("api.pingpolltime") ? Long.parseLong(gateway.get("api.pingpolltime")) : 36000000;
        task = new TimerTask() {
            @Override
            public void run() {
                PublishSubject<Context> repoEmitter = PublishSubject.create();
                repoEmitter
                        .observeOn(Schedulers.io())
                        .subscribe(new Observer<Context>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(Context context) {
                                final IRequest request = new IRequest(){};
                                PingGateway.getInstance().post(context, request, new ResponseListener() {
                                    @Override
                                    public void onSuccess(Context context, Response response) {
                                        Log msgLog = new Log();
                                        msgLog.message ="PONG @ " + new Date() + "!";
                                        msgLog.title = "Sent ping to server";
                                        log(msgLog.title, msgLog.message);
                                    }

                                    @Override
                                    public void onError(Context context, Response response) {
                                        Log msgLog = new Log();
                                        msgLog.message ="ERROR! - " + response.message;
                                        msgLog.title = "Error pinging server";
                                        log(msgLog.title, msgLog.message);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                repoEmitter.onNext(context);
            }
        };

        timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, pollingTime);

    }

    private void stopTimer(){
        if (timer != null) {
            try {
                timer.cancel();
                timer.purge();
                timer = null;
            } catch (Exception e) {
                timer = null;
                e.printStackTrace();
            }
        }
    }

    public void stop(){
        stopTimer();
        logSubscription.unsubscribe();
        instance = null;
    }

    //-----------------------------------
    // listeners!
    //-----------------------------------
    private void log(String title, String message){
        Log msgLog = new Log();
        msgLog.message = message;
        msgLog.title = title;

        // send!
        logSubject.onNext(msgLog);
    }


    public void addLogListener(SmsService.LogListener logListener){
        this.logListeners.add(logListener);
    }

    public void removeLogListener(SmsService.LogListener logListener){
        this.logListeners.remove(logListener);
    }

    private Executor pingExec;

    public Scheduler getPingScheduler(){
        return Schedulers.from(pingExec == null ? pingExec = Executors.newFixedThreadPool(1): pingExec);
    }

}
