package world.zing.smsproxy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import rx.Observer;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.adapters.LogAdapter;
import world.zing.smsproxy.models.Log;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.services.PingService;
import world.zing.smsproxy.services.SmsService;
import world.zing.smsproxy.utils.SmsUtil;

public class MainActivity extends AppCompatActivity {

    // logs
    LogAdapter logAdapter;
    RecyclerView logRecycler;
    ArrayList<Log> logItems;
    SmsService.LogListener logListener;

    SmsService.ConsumerListener consumerListener;

    boolean isDefaultSms = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logRecycler = (RecyclerView) findViewById(R.id.list);
        logRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        logItems = new ArrayList<>();
        logAdapter = new LogAdapter(this, logItems);
        logRecycler.setAdapter(logAdapter);

        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });

        findViewById(R.id.clearsms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logItems.clear();
                logAdapter.notifyDataSetChanged();
            }
        });


        startPing();
        requestSmsPermission();
    }

    private void startPing() {
        PublishSubject<Context> pingEmitter = PublishSubject.create();
        pingEmitter
                .observeOn(PingService.getInstance().getPingScheduler())
                .subscribe(new Observer<Context>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(final Context context) {
                        PingService.getInstance().start(context);
                    }
                });

        // emit the value on io thread
        pingEmitter.onNext(MainActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // remove listeners
        SmsService.getInstance().removeLogListener(logListener);
        PingService.getInstance().removeLogListener(logListener);
        logListener = null;
    }

    private void checkDefaultApp() {
        // check default setting
        final String myPackageName = getPackageName();
        if (Build.VERSION.SDK_INT >= 19) { //KITKAT
            if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
                // App is not default.
                // Show the "not currently set as the default SMS app" interface
                View viewGroup = findViewById(R.id.defaultbutton);
                viewGroup.setVisibility(View.VISIBLE);

                // Set up a button that allows the user to change the default SMS app
                AppCompatTextView button = (AppCompatTextView) viewGroup;
                button.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (Build.VERSION.SDK_INT >= 19) { //KITKAT
                            Intent intent =
                                    new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                                    myPackageName);
                            startActivity(intent);
                        }
                    }
                });
            } else {
                isDefaultSms = true;
                View viewGroup = findViewById(R.id.defaultbutton);
                viewGroup.setVisibility(View.GONE);
            }
        } else {
            isDefaultSms = true;
            View viewGroup = findViewById(R.id.defaultbutton);
            viewGroup.setVisibility(View.GONE);
        }
    }

    private void startConsumer() {
        if (consumerListener == null) {
            SmsService.getInstance().addConsumerListener(consumerListener = new SmsService.ConsumerListener() {
                @Override
                public void consume() {
                    // start processing
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
                                }
                            });

                    // emit the value on io thread
                    processEmitter.onNext(MainActivity.this);
                }
            });


            // start
            consumerListener.consume();
        }
    }
    private void initLogs() {
        // check service logs
        if (logListener == null) {
            logListener = new SmsService.LogListener() {
                @Override
                public void log(Log log) {
                    // needs to be on ui thread
                    try {
                        MainActivity.this.log(log);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void amountLeft(int count) {
                    ((TextView) findViewById(R.id.smsleft)).setText("Queue (" + count + ")");
                }

                @Override
                public void errorLeft(int count) {
                    ((TextView) findViewById(R.id.errorleft)).setText("Errors (" + count + ")");
                }
            };
            SmsService.getInstance().addLogListener(logListener);
            PingService.getInstance().addLogListener(logListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLogs();
        checkDefaultApp();
        if (isDefaultSms){
            readUnreadMsgs();
            startConsumer();
            checkErrors();
        }

    }

    private void checkErrors() {
        findViewById(R.id.reprocess).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // check new unread messages
                findViewById(R.id.reprocess).setVisibility(View.GONE);
                PublishSubject<Context> smsEmitter = PublishSubject.create();
                smsEmitter
                        .observeOn(SmsService.getInstance().getProducerScheduler())
                        .subscribe(new Observer<Context>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(final Context context) {
                                SmsService.getInstance().reprocess(context);
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.reprocess).setVisibility(View.VISIBLE);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                smsEmitter.onNext(MainActivity.this);
            }
        });

        ((TextView)findViewById(R.id.errorleft)).setText("Errors (" + SmsService.getInstance().errors(MainActivity.this).size() + ")");
    }

    private void readUnreadMsgs() {
        findViewById(R.id.reprocess).setVisibility(View.GONE);
        // check new unread messages
        PublishSubject<Context> smsEmitter = PublishSubject.create();
        smsEmitter
                .observeOn(SmsService.getInstance().getProducerScheduler())
                .subscribe(new Observer<Context>() {

                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(final Context context) {
                        ArrayList<Sms> tempQueue = new ArrayList<>();
                        ArrayList<Sms> tempErrors = new ArrayList<>();

                        tempQueue.addAll(SmsService.getInstance().list(context));
                        tempErrors.addAll(SmsService.getInstance().errors(context));


                        // check old messages and produce
                        ArrayList <Sms> smses = SmsUtil.getAllUnreadSms (context);
                        for (final Sms sms : smses){
                            boolean match = false;
                            for (Sms savedSms : tempQueue){
                                if (savedSms.id.equals(sms.id)){
                                    match = true;
                                }
                            }

                            for (Sms errorSms : tempErrors){
                                if (errorSms.id.equals(sms.id)){
                                    match = true;
                                }
                            }

                            if (!match) {
                                SmsService.getInstance().produce(context, sms);
                            }
                        }

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                findViewById(R.id.reprocess).setVisibility(View.VISIBLE);
                            }
                        });

                    }
                });

        // emit the value on io thread
        smsEmitter.onNext(MainActivity.this);

        ((TextView)findViewById(R.id.smsleft)).setText("Queue (" + SmsService.getInstance().list(MainActivity.this).size() + ")");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // remove listeners
        SmsService.getInstance().removeLogListener(logListener);
        PingService.getInstance().removeLogListener(logListener);
        SmsService.getInstance().removeConsumerListener(consumerListener);

        // close
        SmsService.getInstance().close();
        PingService.getInstance().stop();
    }

    private void log (Log log){
        logItems.add(0, log);
        logAdapter.notifyDataSetChanged();
    }

    private void requestSmsPermission() {
        String permission = Manifest.permission.READ_SMS;
        int grant = ContextCompat.checkSelfPermission(this, permission);
        if (grant != PackageManager.PERMISSION_GRANTED) {
            String[] permission_list = new String[1];
            permission_list[0] = permission;
            ActivityCompat.requestPermissions(this, permission_list, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Permission not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
