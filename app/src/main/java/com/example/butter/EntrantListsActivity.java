package com.example.butter;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;

public class EntrantListsActivity extends AppCompatActivity {

    String listSelected;
    ListView entrantList;
    ArrayList<User> entrantsData;
    EntrantsArrayAdapter adapter;

    String eventID;

    private FirebaseFirestore db;
    private CollectionReference userRef;
    private CollectionReference userListRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.entrants_list);

        String deviceID = getIntent().getExtras().getString("deviceID"); // logged in deviceID
        eventID = getIntent().getExtras().getString("eventID"); // eventID

        db = FirebaseFirestore.getInstance();
        userRef = db.collection("user"); // user collection
        userListRef = db.collection("userList"); // userList collection

        entrantsData = new ArrayList<>();
        entrantList = findViewById(R.id.entrants_list);
        adapter = new EntrantsArrayAdapter(getApplicationContext(), entrantsData);
        entrantList.setAdapter(adapter);

        Spinner spinner = findViewById(R.id.entrants_spinner);

        // setting spinner options
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("Waitlist");
        arrayList.add("Draw");
        arrayList.add("Registered");
        arrayList.add("Cancelled");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arrayList);
        adapter.setDropDownViewResource(android.R.layout.select_dialog_singlechoice);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                String list = adapterView.getItemAtPosition(position).toString(); // selected list
                listSelected = list;
                displayEntrants(); // displaying entrants in this list
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void displayEntrants() {
        String userListID; // generating the userListID for the given list
        if (Objects.equals(listSelected, "Waitlist")) {
            userListID = eventID + "-wait";
        } else if (Objects.equals(listSelected, "Draw")) {
            userListID = eventID + "-draw";
        } else if (Objects.equals(listSelected, "Registered")) {
            userListID = eventID + "-registered";
        } else {
            userListID = eventID + "-cancelled";
        }

        entrantsData.clear(); // clearing current data in the entrants list
        adapter.notifyDataSetChanged();
        // getting data from this userList document
        userListRef.document(userListID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        String listSizeString = doc.getString("size");
                        int listSize = Integer.parseInt(listSizeString); // # of entrants in the list

                        for (int i = 0; i < listSize; i++) { // iterating over all entrants in the list
                            String deviceID = doc.getString("user" + i);

                            // getting the user's info
                            userRef.document(deviceID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> userTask) {
                                    if (userTask.isSuccessful()) {
                                        DocumentSnapshot userDoc = userTask.getResult();
                                        if (userDoc.exists()) {
                                            // extracting user details
                                            String deviceID = userDoc.getId();
                                            String name = userDoc.getString("userInfo.name");
                                            String privilegesString = userDoc.getString("userInfo.privilegesString");
                                            int privileges = Integer.parseInt(privilegesString);
                                            String facility = userDoc.getString("userInfo.facility");
                                            String email = userDoc.getString("userInfo.email");
                                            String phone = userDoc.getString("userInfo.phoneNumber");

                                            // creating User object
                                            User user = new User(deviceID, name, privileges, facility, email, phone);

                                            entrantsData.add(user); // adding the user to the list
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            });
                        }
                    }
                }
            }
        });
    }
}