package com.teamcreators.mirrorboard.adapters;

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
import com.teamcreators.mirrorboard.models.Hobby;

import java.util.List;

/**
 * A class for building adapters connecting hobbies and RecyclerViews
 *
 * @author Jianwei Li
 */
public class HobbiesAdapter extends RecyclerView.Adapter<HobbiesAdapter.HobbyViewHolder> {
    private List<Hobby> hobbies;
    private ItemsListener hobbiesListener;

    // constructor for HobbiesAdapter
    public HobbiesAdapter(List<Hobby> hobbies, ItemsListener hobbiesListener) {
        this.hobbies = hobbies;
        this.hobbiesListener = hobbiesListener;
    }

    @NonNull
    @Override
    public HobbyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HobbyViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_container_hobby,
                parent,
                false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull HobbyViewHolder holder, int position) {
        holder.setHobbyData(hobbies.get(position));
    }

    @Override
    public int getItemCount() {
        return hobbies.size();
    }

    /**
     * ViewHolder for setting hobby data
     */
    class HobbyViewHolder extends RecyclerView.ViewHolder {
        TextView textHobbyName;
        ImageView imageHobbyIcon;
        ConstraintLayout hobbyContainer;

        // constructor for HobbyViewHolder
        HobbyViewHolder(@NonNull View itemView) {
            super(itemView);
            textHobbyName = itemView.findViewById(R.id.hobbyContainer_hobbyName);
            imageHobbyIcon = itemView.findViewById(R.id.hobbyContainer_hobbyIcon);
            hobbyContainer = itemView.findViewById(R.id.hobbyContainer);
        }

        /**
         * Initialize the information of each hobby in the hobbies list.
         * @param hobby the hobby to be initialized
         */
        void setHobbyData(Hobby hobby) {
            textHobbyName.setText(hobby.name);
            Glide.with(itemView)
                    .load(hobby.icon)
                    .fitCenter()
                    .error(R.drawable.blank_hobby_image)
                    .into(imageHobbyIcon);

            // each tuple of hobbies
            hobbyContainer.setOnClickListener(view ->
                    hobbiesListener.displayHobbyInformation(hobby));
        }
    }
}
