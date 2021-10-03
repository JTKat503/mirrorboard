package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

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
        cooking = findViewById(R.id.addHobby_cooking_checkbox);
        exercise = findViewById(R.id.addHobby_exercise_checkbox);
        fishing = findViewById(R.id.addHobby_fishing_checkbox);
        gardening = findViewById(R.id.addHobby_gardening_checkbox);
        knitting = findViewById(R.id.addHobby_knitting_checkbox);
        music = findViewById(R.id.addHobby_music_checkbox);
        pets = findViewById(R.id.addHobby_pets_checkbox);
        tv = findViewById(R.id.addHobby_tv_checkbox);

        if (hobbies != null && hobbies.size() > 0) {
            for (String s : hobbies) {
                presetHobbiesSelection(s);
            }
        }

        cooking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cooking.isChecked()) {
                    hobbies.add(cooking.getText().toString());
                } else {
                    hobbies.remove(cooking.getText().toString());
                }
            }
        });

        exercise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (exercise.isChecked()) {
                    hobbies.add(exercise.getText().toString());
                } else {
                    hobbies.remove(exercise.getText().toString());
                }
            }
        });

        fishing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fishing.isChecked()) {
                    hobbies.add(fishing.getText().toString());
                } else {
                    hobbies.remove(fishing.getText().toString());
                }
            }
        });

        gardening.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gardening.isChecked()) {
                    hobbies.add(gardening.getText().toString());
                } else {
                    hobbies.remove(gardening.getText().toString());
                }
            }
        });

        knitting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (knitting.isChecked()) {
                    hobbies.add(knitting.getText().toString());
                } else {
                    hobbies.remove(knitting.getText().toString());
                }
            }
        });

        music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (music.isChecked()) {
                    hobbies.add(music.getText().toString());
                } else {
                    hobbies.remove(music.getText().toString());
                }
            }
        });

        pets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pets.isChecked()) {
                    hobbies.add(pets.getText().toString());
                } else {
                    hobbies.remove(pets.getText().toString());
                }
            }
        });

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tv.isChecked()) {
                    hobbies.add("Films/TV");
                } else {
                    hobbies.remove("Films/TV");
                }
            }
        });

        // go back button
        findViewById(R.id.addHobby_back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hobbies.clear();
                onBackPressed();
                finish();
            }
        });

        // done button
        findViewById(R.id.addHobby_done_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                String myID = preferenceManager.getString(Constants.KEY_USER_ID);
                database.collection(Constants.KEY_COLLECTION_USERS)
                        .document(myID)
                        .update(Constants.KEY_HOBBIES, new ArrayList<>(hobbies))
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                preferenceManager.putStringSet(Constants.KEY_HOBBIES, hobbies);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(), "Sync failed", Toast.LENGTH_SHORT).show();
                            }
                        });
                onBackPressed();
                finish();
            }
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