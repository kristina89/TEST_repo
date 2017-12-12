package com.enkeli;

/**
 * Created by alexey on 27.09.17.
 */

public enum NotificationDuration {
    MIN_1(1, "1 min"),
    MIN_2(2, "2 min"),
    MIN_3(3, "3 min"),
    MIN_4(5, "5 min (Recommended)");

    private int value;
    private String title;

    NotificationDuration(int value, String title) {
        this.value = value;
        this.title = title;
    }

    public int getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static int getItemPositionByValue(int value) {
        for (int i = 0; i < values().length; i++) {
            if (values()[i].getValue() == value) {
                return i;
            }
        }

        return -1;
    }

    public static String[] getTitles() {
        String[] titlesList = new String[values().length];

        for (int i = 0; i < values().length; i++) {
            titlesList[i] = values()[i].getTitle();
        }

        return titlesList;
    }

    public static NotificationDuration findByValue(int value) {
        for (NotificationDuration notificationDuration : values()) {
            if (notificationDuration.value == value) {
                return notificationDuration;
            }
        }

        return MIN_4;
    }
}
