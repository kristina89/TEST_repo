package com.enkeli;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kristina on 9/25/17.
 */

public class AppPreferences {

    private static final String SHARED_PREFERENCES_NAME = "app_preferences";

    private static final String IS_ENABLED_KEY = "IS_ENABLED";
    private static final String NOTIFICATION_MESSAGE_KEY = "NOTIFICATION_MESSAGE";
    private static final String SEND_NOTIFICATION_AFTER_KEY = "SEND_NOTIFICATION_AFTER";
    private static final String IS_SOUND_KEY = "IS_SOUND";

    public static boolean IS_ENABLED_DEFAULT = false;
    public static int SEND_NOTIFICATION_AFTER_DEFAULT = NotificationDuration.MIN_4.getValue(); // 5 min
    public static boolean IS_SOUND_DEFAULT = false;

    private static SharedPreferences sharedPreferences;

    public static void setIsEnabled(boolean enable, Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(IS_ENABLED_KEY, enable);
        editor.commit();
    }

    public static boolean getIsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(IS_ENABLED_KEY, IS_ENABLED_DEFAULT);
    }

    public static void setNotificationMessage(String message, Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(NOTIFICATION_MESSAGE_KEY, message);
        editor.commit();
    }

    public static String getNotificationMessage(Context context) {
        String defaultMessage = context.getString(R.string.default_notification_message);
        String message = getSharedPreferences(context).getString(NOTIFICATION_MESSAGE_KEY, "");
        return message.isEmpty() ? defaultMessage : message;
    }

    public static boolean isNotificationTextDefined(Context context) {
        return !getSharedPreferences(context).getString(NOTIFICATION_MESSAGE_KEY, "").isEmpty();
    }

    public static void setSendNotificationAfter(int minutes, Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt(SEND_NOTIFICATION_AFTER_KEY, minutes);
        editor.commit();
    }

    public static int getSendNotificationAfter(Context context) {
        return getSharedPreferences(context).getInt(SEND_NOTIFICATION_AFTER_KEY, SEND_NOTIFICATION_AFTER_DEFAULT);
    }

    public static void setIsSound(boolean enable, Context context) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(IS_SOUND_KEY, enable);
        editor.commit();
    }

    public static boolean getIsSound(Context context) {
        return getSharedPreferences(context).getBoolean(IS_SOUND_KEY, IS_SOUND_DEFAULT);
    }

    public static SharedPreferences getSharedPreferences(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, 0);
        }
        return sharedPreferences;
    }
}
