package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * A class that contains adding and deleting hobbies for elderly
 *
 * @author Jianwei Li
 */
public class AddHobbyActivity extends AppCompatActivity {
    private HashSet<String> hobbies;
    private PreferenceManager preferenceManager;
    private CheckBox cooking, exercise, fishing, gardening, knitting, music, pets, tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_hobby);

        preferenceManager = new PreferenceManager(getApplicationContext());
        hobbies = preferenceManager.getStringSet(Constants.KEY_HOBBIES);
        cooking = findViewById(R.id.addHobby_cooking);
        exercise = findViewById(R.id.addHobby_exercise);
        fishing = findViewById(R.id.addHobby_fishing);
        gardening = findViewById(R.id.addHobby_gardening);
        knitting = findViewById(R.id.addHobby_knitting);
        music = findViewById(R.id.addHobby_music);
        pets = findViewById(R.id.addHobby_pets);
        tv = findViewById(R.id.addHobby_tv);

        // Initialize the hobbies that the user has selected
        if (hobbies != null && hobbies.size() > 0) {
            for (String s : hobbies) {
                presetHobbiesSelection(s);
            }
        }

        cooking.setOnClickListener(view -> {
            if (cooking.isChecked()) {
                hobbies.add(cooking.getText().toString());
            } else {
                hobbies.remove(cooking.getText().toString());
            }
        });

        exercise.setOnClickListener(view -> {
            if (exercise.isChecked()) {
                hobbies.add(exercise.getText().toString());
            } else {
                hobbies.remove(exercise.getText().toString());
            }
        });

        fishing.setOnClickListener(view -> {
            if (fishing.isChecked()) {
                hobbies.add(fishing.getText().toString());
            } else {
                hobbies.remove(fishing.getText().toString());
            }
        });

        gardening.setOnClickListener(view -> {
            if (gardening.isChecked()) {
                hobbies.add(gardening.getText().toString());
            } else {
                hobbies.remove(gardening.getText().toString());
            }
        });

        knitting.setOnClickListener(view -> {
            if (knitting.isChecked()) {
                hobbies.add(knitting.getText().toString());
            } else {
                hobbies.remove(knitting.getText().toString());
            }
        });

        music.setOnClickListener(view -> {
            if (music.isChecked()) {
                hobbies.add(music.getText().toString());
            } else {
                hobbies.remove(music.getText().toString());
            }
        });

        pets.setOnClickListener(view -> {
            if (pets.isChecked()) {
                hobbies.add(pets.getText().toString());
            } else {
                hobbies.remove(pets.getText().toString());
            }
        });

        tv.setOnClickListener(view -> {
            if (tv.isChecked()) {
                hobbies.add("Films/TV");
            } else {
                hobbies.remove("Films/TV");
            }
        });

        // go back button
        findViewById(R.id.addHobby_back).setOnClickListener(view -> {
            hobbies.clear();
            onBackPressed();
            finish();
        });

        // done button
        findViewById(R.id.addHobby_done).setOnClickListener(view -> {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            String myID = preferenceManager.getString(Constants.KEY_USER_ID);
            database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(myID)
                    .update(Constants.KEY_HOBBIES, new ArrayList<>(hobbies))
                    .addOnSuccessListener(unused ->
                            preferenceManager.putStringSet(Constants.KEY_HOBBIES, hobbies))
                    .addOnFailureListener(e -> Toast.makeText(
                            getApplicationContext(),
                            "Sync failed",
                            Toast.LENGTH_SHORT).show());
            onBackPressed();
            finish();
        });
    }

    /**
     * Preset the user’s hobbies, the hobbies that have been added are preset as selected,
     * and the hobbies that have not been added are preset as unselected
     * @param hobby Hobby to be set as selected
     */
    private void presetHobbiesSelection(String hobby) {
        switch (hobby) {
            case "Cooking":
                cooking.setChecked(true);
                break;
            case "Exercise":
                exercise.setChecked(true);
                break;
            case "Fishing":
                fishing.setChecked(true);
                break;
            case "Gardening":
                gardening.setChecked(true);
                break;
            case "Knitting":
                knitting.setChecked(true);
                break;
            case "Music":
                music.setChecked(true);
                break;
            case "Pets":
                pets.setChecked(true);
                break;
            case "Films/TV":
                tv.setChecked(true);
                break;
        }
    }
}