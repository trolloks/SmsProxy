package world.zing.smsproxy.services;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.gateways.GatewayRouter;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Log;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.repositories.ErrorQueueRepository;
import world.zing.smsproxy.repositories.SmsQueueRepository;
import world.zing.smsproxy.utils.SmsUtil;

/**
 * Created by rikus on 2017/07/04.
 */

public class SmsService {

    private static SmsService instance;

    private ArrayList<ConsumerListener> consumerListeners;
    private ArrayList<LogListener> logListeners;

    private PublishSubject<Log> logSubject;
    private Subscription logSubscription;

    private PublishSubject<Integer> amountSubject;
    private Subscription amountSubscription;

    private PublishSubject<Integer> errorSubject;
    private Subscription errorSubscription;

    private final Object consumerLock = new Object();
    private final Object producerLock = new Object();
    private final Object errorLock = new Object();

    private SmsService(){
        logListeners = new ArrayList<>();
        consumerListeners = new ArrayList<>();

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
                            for (LogListener logListener : logListeners) {
                                logListener.log(log);
                            }
                        }
                    }
                });

        amountSubject = PublishSubject.create();
        amountSubscription = amountSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(Integer integer) {
                        for (LogListener logListener : logListeners) {
                            logListener.amountLeft(integer);
                        }
                    }
                });

        errorSubject = PublishSubject.create();
        errorSubscription = errorSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(Integer integer) {
                        for (LogListener logListener : logListeners) {
                            logListener.errorLeft(integer);
                        }
                    }
                });

    }

    public static SmsService getInstance(){
        return instance == null ? instance = new SmsService() : instance;
    }

    // service methods
    //-----------------------------------
    public void produce(final Context context, final Sms sms){
        // ADD ITEM TO QUEUE-----
        synchronized (producerLock) {
            SmsQueueRepository.getInstance().add(context, sms);
        }
        log("Add SMS (" + sms.id + ") to queue - " + sms.sender, sms.message);
        // ---------------------------

        // get size
        ArrayList<Sms> queue = new ArrayList<>();
        synchronized (producerLock){
            queue = list(context);
        }
        amountLeft(queue.size());
        synchronized (consumerLock){
            consumerLock.notifyAll();
        }
    }


    public ArrayList<Sms> list(final Context context){
        ArrayList<Sms> smses = new ArrayList<>();
        synchronized (producerLock){
            smses = SmsQueueRepository.getInstance().list(context);
        }
        return smses;
    }

    public ArrayList<Sms> errors(final Context context){
        ArrayList<Sms> errors = new ArrayList<>();
        synchronized (errorLock){
            errors = ErrorQueueRepository.getInstance().list(context);
        }
        return errors;
    }

    public void consume(final Context context) {
        ArrayList<Sms> queue = new ArrayList<>();
        synchronized (consumerLock) {
            while (true) {
                synchronized (producerLock){
                    queue = list(context);
                }

                while (!queue.isEmpty()) {
                    Sms sms = null;
                    synchronized (producerLock){
                        try {
                            // REMOVE ITEM FROM QUEUE-----
                            sms = SmsQueueRepository.getInstance().take(context);
                        } catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                    // add as error so long
                    synchronized (errorLock) {
                        ErrorQueueRepository.getInstance().add(context, sms);
                    }

                    // emitters
                    final PublishSubject<Response> processEmitter = PublishSubject.create();
                    processEmitter
                            .observeOn(getEmitterScheduler())
                            .subscribe(new Observer<Response>() {
                                @Override
                                public void onCompleted() {
                                }

                                @Override
                                public void onError(Throwable e) {
                                }

                                @Override
                                public void onNext(Response response) {
                                    log("Sent SMS (" + ((Sms) response.request).id + ") to API - " + ((Sms) response.request).sender, response.message);

                                    // check size
                                    ArrayList<Sms> queue = new ArrayList<>();
                                    ArrayList<Sms> errors = new ArrayList<>();
                                    synchronized (producerLock){
                                        queue = list(context);
                                    }
                                    synchronized (errorLock){
                                        // remove error if it succeeds
                                        ErrorQueueRepository.getInstance().remove(context, (Sms)response.request);
                                        errors = errors(context);
                                    }
                                    amountLeft(queue.size());
                                    errorLeft(errors.size());

                                }
                            });

                    final PublishSubject<Response> processErrorEmitter = PublishSubject.create();
                    processErrorEmitter
                            .observeOn(getEmitterScheduler())
                            .subscribe(new Observer<Response>() {
                                @Override
                                public void onCompleted() {
                                }

                                @Override
                                public void onError(Throwable e) {
                                }

                                @Override
                                public void onNext(Response response) {
                                    log("Error sending SMS (" + ((Sms) response.request).id + ") to API - " + ((Sms) response.request).sender, response.message);
                                    // re-add to queue (unless this is a 411 error)
                                    // ADD ITEM TO ERROR QUEUE-----
                                    ArrayList<Sms> errors = new ArrayList<>();
                                    synchronized (errorLock) {
                                        errors = errors(context);
                                    }
                                    errorLeft(errors.size());
                                }
                            });

                    if (sms != null) {
                        GatewayRouter.getInstance().post(context, sms, new ResponseListener() {
                            @Override
                            public void onSuccess(Context context, Response response) {
                                // set message as read
                                if (SmsUtil.markMessageRead(context, ((Sms) response.request).sender, ((Sms) response.request).message) != 0)
                                    processEmitter.onNext(response);
                                else
                                    processErrorEmitter.onNext(response);
                            }

                            @Override
                            public void onError(Context context, Response response) {
                                processErrorEmitter.onNext(response);
                            }
                        });
                    }
                }


                if (queue.isEmpty()){
                    try{
                        amountLeft(queue.size());
                        consumerLock.wait();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void reprocess(Context context){
        // check errors too
        ArrayList<Sms> errors = new ArrayList<>();
        synchronized (producerLock) {
            errors.addAll(errors(context));
            // clear all
            ErrorQueueRepository.getInstance().clear(context);
        }

        // reprocess errors
        for (final Sms error : errors){
            produce(context, error);
        }
    }

    public void close(){
        logSubscription.unsubscribe();
        amountSubscription.unsubscribe();
        errorSubscription.unsubscribe();
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

    private void amountLeft (int amountLeft){
        // send
        amountSubject.onNext(amountLeft);
    }

    private void errorLeft (int amountLeft){
        // send
        errorSubject.onNext(amountLeft);
    }

    public void addLogListener(LogListener logListener){
        this.logListeners.add(logListener);
    }

    public void removeLogListener(LogListener logListener){
        this.logListeners.remove(logListener);
    }

    public void addConsumerListener(ConsumerListener consumerListener){
        this.consumerListeners.add(consumerListener);
    }

    public void removeConsumerListener(ConsumerListener consumerListener){
        this.consumerListeners.remove(consumerListener);
    }
    //-----------------------------------


    public interface LogListener{
        public void log(Log log);
        public void amountLeft(int count);
        public void errorLeft(int count);
    }

    public interface ConsumerListener {
        public void consume();
    }

    private Executor producerExec, consumerExec, emitterExec;

    public Scheduler getProducerScheduler(){
        return Schedulers.from(producerExec == null ? producerExec = Executors.newFixedThreadPool(1): producerExec);
    }

    public Scheduler getConsumerScheduler(){
        return Schedulers.from(consumerExec == null ? consumerExec = Executors.newFixedThreadPool(1): consumerExec);
    }

    public Scheduler getEmitterScheduler(){
        return Schedulers.from(emitterExec == null ? emitterExec = Executors.newFixedThreadPool(1): emitterExec);
    }
}
