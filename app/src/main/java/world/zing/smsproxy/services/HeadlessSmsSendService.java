package world.zing.smsproxy.services;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * Created by rikus on 2017/07/04.
 */

public class HeadlessSmsSendService extends IntentService {

    public HeadlessSmsSendService(){
        super("");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // not implemented
    }
}
