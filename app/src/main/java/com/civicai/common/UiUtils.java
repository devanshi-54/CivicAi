package com.civicai.common;

import android.content.Context;
import androidx.core.content.ContextCompat;
import com.civicai.R;
import com.civicai.model.ComplaintStatus;
import com.civicai.model.Priority;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * UI styling and formatting helpers matching the CivicAI Stitch Design System.
 */
public final class UiUtils {
    private UiUtils() {}

    public static int getPriorityColor(Context context, Priority priority) {
        if (priority == null) return ContextCompat.getColor(context, R.color.medium_priority);
        switch (priority) {
            case HIGH:
                return ContextCompat.getColor(context, R.color.high_priority);
            case LOW:
                return ContextCompat.getColor(context, R.color.low_priority);
            case MEDIUM:
            default:
                return ContextCompat.getColor(context, R.color.medium_priority);
        }
    }

    public static int getPriorityBgColor(Context context, Priority priority) {
        if (priority == null) return ContextCompat.getColor(context, R.color.medium_priority_bg);
        switch (priority) {
            case HIGH:
                return ContextCompat.getColor(context, R.color.high_priority_bg);
            case LOW:
                return ContextCompat.getColor(context, R.color.low_priority_bg);
            case MEDIUM:
            default:
                return ContextCompat.getColor(context, R.color.medium_priority_bg);
        }
    }

    public static int getStatusColor(Context context, ComplaintStatus status) {
        if (status == null) return ContextCompat.getColor(context, R.color.status_submitted);
        switch (status) {
            case UNDER_REVIEW:
                return ContextCompat.getColor(context, R.color.status_under_review);
            case ASSIGNED:
                return ContextCompat.getColor(context, R.color.status_assigned);
            case IN_PROGRESS:
                return ContextCompat.getColor(context, R.color.status_in_progress);
            case RESOLVED:
                return ContextCompat.getColor(context, R.color.status_resolved);
            case REJECTED:
                return ContextCompat.getColor(context, R.color.status_rejected);
            case SUBMITTED:
            default:
                return ContextCompat.getColor(context, R.color.status_submitted);
        }
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
