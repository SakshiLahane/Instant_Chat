package com.example.instant_chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int SENDER = 0;
    private static final int RECEIVER = 1;

    private final Context context;
    private final List<MessageModel> messageList;
    private final String currentUserId;

    private OnMessageLongClickListener longClickListener;

    public MessageAdapter(Context context, List<MessageModel> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageModel message);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messageList.get(position);
        return message.getSenderId().equals(currentUserId) ? SENDER : RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SENDER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_sent, parent, false);
            return new SenderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_received, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        String formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(message.getTimestamp()));

        if (holder.getItemViewType() == SENDER) {
            SenderViewHolder senderHolder = (SenderViewHolder) holder;

            if ("image".equals(message.getType()) && message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                // FIXED: Show image message
                senderHolder.messageText.setVisibility(View.GONE);
                senderHolder.messageImage.setVisibility(View.VISIBLE);

                // CRITICAL FIX: Use getMediaUrl() instead of getMessage()
                Glide.with(context)
                        .load(message.getMediaUrl()) // ✅ FIXED: Was message.getMessage()
                        .placeholder(R.drawable.ic_camera) // Add placeholder
                        .error(R.drawable.scanner) // Add error image
                        .into(senderHolder.messageImage);

                // Show caption if exists
                if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                    senderHolder.messageText.setVisibility(View.VISIBLE);
                    senderHolder.messageText.setText(message.getMessage()); // Caption
                }

            } else if ("deleted_sender".equals(message.getType()) || message.isDeleted()) {
                // Handle deleted messages
                senderHolder.messageImage.setVisibility(View.GONE);
                senderHolder.messageText.setVisibility(View.VISIBLE);
                senderHolder.messageText.setText("You deleted this message");
                senderHolder.messageText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));

            } else {
                // Handle text messages
                senderHolder.messageImage.setVisibility(View.GONE);
                senderHolder.messageText.setVisibility(View.VISIBLE);
                senderHolder.messageText.setText(message.getMessage());
                senderHolder.messageText.setTextColor(context.getResources().getColor(android.R.color.black));
            }

            senderHolder.messageTime.setText(formattedTime);

            // Tick status
            if (message.isSeen()) {
                senderHolder.tickStatus.setImageResource(R.drawable.double_tick_blue);
            } else if (message.isDelivered()) {
                senderHolder.tickStatus.setImageResource(R.drawable.double_tick_gray);
            } else {
                senderHolder.tickStatus.setImageResource(R.drawable.ic_single_tick);
            }

            senderHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message);
                }
                return true;
            });

        } else {
            ReceiverViewHolder receiverHolder = (ReceiverViewHolder) holder;

            if ("image".equals(message.getType()) && message.getMediaUrl() != null && !message.getMediaUrl().isEmpty()) {
                // FIXED: Show image message
                receiverHolder.messageText.setVisibility(View.GONE);
                receiverHolder.messageImage.setVisibility(View.VISIBLE);

                // CRITICAL FIX: Use getMediaUrl() instead of getMessage()
                Glide.with(context)
                        .load(message.getMediaUrl()) // ✅ FIXED: Was message.getMessage()
                        .placeholder(R.drawable.calls) // Add placeholder
                        .error(R.drawable.delete) // Add error image
                        .into(receiverHolder.messageImage);

                // Show caption if exists
                if (message.getMessage() != null && !message.getMessage().trim().isEmpty()) {
                    receiverHolder.messageText.setVisibility(View.VISIBLE);
                    receiverHolder.messageText.setText(message.getMessage()); // Caption
                }

            } else if ("deleted_sender".equals(message.getType()) || message.isDeleted()) {
                // Handle deleted messages
                receiverHolder.messageImage.setVisibility(View.GONE);
                receiverHolder.messageText.setVisibility(View.VISIBLE);
                receiverHolder.messageText.setText("This message was deleted");
                receiverHolder.messageText.setTextColor(context.getResources().getColor(android.R.color.darker_gray));

            } else {
                // Handle text messages
                receiverHolder.messageImage.setVisibility(View.GONE);
                receiverHolder.messageText.setVisibility(View.VISIBLE);
                receiverHolder.messageText.setText(message.getMessage());
                receiverHolder.messageText.setTextColor(context.getResources().getColor(android.R.color.black));
            }

            receiverHolder.messageTime.setText(formattedTime);

            receiverHolder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class SenderViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        ImageView tickStatus, messageImage;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.sendMessageText);
            messageTime = itemView.findViewById(R.id.sendMessageTime);
            tickStatus = itemView.findViewById(R.id.tickIcon);
            messageImage = itemView.findViewById(R.id.sendImageView);
        }
    }

    static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, messageTime;
        ImageView messageImage;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.receiveMessageText);
            messageTime = itemView.findViewById(R.id.receiveMessageTime);
            messageImage = itemView.findViewById(R.id.receiveImageView);
        }
    }
}