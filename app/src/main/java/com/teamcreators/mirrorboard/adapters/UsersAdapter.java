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
import com.teamcreators.mirrorboard.listeners.ItemsListener;
import com.teamcreators.mirrorboard.models.User;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for building adapters connecting users and RecyclerViews
 *
 * @author Jianwei Li
 */
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {
    private List<User> users;
    private ItemsListener usersListener;
    private List<User> selectedUsers;

    // constructor for UsersAdapter
    public UsersAdapter(List<User> users, ItemsListener usersListener) {
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
     * ViewHolder for setting user data
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        TextView textUserName;
        ImageView imageUserAvatar;
        ConstraintLayout userContainer;
        ImageView imageSelected;

        // constructor for UserViewHolder
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            textUserName = itemView.findViewById(R.id.userContainer_userName);
            imageUserAvatar = itemView.findViewById(R.id.userContainer_userAvatar);
            userContainer = itemView.findViewById(R.id.userContainer);
            imageSelected = itemView.findViewById(R.id.userContainer_selected);
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

            /*
            * In multi-selected state:
            *       click unselected user -> selected
            *       click selected user   -> deselected
            *       click the last selected user -> quit multi-selected state
            * Not in multi-selected state:
            *       click user -> display user information
            * */
            userContainer.setOnClickListener(view -> {
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
            });

            /*
            * In accordance with the requirements of the UX Design team,
            * multi-party call function is disabled.
            * */
            // Long press the user to add the user to the multi-party call invitation list
            // Enter multiple selection state
//            userContainer.setOnLongClickListener(view -> {
//                // prevent repeated selection of the same contact
//                if (imageSelected.getVisibility() != View.VISIBLE) {
//                    selectedUsers.add(user);
//                    imageSelected.setVisibility(View.VISIBLE);
//                    usersListener.onMultipleUsersAction(true);
//                }
//                return true;
//            });
        }
    }
}
