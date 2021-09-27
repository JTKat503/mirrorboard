package com.teamcreators.mirrorboard.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.listeners.UsersListener;
import com.teamcreators.mirrorboard.models.User;

import java.util.List;

public class UsersAdapterElderly extends RecyclerView.Adapter<UsersAdapterElderly.UserViewHolder> {

    private List<User> users;
    private UsersListener usersListener;

    public UsersAdapterElderly(List<User> users, UsersListener usersListener) {
        this.users = users;
        this.usersListener = usersListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_container_user,
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName;
        ImageView imageUserAvatar;
        ConstraintLayout userContainer;
//        Button makeVideoCall;
//        ImageView imageAudioCall;

        // constructor
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.userContainer_userName);
            imageUserAvatar = itemView.findViewById(R.id.userContainer_imageAvatar);
            userContainer = itemView.findViewById(R.id.userContainer);
//            makeVideoCall = itemView.findViewById(R.id.contactInfo_makeCall_button);
//            imageVideoCall = itemView.findViewById(R.id.userContainer_more_imageView);
//            imageAudioCall = itemView.findViewById()
        }

        void setUserData(User user) {
            textUserName.setText(user.name);
            Glide.with(itemView)
                    .load(Uri.parse(user.avatarUri))
                    .fitCenter()
                    .error(R.drawable.blank_profile)
                    .into(imageUserAvatar);
//            userContainer.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Intent intent = new Intent(context, InfoContactActivity.class);
//                    Bundle bundle = new Bundle();
//                    bundle.putString("name", user.name);
//                    bundle.putString("avatarUri", user.avatarUri);
//                    intent.putExtras(bundle);
//                    context.startActivity(intent);
//                }
//            });

//            imageVideoCall.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    usersListener.initiateVideoMeeting(user);
//                }
//            });

//            imageAudioCall.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    usersListener.initiateAudioMeeting(user);
//                }
//            });

            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    usersListener.checkContactInformation(user);
                }
            });

//            makeVideoCall.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    usersListener.initiateVideoMeeting(user);
//                }
//            });
        }
    }
}
