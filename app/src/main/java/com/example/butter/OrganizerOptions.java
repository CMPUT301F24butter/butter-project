package com.example.butter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class OrganizerOptions extends DialogFragment {

    private String eventID;
    private String deviceID;

    public OrganizerOptions(String eventID, String deviceID) {
        this.eventID = eventID;
        this.deviceID = deviceID;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getContext()).inflate(R.layout.organizer_options_dialog, null);

        // getting text boxes
        TextView editEvent = view.findViewById(R.id.edit_event_text);
        TextView viewEntrants = view.findViewById(R.id.view_entrants_text);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        // setting click listener for 'edit event' text
        editEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), EditEventActivity.class);
                intent.putExtra("deviceID", deviceID);
                intent.putExtra("eventID", eventID);
                startActivity(intent);
            }
        });

        // setting click listener for 'view entrants' text
        viewEntrants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), EntrantListsActivity.class);
                intent.putExtra("deviceID", deviceID);
                intent.putExtra("eventID", eventID);
                startActivity(intent);
            }
        });

        return builder
                .setView(view)
                .setNeutralButton("Ok", null)
                .create();
    }
}