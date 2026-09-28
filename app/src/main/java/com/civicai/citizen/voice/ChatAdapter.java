package com.civicai.citizen.voice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.civicai.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public static class ChatMessage {
        public String text;
        public boolean isUser;
        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        notifyItemInserted(messages.size() - 1);
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (msg.isUser) {
            holder.userContainer.setVisibility(View.VISIBLE);
            holder.assistantContainer.setVisibility(View.GONE);
            holder.userText.setText(msg.text);
        } else {
            holder.userContainer.setVisibility(View.GONE);
            holder.assistantContainer.setVisibility(View.VISIBLE);
            holder.assistantText.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View userContainer;
        View assistantContainer;
        TextView userText;
        TextView assistantText;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userContainer = itemView.findViewById(R.id.userMessageContainer);
            assistantContainer = itemView.findViewById(R.id.assistantMessageContainer);
            userText = itemView.findViewById(R.id.tvUserMessage);
            assistantText = itemView.findViewById(R.id.tvAssistantMessage);
        }
    }
}
