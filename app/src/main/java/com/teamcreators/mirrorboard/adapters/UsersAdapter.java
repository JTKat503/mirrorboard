package com.teamcreators.mirrorboard.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.listeners.UsersListener;
import com.teamcreators.mirrorboard.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> users;
    private UsersListener usersListener;
    private List<User> selectedUsers;

    public UsersAdapter(List<User> users, UsersListener usersListener) {
        this.users = users;
        this.usersListener = usersListener;
        selectedUsers = new ArrayList<>();
    }

    /**
     * A getter method for selected contacts
     * @return selected contacts
     */
    public List<User> getSelectedUsers() {
        return selectedUsers;
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

    /**
     *
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName;
        ImageView imageUserAvatar;
        ConstraintLayout userContainer;
        ImageView imageSelected;
//        Button makeVideoCall;
//        ImageView imageAudioCall;

        // constructor
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.userContainer_userName);
            imageUserAvatar = itemView.findViewById(R.id.userContainer_imageAvatar);
            userContainer = itemView.findViewById(R.id.userContainer);
            imageSelected = itemView.findViewById(R.id.userContainer_imageSelected);
//            makeVideoCall = itemView.findViewById(R.id.contactInfo_makeCall_button);
//            imageVideoCall = itemView.findViewById(R.id.userContainer_more_imageView);
//            imageAudioCall = itemView.findViewById()
        }

        /**
         * Initialize the information of each contact in the contact list.
         * @param user the contact to be initialized
         */
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

            /*
            In multi-selected state:
                click unselected user -> selected
                click selected user   -> deselected
                click the last selected user -> quit multi-selected state
            Not in multi-selected state:
                click user -> display user information
             */
            userContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (imageSelected.getVisibility() != View.VISIBLE) {
                        if (selectedUsers.size() == 0) {
                            usersListener.displayContactInformation(user);
                        } else {
                            selectedUsers.add(user);
                            imageSelected.setVisibility(View.VISIBLE);
                        }
                    } else {
                        selectedUsers.remove(user);
                        imageSelected.setVisibility(View.GONE);
                        if (selectedUsers.size() == 0) {
                            usersListener.onMultipleUsersAction(false);
                        }
                    }
                }
            });

//            makeVideoCall.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    usersListener.initiateVideoMeeting(user);
//                }
//            });

            // Long press the user to add the user to the multi-party call invitation list
            // Enter multiple selection state
            userContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // prevent repeated selection of the same contact
                    if (imageSelected.getVisibility() != View.VISIBLE) {
                        selectedUsers.add(user);
                        imageSelected.setVisibility(View.VISIBLE);
                        usersListener.onMultipleUsersAction(true);
                    }
                    return true;
                }
            });
        }
    }
}
