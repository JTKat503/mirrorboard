package com.teamcreators.mirrorboard.activitiesforelderly;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.teamcreators.mirrorboard.R;

public class InfoRequestActivity extends AppCompatActivity {

    private Button addContact, removeRequest, nextRequest, goBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_request_elderly);

        addContact = findViewById(R.id.requestInfo_addContact_button);
        removeRequest = findViewById(R.id.requestInfo_removeRequest_button);

        // adding contact button
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
                Toast.makeText(getApplicationContext(), "Contact added", Toast.LENGTH_SHORT).show();
                onBackPressed();
                finish();
            }
        });

        // removing a Request button
        removeRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(InfoRequestActivity.this);
                builder.setMessage("Are you sure you want to delete this request?")
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // delete request, go back to main page
                                // to do - delete request operation
                                onBackPressed();
                                finish();
                            }
                        }).show();
            }
        });

        // next Request button
        nextRequest = findViewById(R.id.requestInfo_nextRequest_button);
        nextRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // to do
                // if this is the last request, show hint
            }
        });

        // goBack button
        goBack = findViewById(R.id.requestInfo_goBack_button);
        goBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });



    }
}