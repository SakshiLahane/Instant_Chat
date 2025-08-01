package com.example.instant_chat;

import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class TimeUtilsLastSeen {

    public static String formatLastSeen(long timestamp) {
        Calendar now = Calendar.getInstance();
        Calendar lastSeenTime = Calendar.getInstance();
        lastSeenTime.setTimeInMillis(timestamp);

        if (DateFormat.format("ddMMyyyy", now).equals(DateFormat.format("ddMMyyyy", lastSeenTime))) {
            return "today at " + DateFormat.format("hh:mm a", lastSeenTime);
        } else if (now.get(Calendar.DATE) - lastSeenTime.get(Calendar.DATE) == 1) {
            return "yesterday at " + DateFormat.format("hh:mm a", lastSeenTime);
        } else {
            return DateFormat.format("dd MMM, hh:mm a", lastSeenTime).toString();
        }
    }
}
