package com.example.instant_chat;

import android.content.Context;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

public class TimeUtilsUserlist {

    public static String formatTimestamp(Context context, long timestamp) {
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(timestamp);

        Calendar now = Calendar.getInstance();

        if (isSameDay(messageTime, now)) {
            // Today: show "Today at 5:32 PM"
            return "" + DateFormat.format("hh:mm a", messageTime);
        } else {
            now.add(Calendar.DATE, -1);
            if (isSameDay(messageTime, now)) {
                // Yesterday
                return "Yesterday" + DateFormat.format("", messageTime);
            } else {
                // Older: show date like "15 Jul 2025"
                return DateFormat.format("dd MMM yyyy", messageTime).toString();
            }
        }
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
