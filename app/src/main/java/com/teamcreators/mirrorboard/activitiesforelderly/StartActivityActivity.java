package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.adapters.HobbiesAdapter;
import com.teamcreators.mirrorboard.listeners.ItemsListener;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class StartActivityActivity extends AppCompatActivity implements ItemsListener {

    private List<Hobby> hobbies;
    private HobbiesAdapter hobbiesAdapter;
    private TextView textErrorMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_activity);

        Button editProfile = findViewById(R.id.startActivity_editProfile_button);
        Button addHobby = findViewById(R.id.startActivity_addHobby_button);
        Button exit = findViewById(R.id.startActivity_exit_button);
        Button family = findViewById(R.id.startActivity_family_button);
        textErrorMessage = findViewById(R.id.startActivity_errorMessage_textView);
        swipeRefreshLayout = findViewById(R.id.startActivity_swipeRefreshLayout);
        preferenceManager = new PreferenceManager(getApplicationContext());

        // building and loading hobbies list
        RecyclerView hobbiesRecyclerView = findViewById(R.id.startActivity_hobbies_RecyclerView);
        hobbies = new ArrayList<>();
        hobbiesAdapter = new HobbiesAdapter(hobbies, this);
        hobbiesRecyclerView.setAdapter(hobbiesAdapter);
        getHobbies();

        // refreshing hobbies list
        swipeRefreshLayout.setOnRefreshListener(this::getHobbies);

        // editing Profile button
        editProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivityActivity.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });

        // adding new hobbies button
        addHobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(StartActivityActivity.this, AddHobbyActivity.class);
                startActivity(intent);
            }
        });

        // going back to main interface button
        family.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        // exit app button
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveTaskToBack(true);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        });
    }

    /**
     * Get all this user's hobbies names from the local file
     * and load the hobbies list
     */
    private void getHobbies() {
        swipeRefreshLayout.setRefreshing(true);
        HashSet<String> myHobbies = preferenceManager.getStringSet(Constants.KEY_HOBBIES);
        swipeRefreshLayout.setRefreshing(false);
        if (myHobbies != null) {
            hobbies.clear();
            for (String hobbyName : myHobbies) {
                Hobby hobby = new Hobby();
                hobby.name = hobbyName;
                hobby.drawable = getHobbyIcon(hobbyName);
                hobbies.add(hobby);
            }
            if (hobbies.size() > 0) {
                hobbiesAdapter.notifyDataSetChanged();
                textErrorMessage.setVisibility(View.GONE);
            } else {
                textErrorMessage.setText(String.format("%s", "No hobbies added"));
                textErrorMessage.setVisibility(View.VISIBLE);
            }
        } else {
            textErrorMessage.setText(String.format("%s", "No hobbies added"));
            textErrorMessage.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Obtain the icon's code of this hobby according to the entered hobby name
     * @param hobbyName The name of the hobby for which the icon is to be obtained
     * @return the icon's code of this hobby
     */
    private int getHobbyIcon(String hobbyName) {
        switch (hobbyName) {
            case "Cooking":
                return R.drawable.hobby_icon_cooking;
            case "Exercise":
                return R.drawable.hobby_icon_exercise;
            case "Fishing":
                return R.drawable.hobby_icon_fishing;
            case "Gardening":
                return R.drawable.hobby_icon_gardening;
            case "Knitting":
                return R.drawable.hobby_icon_knitting;
            case "Music":
                return R.drawable.hobby_icon_music;
            case "Pets":
                return R.drawable.hobby_icon_pets;
            case "Films/TV":
                return R.drawable.hobby_icon_tv;
            default:
                return R.drawable.blank_hobby_image;
        }
    }

    /**
     * No need to implement method
     */
    @Override
    public void displayContactInformation(User user) {}

    /**
     * Implementation of ItemsAdapter interface method.
     * Jump to the MatchHobbyActivity and display the hobby information of the selected hobby
     * @param hobby the hobby selected by clicking, whose information is to be displayed
     */
    @Override
    public void displayHobbyInformation(Hobby hobby) {
        Intent intent = new Intent(getApplicationContext(), MatchHobbyActivity.class);
        intent.putExtra("hobby", hobby);
        startActivity(intent);
    }

    /**
     * No need to implement method
     */
    @Override
    public void onMultipleUsersAction(Boolean isMultipleUsersSelected) {}
}