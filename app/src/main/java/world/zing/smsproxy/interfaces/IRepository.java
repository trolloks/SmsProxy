package world.zing.smsproxy.interfaces;

import android.content.Context;

/**
 * Created by rikus on 2017/07/06.
 */

public interface IRepository {
    public void init(Context context);
    public <T> void persist(Context context, T data);
}
