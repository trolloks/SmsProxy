package world.zing.smsproxy.interfaces;

import android.content.Context;

/**
 * Created by rikus on 2017/07/03.
 */

public interface IGateway {
    public void post(Context context, IRequest message, ResponseListener responseListener);
}
