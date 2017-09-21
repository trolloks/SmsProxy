package world.zing.smsproxy.models;

import world.zing.smsproxy.interfaces.IRequest;

/**
 * Created by rikus on 2017/07/03.
 */

public class Sms implements IRequest {

    public String id;
    public String sender;
    public String message;
    public long created;

}
