package world.zing.smsproxy.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import world.zing.smsproxy.models.Sms;

/**
 * Created by rikus on 2017/07/05.
 */

public class SmsUtil {
    public static int markMessageRead(Context context, String number, String body) {
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        try{
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if ((cursor.getString(cursor.getColumnIndex("address")).equals(number)) && (cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                        if (cursor.getString(cursor.getColumnIndex("body")).startsWith(body)) {
                            String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                            ContentValues values = new ContentValues();
                            values.put("read", true);

                            int result = context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                            if (result == 0){
                                System.out.println("test");
                            }
                            cursor.close();
                            return result;
                        }
                    }
                }
                cursor.close();
            }
        }catch(Exception e)
        {
            Log.e("Mark Read", "Error in Read: "+e.toString());
            return 0;
        }
        // no unread matches
        return 1;
    }

    public static ArrayList<Sms> getAllUnreadSms (Context context){
        ArrayList<Sms> smses = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cursor = cr.query(uriSMSURI, null, "read = 0", null, null);
        try{
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if ((cursor.getInt(cursor.getColumnIndex("read")) == 0)) {
                        String address = cursor.getString(cursor.getColumnIndex("address"));
                        String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                        String date =  cursor.getString(cursor.getColumnIndex("date"));

                        Sms sms = new Sms();

                        sms.message = body;
                        sms.sender = address;
                        sms.created = Long.parseLong(date);
                        sms.id = md5(sms.sender + "|" + sms.created);

                        smses.add(sms);
                    }
                }
                cursor.close();
            }
        } catch(Exception e)
        {
            android.util.Log.e("Mark Read", "Error in Read: "+e.toString());
        }

        // return empty list
        return smses;

    }

    public static void setAllMessagesToUnread (Context context){
        ContentResolver cr = context.getContentResolver();
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cursor = cr.query(uriSMSURI, null, "read = 1", null, null);
        try{
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if ((cursor.getInt(cursor.getColumnIndex("read")) == 1)) {
                        String SmsMessageId = cursor.getString(cursor.getColumnIndex("_id"));
                        ContentValues values = new ContentValues();
                        values.put("read", false);
                        context.getContentResolver().update(Uri.parse("content://sms/inbox"), values, "_id=" + SmsMessageId, null);
                    }
                }
                cursor.close();
            }
        } catch(Exception e)
        {
            android.util.Log.e("Mark Read", "Error in Read: "+e.toString());
        }

    }

    public static String md5(final String s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}
