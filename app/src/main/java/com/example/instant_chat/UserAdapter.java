package com.example.instant_chat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<UserModel> userList;
    private OnUserLongClickListener longClickListener;

    private OnUserClickListener listener;


    public interface OnUserLongClickListener {
        void onUserLongClick(UserModel user);
    }

    public interface OnUserClickListener {
        void onUserClick(UserModel user);
    }


    public UserAdapter(Context context, List<UserModel> userList,OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;

    }
    public void setOnUserLongClickListener(OnUserLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);

        holder.nameTextView.setText(user.getName());
        holder.emailTextView.setText(user.getEmail());

        long timestamp = user.getLastMessageTime();
        if (timestamp > 0) {
            String timeString = TimeUtilsUserlist.formatTimestamp(context, timestamp);
            holder.userlistTime.setText(timeString);
        } else {
            holder.userlistTime.setText(""); // or "No messages yet"
        }




        Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.person)
                .circleCrop()
                .into(holder.profileImageView);



        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }

            Intent intent = new Intent(context, Chatting.class);
            intent.putExtra("receiverUid", user.getUid());
            intent.putExtra("name", user.getName());
            intent.putExtra("profileImageUrl", user.getProfileImageUrl());
            context.startActivity(intent);
        });


        holder.itemView.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle("Delete User")
                    .setMessage("Are you sure you want to delete this user?")
                    .setIcon(R.drawable.delete) // Remove this line temporarily if not visible
                    .setPositiveButton("Delete", (dialog, which) -> {
                        int currentPosition = holder.getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            UserModel userToDelete = userList.get(currentPosition);

                            if (longClickListener != null) {
                                longClickListener.onUserLongClick(userToDelete);
                            }

                            userList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);

                            // Show custom toast
                            LayoutInflater inflater = LayoutInflater.from(context);
                            View layout = inflater.inflate(R.layout.custom_toast, null);

                            TextView toastText = layout.findViewById(R.id.toast_text);
                            ImageView toastIcon = layout.findViewById(R.id.toast_icon);

                            toastText.setText("User Deleted");
                            toastText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                            toastIcon.setImageResource(R.drawable.delete);
                            toastIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_red_dark));

                            Toast toast = new Toast(context);
                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(layout);
                            toast.show();
                        }
                    })
                    .setNegativeButton("Cancel", null);

            AlertDialog dialog = builder.create();
            dialog.show(); // Important!

            return true;
        });
    }


        @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView, emailTextView, userlistTime;


        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.userImage);
            nameTextView = itemView.findViewById(R.id.userName);
            emailTextView = itemView.findViewById(R.id.userEmail);
            userlistTime = itemView.findViewById(R.id.userlistTime); // Add this TextView in item_user.xml

        }
    }
}
