package com.civicai.government;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.civicai.R;
import com.civicai.common.UiUtils;
import com.civicai.model.Complaint;
import com.civicai.model.Priority;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the Government Priority Queue.
 * Displays complaints with priority colour-coding and AI severity.
 */
public class GovComplaintAdapter extends RecyclerView.Adapter<GovComplaintAdapter.ViewHolder> {

    public interface OnComplaintClickListener {
        void onComplaintClicked(Complaint complaint);
    }

    private List<Complaint> complaints = new ArrayList<>();
    private final OnComplaintClickListener listener;

    public GovComplaintAdapter(OnComplaintClickListener listener) {
        this.listener = listener;
    }

    public void setComplaints(List<Complaint> list) {
        this.complaints = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Complaint> getComplaints() {
        return complaints;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gov_complaint_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Complaint c = complaints.get(position);
        Context ctx = holder.itemView.getContext();

        // Priority badge
        Priority p = c.getEffectivePriority();
        String priorityLabel = p != null ? p.name() : "MED";
        holder.priorityBadge.setText(priorityLabel);
        if (p == Priority.HIGH) {
            holder.priorityBadge.setTextColor(ctx.getColor(R.color.high_priority));
            holder.priorityBadge.setBackgroundResource(R.drawable.bg_badge_high_priority);
            // High priority items get a subtle left-border tint via card stroke
            holder.card.setStrokeColor(ctx.getColor(R.color.high_priority));
            holder.card.setStrokeWidth(2);
        } else if (p == Priority.LOW) {
            holder.priorityBadge.setTextColor(ctx.getColor(R.color.low_priority));
            holder.priorityBadge.setBackgroundResource(R.drawable.bg_badge_low_priority);
            holder.card.setStrokeColor(ctx.getColor(R.color.border_color));
            holder.card.setStrokeWidth(1);
        } else {
            holder.priorityBadge.setTextColor(ctx.getColor(R.color.medium_priority));
            holder.priorityBadge.setBackgroundResource(R.drawable.bg_badge_medium_priority);
            holder.card.setStrokeColor(ctx.getColor(R.color.border_color));
            holder.card.setStrokeWidth(1);
        }

        // Complaint ID
        holder.complaintId.setText(c.getComplaintId() != null ? "#" + c.getComplaintId() : "");

        // Status badge
        if (c.getStatus() != null) {
            holder.statusBadge.setText(c.getStatus().getDisplayName());
            holder.statusBadge.setTextColor(UiUtils.getStatusColor(ctx, c.getStatus()));
        }

        // Title
        holder.title.setText(c.getTitle() != null ? c.getTitle() : "Untitled");

        // Category
        holder.category.setText(c.getCategory() != null ? c.getCategory() : "—");

        // Location
        holder.location.setText(c.getLocationAddress() != null ? c.getLocationAddress() : "—");

        // AI Severity
        String aiSev = c.getAiSeverity();
        String aiUrgency = c.getAiUrgency();
        StringBuilder aiInfo = new StringBuilder();
        if (aiSev != null && !aiSev.isEmpty()) aiInfo.append(aiSev);
        if (aiUrgency != null && !aiUrgency.isEmpty()) {
            if (aiInfo.length() > 0) aiInfo.append(" · ");
            aiInfo.append(aiUrgency);
        }
        holder.aiSeverity.setText(aiInfo.length() > 0 ? aiInfo.toString() : "No AI data");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onComplaintClicked(c);
        });
    }

    @Override
    public int getItemCount() {
        return complaints.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView priorityBadge;
        final TextView complaintId;
        final TextView statusBadge;
        final TextView title;
        final TextView category;
        final TextView location;
        final TextView aiSeverity;

        ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            priorityBadge = itemView.findViewById(R.id.item_priority_badge);
            complaintId = itemView.findViewById(R.id.item_complaint_id);
            statusBadge = itemView.findViewById(R.id.item_status_badge);
            title = itemView.findViewById(R.id.item_title);
            category = itemView.findViewById(R.id.item_category);
            location = itemView.findViewById(R.id.item_location);
            aiSeverity = itemView.findViewById(R.id.item_ai_severity);
        }
    }
}
