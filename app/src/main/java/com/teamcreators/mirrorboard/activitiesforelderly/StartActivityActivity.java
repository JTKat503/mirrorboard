package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.adapters.HobbiesAdapter;
import com.teamcreators.mirrorboard.listeners.ItemsListener;
import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.NetworkConnection;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * A class that shows the user’s hobbies and provides access to edit user profile
 *
 * @author Jianwei Li
 */
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

        preferenceManager = new PreferenceManager(getApplicationContext());
        textErrorMessage = findViewById(R.id.startActivity_errorMessage);
        swipeRefreshLayout = findViewById(R.id.startActivity_hobbiesLayout);
        Button editProfile = findViewById(R.id.startActivity_editProfile);
        Button addHobby = findViewById(R.id.startActivity_addHobby);
        Button exit = findViewById(R.id.startActivity_exit);
        Button family = findViewById(R.id.startActivity_family);
        Button exitApp = findViewById(R.id.startActivity_exitApp);
        LinearLayout offlineWarning = findViewById(R.id.startActivity_offlineWarning);

        // Monitor network connection changes
        NetworkConnection networkConnection = new NetworkConnection(getApplicationContext());
        networkConnection.observe(this, isConnected -> {
            if (isConnected) {
                editProfile.setVisibility(View.VISIBLE);
                addHobby.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setVisibility(View.VISIBLE);
                exit.setVisibility(View.VISIBLE);
                family.setVisibility(View.VISIBLE);
                offlineWarning.setVisibility(View.GONE);
            } else {
                offlineWarning.setVisibility(View.VISIBLE);
                editProfile.setVisibility(View.GONE);
                addHobby.setVisibility(View.GONE);
                swipeRefreshLayout.setVisibility(View.GONE);
                exit.setVisibility(View.GONE);
                family.setVisibility(View.GONE);
            }
        });

        // building and loading hobbies list
        RecyclerView hobbiesRecyclerView = findViewById(R.id.startActivity_hobbiesView);
        hobbies = new ArrayList<>();
        hobbiesAdapter = new HobbiesAdapter(hobbies, this);
        hobbiesRecyclerView.setAdapter(hobbiesAdapter);
        getHobbies();

        // refreshing hobbies list
        swipeRefreshLayout.setOnRefreshListener(this::getHobbies);

        // editing Profile button
        editProfile.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivityActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // adding new hobbies button
        addHobby.setOnClickListener(view -> {
            Intent intent = new Intent(StartActivityActivity.this, AddHobbyActivity.class);
            startActivity(intent);
        });

        // going back to main interface button
        family.setOnClickListener(view -> {
            onBackPressed();
            finish();
        });

        // exit app button
        exit.setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });

        // exit app button on the offline warning page
        exitApp.setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }

    /**
     * Get all this user's hobbies names from the local file
     * and load the hobbies list
     */
    private void getHobbies() {
//        // check if internet connection is available
//        if (!isNetworkConnected()) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(StartActivityActivity.this);
//            builder.setTitle("No Internet Connection")
//                    .setMessage("Please reconnect and try again.")
//                    .setPositiveButton(android.R.string.yes, null).show();
//            swipeRefreshLayout.setRefreshing(false);
//            return;
//        }
        hobbies.clear();
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        String myID = preferenceManager.getString(Constants.KEY_USER_ID);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(myID)
                .get()
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            List<String> myHobbiesNames = (List<String>) document.get(Constants.KEY_HOBBIES);
                            HashSet<String> myHobbiesSet = new HashSet<>(myHobbiesNames);
                            preferenceManager.putStringSet(Constants.KEY_HOBBIES, myHobbiesSet);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Sync failed", Toast.LENGTH_SHORT).show();
                    }
                });
        HashSet<String> myHobbies = preferenceManager.getStringSet(Constants.KEY_HOBBIES);
        if (!myHobbies.isEmpty()) {
            for (String hobbyName : myHobbies) {
                Hobby hobby = new Hobby();
                hobby.name = hobbyName;
                hobby.icon = getHobbyIcon(hobbyName);
                hobbies.add(hobby);
            }
            if (hobbies.size() > 0) {
                hobbiesAdapter.notifyDataSetChanged();
                textErrorMessage.setVisibility(View.GONE);
            } else {
                textErrorMessage.setText(String.format("%s", "No Hobbies"));
                textErrorMessage.setVisibility(View.VISIBLE);
            }
        } else {
            textErrorMessage.setText(String.format("%s", "No Hobbies"));
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

//    /**
//     * Check if the device is connected to the network
//     * @return true if is connected, false if not
//     */
//    private boolean isNetworkConnected() {
//        ConnectivityManager connectivityManager
//                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
//        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
//    }
}