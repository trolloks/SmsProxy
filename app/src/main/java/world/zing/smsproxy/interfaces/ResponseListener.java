package world.zing.smsproxy.interfaces;

import android.content.Context;

import world.zing.smsproxy.models.Response;

/**
 * Created by rikus on 2017/07/03.
 */

public interface ResponseListener {

    public void onSuccess(Context context, Response response);
    public void onError(Context context, Response response);

}
