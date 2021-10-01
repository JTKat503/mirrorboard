package com.teamcreators.mirrorboard.activitiesmutual;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.teamcreators.mirrorboard.R;
import com.teamcreators.mirrorboard.activitiesforelderly.MainActivityElderly;
import com.teamcreators.mirrorboard.activitiesforfamily.MainActivityFamily;
import com.teamcreators.mirrorboard.utilities.Constants;
import com.teamcreators.mirrorboard.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 */
public class LoginActivity extends AppCompatActivity {

    private EditText inputPhoneNum, inputPassword;
    private Button buttonLogIn;
    private ProgressBar loginProgressBar;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // stay in login
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            stayInUserMode();
        }

        inputPhoneNum = findViewById(R.id.login_phoneNum);
        inputPassword = findViewById(R.id.login_password);
        buttonLogIn = findViewById(R.id.login_logIn_button);
        loginProgressBar = findViewById(R.id.login_progressbar);

        // log in button
        buttonLogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputPhoneNum.getText().toString().trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.PHONE.matcher(inputPhoneNum.getText().toString()).matches()) {
                    Toast.makeText(LoginActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                } else if (inputPassword.getText().toString().trim().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                } else {
                    login();
                }
            }
        });

        // new account button (create an Account)
        findViewById(R.id.login_newAccount_button).setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });

        // exit button
        findViewById(R.id.login_exit_button).setOnClickListener(view -> {
            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }

    /**
     * Verify whether the mobile phone number and password entered
     * by the user are consistent with those stored in the database.
     * If they are consistent, log the user into the application
     * and update the user's local information from database
     */
    private void login() {
        buttonLogIn.setVisibility(View.INVISIBLE);
        loginProgressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, inputPhoneNum.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            final String mode = documentSnapshot.getString(Constants.KEY_MODE);
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_MODE, mode);
                            preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                            preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_PHONE, documentSnapshot.getString(Constants.KEY_PHONE));
                            preferenceManager.putString(Constants.KEY_AVATAR_URI, documentSnapshot.getString(Constants.KEY_AVATAR_URI));
                            List<String> hobbies = (List<String>) documentSnapshot.get(Constants.KEY_HOBBIES);
                            preferenceManager.putStringSet(Constants.KEY_HOBBIES, new HashSet<>(hobbies));
                            startUserMode(mode);
                            finish();
                        } else {
                            buttonLogIn.setVisibility(View.VISIBLE);
                            loginProgressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(LoginActivity.this, "Incorrect phone number or password", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Keep staying in the application mode selected by user
     */
    private void stayInUserMode() {
        if (preferenceManager.getString(Constants.KEY_MODE).equals(Constants.MODE_ELDERLY)) {
            Intent intent = new Intent(getApplicationContext(), MainActivityElderly.class);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(getApplicationContext(), MainActivityFamily.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Enter the application mode selected by user
     */
    private void startUserMode(String mode) {
        Intent intent;
        if (mode != null && mode.equals(Constants.MODE_ELDERLY)) {
            intent = new Intent(getApplicationContext(), MainActivityElderly.class);
        } else {
            intent = new Intent(getApplicationContext(), MainActivityFamily.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}