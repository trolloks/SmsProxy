package world.zing.smsproxy;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import world.zing.smsproxy.gateways.PingGateway;
import world.zing.smsproxy.interfaces.IRequest;
import world.zing.smsproxy.interfaces.ResponseListener;
import world.zing.smsproxy.models.Log;
import world.zing.smsproxy.gateways.GatewayRouter;
import world.zing.smsproxy.models.Response;
import world.zing.smsproxy.models.Sms;
import world.zing.smsproxy.repositories.ErrorQueueRepository;
import world.zing.smsproxy.repositories.SmsQueueRepository;
import world.zing.smsproxy.services.SmsService;
import world.zing.smsproxy.utils.SmsUtil;

/**
 * Created by rikus on 2017/07/06.
 */

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Settings");
        setContentView(R.layout.activity_settings);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        createDevArea();
        createGatewayArea();

    }

    private void createGatewayArea() {
        ((SwitchCompat)findViewById(R.id.emailswitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                GatewayRouter.getInstance().enableGateway(SettingsActivity.this, "email", checked);
                if (checked){
                    findViewById(R.id.emailContainer).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.emailContainer).setVisibility(View.GONE);
                }

            }
        });

        ((SwitchCompat)findViewById(R.id.emailswitch)).setChecked(GatewayRouter.getInstance().isGatewayEnabled(SettingsActivity.this, "email"));
        findViewById(R.id.emailheadercontainer).setOnClickListener(new View.OnClickListener() {
            boolean checked = ((SwitchCompat)findViewById(R.id.emailswitch)).isChecked();

            @Override
            public void onClick(View view) {
                ((SwitchCompat)findViewById(R.id.emailswitch)).setChecked(!checked);
                checked = !checked;
            }
        });

        ((SwitchCompat)findViewById(R.id.apiswitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                GatewayRouter.getInstance().enableGateway(SettingsActivity.this, "api", checked);
                if (checked){
                    findViewById(R.id.apiContainer).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.apiContainer).setVisibility(View.GONE);
                }
            }
        });

        ((SwitchCompat)findViewById(R.id.apiswitch)).setChecked(GatewayRouter.getInstance().isGatewayEnabled(SettingsActivity.this, "api"));
        findViewById(R.id.apiheadercontainer).setOnClickListener(new View.OnClickListener() {
            boolean checked = ((SwitchCompat)findViewById(R.id.apiswitch)).isChecked();

            @Override
            public void onClick(View view) {
                ((SwitchCompat)findViewById(R.id.apiswitch)).setChecked(!checked);
                checked = !checked;
            }
        });


        ((AppCompatEditText)findViewById(R.id.emailSenderAccount)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "email.account", ((AppCompatEditText)findViewById(R.id.emailSenderAccount)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.emailSenderPassword)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "email.password", ((AppCompatEditText)findViewById(R.id.emailSenderPassword)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });



        ((AppCompatEditText)findViewById(R.id.email)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "email.email", ((AppCompatEditText)findViewById(R.id.email)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.emailHeader)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "email.subject", ((AppCompatEditText)findViewById(R.id.emailHeader)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // set email settings
        ((AppCompatEditText)findViewById(R.id.emailHeader)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "email.subject", "SMS Proxy Message"));
        ((AppCompatEditText)findViewById(R.id.email)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "email.email", "rikuswlouw@gmail.com"));
        ((AppCompatEditText)findViewById(R.id.emailSenderAccount)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "email.account", "rikuswlouw@gmail.com"));
        ((AppCompatEditText)findViewById(R.id.emailSenderPassword)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "email.password", ""));




        ((AppCompatEditText)findViewById(R.id.apiendpoint)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.endpoint", ((AppCompatEditText)findViewById(R.id.apiendpoint)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.apiusername)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.username", ((AppCompatEditText)findViewById(R.id.apiusername)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.apipin)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.pin", ((AppCompatEditText)findViewById(R.id.apipin)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        ((AppCompatEditText)findViewById(R.id.apipingpolltime)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.pingpolltime", ((AppCompatEditText)findViewById(R.id.apipingpolltime)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.apidevicename)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.devicename", ((AppCompatEditText)findViewById(R.id.apidevicename)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        ((AppCompatEditText)findViewById(R.id.apinumber)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                GatewayRouter.getInstance().putSetting(SettingsActivity.this, "api.mynumber", ((AppCompatEditText)findViewById(R.id.apinumber)).getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // set email settings
        ((AppCompatEditText)findViewById(R.id.apiendpoint)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.endpoint", ""));
        ((AppCompatEditText)findViewById(R.id.apiusername)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.username", ""));
        ((AppCompatEditText)findViewById(R.id.apipin)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.pin", ""));
        ((AppCompatEditText)findViewById(R.id.apinumber)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.mynumber", ""));
        ((AppCompatEditText)findViewById(R.id.apidevicename)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.devicename", ""));
        ((AppCompatEditText)findViewById(R.id.apipingpolltime)).setText(GatewayRouter.getInstance().getSetting(SettingsActivity.this, "api.pingpolltime", ""));

    }

    private void createDevArea() {
        findViewById(R.id.unreadbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start processing
                PublishSubject<Context> processEmitter = PublishSubject.create();
                processEmitter
                        .observeOn(Schedulers.io())
                        .subscribe(new Observer<Context>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(Context context) {
                                SmsUtil.setAllMessagesToUnread(SettingsActivity.this);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.loading).setVisibility(View.GONE);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                processEmitter.onNext(SettingsActivity.this);
            }
        });

        findViewById(R.id.testmessage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Sms sms = new Sms();

                sms.sender = "27840000001";
                sms.message = "This is a test message";
                sms.created = System.currentTimeMillis();
                sms.id = sms.sender + "|" + sms.created;

                PublishSubject<Sms> smsEmitter = PublishSubject.create();
                smsEmitter
                        .observeOn(SmsService.getInstance().getProducerScheduler())
                        .subscribe(new Observer<Sms>() {
                            @Override
                            public void onCompleted() {}

                            @Override
                            public void onError(Throwable e) {}

                            @Override
                            public void onNext(Sms sms) {
                                SmsService.getInstance().produce(SettingsActivity.this, sms);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.loading).setVisibility(View.GONE);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                // emit the value on io thread
                smsEmitter.onNext(sms);
            }
        });

        findViewById(R.id.clearqueue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                SmsQueueRepository.getInstance().clear(context);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.loading).setVisibility(View.GONE);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                // emit the value on io thread
                repoEmitter.onNext(SettingsActivity.this);
            }
        });

        findViewById(R.id.clearerrors).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                ErrorQueueRepository.getInstance().clear(context);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        findViewById(R.id.loading).setVisibility(View.GONE);
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });

                // emit the value on io thread
                repoEmitter.onNext(SettingsActivity.this);
            }
        });


        findViewById(R.id.testbutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                                        msgLog.message ="PONG!";
                                        msgLog.title = "Sent ping to server";
                                        System.err.println(msgLog.title + " - " + msgLog.message);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                findViewById(R.id.loading).setVisibility(View.GONE);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(Context context, Response response) {
                                        Log msgLog = new Log();
                                        msgLog.message ="ERROR!";
                                        msgLog.title = "Error pinging server";
                                        System.err.println(msgLog.title + " - " + msgLog.message);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                findViewById(R.id.loading).setVisibility(View.GONE);
                                            }
                                        });
                                    }
                                });
                            }
                        });

                // emit the value on io thread
                findViewById(R.id.loading).setVisibility(View.VISIBLE);
                findViewById(R.id.loading).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });

                // emit the value on io thread
                repoEmitter.onNext(SettingsActivity.this);

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }
}
