package com.example.butter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple activity called from {@link ProfileFragment}
 * To edit/update an existing user in the database
 * Is called to from {@link ProfileFragment} when the user clicks the button to edit their profile.
 * @author Soopyman
 */
public class EditProfileActivity extends AppCompatActivity {

    /**
     * User Database object.
     * Used specifically for updating the user in the database after editing and verifying for validity.
     */
    private UserDB users; // interact with userDB
    /**
     * ActivityResultLauncher object
     * Used to receive image from gallery to set it up as a profile picture.
     */
    private ActivityResultLauncher<Intent> getImageLauncher; // init image launcher
    /**
     * Uri object containing an image
     * Used to store the image, and for notifying if an image is/was stored.
     */
    private Uri imageUri;
    /**
     * newPFP boolean
     * Used to determine when submitting image to db, whether a pfp existed before or not
     */
    private boolean newPFP;
    /**
     * removed boolean
     * Used to determine if a profile picture existing has been removed
     */
    private boolean removed;
    /**
     * ImageDB Image database object
     * Used for adding, updating, and deleting images in the database
     * Includes helper functions to convert image datatypes
     */
    private ImageDB imageDB;
    /**
     * Firebase database object
     * Used for querying user facilities
     */
    private FirebaseFirestore db;

    private interface OnFacilitiesLoadedCallback {
        void checkForFacility(List<String> facilities);
    }

    /**
     * onCreate contains firstly setting up the views and editable options on screen for specific user,
     * filling it in with already existing user data.
     * This also requires managing the view dependent on which role (i.e. if role is entrant, do not show facility)
     * Contains many different onClickListeners for each of the buttons/spinners:
     * saveButton: performs checks for valid data, and updates user in database if valid.
     * roleSpinner: onClick of a specific role, hide/un-hide facility attributes.
     * There will be more buttons added later.
     * @param savedInstanceState
     * The last saved state of the activity (if exists)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        users = new UserDB(); // init the userDB object
        imageDB = new ImageDB(); // init imageDB object
        db = FirebaseFirestore.getInstance();
        // get args
        User user = (User) getIntent().getSerializableExtra("user");    // set user object
        String base64Image = getIntent().getStringExtra("base64Image"); // set image str (could be null)

        setContentView(R.layout.edit_profile);    // set view to create profile screen

        // setup role spinner
        Spinner roleSpinner = findViewById(R.id.edit_role_spinner);
        // convert roles array to modifiable list
        List<String> rolesList = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.roles_array)));
        // create an array adapter for the list
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rolesList);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        CircleImageView profileImage = findViewById(R.id.profileImage); // profile image view

        // setup our various buttons
        ImageButton backButton = findViewById(R.id.back_button);
        ImageButton addImageButton = findViewById(R.id.add_image_button);
        ImageButton removeImageButton = findViewById(R.id.remove_image_button);
        Button saveButton = findViewById(R.id.save_changes_button);    // create button on profile screen

        TextView facilityLabel = findViewById(R.id.facility_label); // view to show facility, used to hide/unhide text

        TextView editInitial = findViewById(R.id.profileText);
        EditText editUsername = findViewById(R.id.username);
        EditText editEmail = findViewById(R.id.email);
        EditText editPhone = findViewById(R.id.create_number_text);
        EditText editFacility = findViewById(R.id.facility_name);

        // now we want to set all of our text and such to be what was included in user object passed
        editInitial.setText(user.getName().substring(0,1));
        editUsername.setText(user.getName());
        editEmail.setText(user.getEmail());
        editPhone.setText(user.getPhoneNumber());

        // now lets change our selection for spinner depending on privileges
        if (user.getPrivileges() < 200) { // then we are entrant
            roleSpinner.setSelection(0); // set spinner to be selected to entrant
        } else if (user.getPrivileges() < 300) { // then we are organizer
            roleSpinner.setSelection(1);
        } else if (user.getPrivileges() < 400) { // then we are both
            roleSpinner.setSelection(2);
        } else {    // else are admin, and should update spinner based on which admin role we are
            rolesList.add("Admin"); // add all possible roles for admin
            rolesList.add("Admin & Entrant");
            rolesList.add("Admin & Organizer");
            rolesList.add("Admin, Organizer, & Entrant");
            roleAdapter.notifyDataSetChanged();
            for (int i = 3; i < roleAdapter.getCount(); i++) { // grab index of our role and set to it
                if (roleAdapter.getItem(i).equals(user.getRole())) {
                    roleSpinner.setSelection(i);
                }
            }
        }

        if (user.getPrivileges() > 100 && user.getPrivileges() != 400 && user.getPrivileges() != 500) { // if we are not admin and/or entrant, show facility
            facilityLabel.setVisibility(View.VISIBLE);
            editFacility.setVisibility(View.VISIBLE);
            editFacility.setText(user.getFacility());
        }

        // setup profile picture if exists
        if (base64Image != null) {  // if we have an existing image, lets set it up
            profileImage.setImageBitmap(imageDB.stringToBitmap(base64Image));
            editInitial.setVisibility(View.INVISIBLE);    // hide initial
            newPFP = false; // if we already have a pfp (to update)
        } else {
            newPFP = true;  // else we are adding for first
        }
        imageUri = null;    // init uri to null (determines if a photo existed
        removed = false;    // init our removed bool (determines if we have deleted the current photo or not)

        // now that we are here, we should have all data put in.
        // simply set up our onClick/onSelected listeners

        // setup our addImage listener
        // if we receive a result, update our image with result
        getImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            imageUri = result.getData().getData();  // getting the Uri of the selected image
                            profileImage.setImageURI(imageUri);     // displaying the image
                            // if not hidden yet, hide our initial
                            editInitial.setVisibility(View.INVISIBLE);
                        } catch (Exception e) {
                            System.out.println("Error");
                        }
                    }
                }
        );

        // on click listener for the back button
        // finish and go back from this activity
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if clicked, finish and go back (no updates made)
                finish();
            }
        });

        // on click listener for adding a profile picture
        // add profile pic as attribute
        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if clicked, show image and add as attribute
                Intent intent = new Intent(MediaStore.ACTION_PICK_IMAGES);
                getImageLauncher.launch(intent);
            }
        });

        // @drawable/profile_circle
        // on click listener for removing the current profile picture
        removeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if clicked, set image to null and set initial to visible
                profileImage.setImageResource(R.drawable.profile_circle);
                editInitial.setVisibility(View.VISIBLE);
                imageUri = null;    // set image to null
                removed = true;     // set value to know we want to remove our current img (no going back)
            }
        });

        // on click listener for the save changes button
        // must perform validity checks first
        // if checks pass, add user to database and go to MainActivity
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if clicked, test if results are valid

                // get the placed text into strings
                String username = editUsername.getText().toString();
                String email = editEmail.getText().toString();
                String phone = editPhone.getText().toString();
                String facility = editFacility.getText().toString(); // will be empty if entrant
                String role = roleSpinner.getSelectedItem().toString();

                // pass to validity check to get t/f (and trim username & facility)
                String validRet = validityCheck(username.trim(), email, phone, facility.trim(), role);

                if (validRet.equals("true")) {
                    // if valid, add to database
                    int privileges = 0;  // init privileges number
                    // if phone is empty, set to null
                    if (phone.isEmpty()) phone = null;
                    // if facility is empty (i.e, we are entrant), set to null
                    if (facility.isEmpty()) facility = null;
                    if (facility != null) facility = facility.trim();   // else trim the facility

                    // now to set privilege values:
                    // Entrant = 100, Organizer = 200, both = 300, admin = 400, admin & entrant = 500, admin & org = 600, all = 700
                    switch (role) {
                        case "Entrant":
                            privileges = 100;
                            facility = null;
                            break;
                        case "Organizer":
                            privileges = 200;
                            break;
                        case "Both":
                            privileges = 300;
                            break;
                        case "Admin":
                            privileges = 400;
                            facility = null;
                            break;
                        case "Admin & Entrant":
                            privileges = 500;
                            facility = null;
                            break;
                        case "Admin & Organizer":
                            privileges = 600;
                            break;
                        case "Admin, Organizer, & Entrant":
                            privileges = 700;
                            break;
                    }

                    // now we just simply update the user object with the new one in our database
                    User userUpdate = new User(user.getDeviceID(), username.trim(), privileges, facility, email, phone);

                    if (facility != null) { // if we are an org, check for conflicting facility
                        // run a final check for conflicting facility
                        String finalFacility = facility;
                        // load our callback function to fetch a list of facilities
                        fetchFacilities(new OnFacilitiesLoadedCallback() {
                            @Override
                            public void checkForFacility(List<String> facilities) {
                                // check if facility is in our fetched facility list from db
                                if (!facilities.contains(finalFacility) || finalFacility.equals(user.getFacility())) { // if the facility is not in db or is the original facility
                                    // update user in db and finish
                                    users.update(userUpdate);

                                    // update our profile pic in db first
                                    // if we have a non null uri, update uri as well
                                    if (imageUri != null) { // if we have a new image
                                        if (newPFP) {   // if our new image is just new
                                            // add new image to db
                                            imageDB.add(imageUri, user.getDeviceID(), getApplicationContext());
                                        } else { // else we are updating existing
                                            imageDB.update(imageUri, user.getDeviceID(), getApplicationContext());
                                        }
                                    } else {    // else then we are simply removing the current pfp or doing nothing
                                        if (removed && !newPFP) {  // if we had a pfp & want to remove, remove it
                                            // remove current pfp
                                            imageDB.delete(user.getDeviceID());
                                        }
                                        // else we do nothing (no updated pfp, no updates in db)
                                    }

                                    // run a quick sleep to ensure that all items have been fetched/updated in db
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }

                                    // now our updated user should be in the database and pfp updated, and we can return
                                    finish();
                                } else {    // else create builder and conflicting facility error
                                    AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
                                    builder.setTitle("Invalid Signup");
                                    builder.setMessage("Conflicting Facility Name.\nPlease try again.");
                                    builder.setPositiveButton("OK", null);
                                    builder.show();
                                }
                            }
                        });
                    } else {    // else we are not an org and should be valid
                        // update user in db and finish
                        users.update(userUpdate);

                        // update our profile pic in db first
                        // if we have a non null uri, update uri as well
                        if (imageUri != null) { // if we have a new image
                            if (newPFP) {   // if our new image is just new
                                // add new image to db
                                imageDB.add(imageUri, user.getDeviceID(), getApplicationContext());
                            } else { // else we are updating existing
                                imageDB.update(imageUri, user.getDeviceID(), getApplicationContext());
                            }
                        } else {    // else then we are simply removing the current pfp or doing nothing
                            if (removed && !newPFP) {  // if we had a pfp & want to remove, remove it
                                // remove current pfp
                                imageDB.delete(user.getDeviceID());
                            }
                            // else we do nothing (no updated pfp, no updates in db)
                        }

                        // run a quick sleep to ensure that all items have been fetched/updated in db
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // now our updated user should be in the database and pfp updated, and we can return
                        finish();
                    }
                } else {    // else then show dialogue message and continue
                    AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
                    builder.setTitle("Invalid Signup");
                    builder.setMessage(validRet);
                    builder.setPositiveButton("OK", null);
                    builder.show();
                }
            }
        });

        // OnItemSelected listener for the role spinner
        // if selected to organizer or both, show the facility TextView & EditText
        // else, hide it since we are selected to entrant.
        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedRole = adapterView.getItemAtPosition(i).toString();
                if (!selectedRole.equals("Entrant") && !selectedRole.equals("Admin") && !selectedRole.equals("Admin & Entrant")) {  // if not entrant, add facility
                    // show facility options
                    facilityLabel.setVisibility(View.VISIBLE);
                    editFacility.setVisibility(View.VISIBLE);
                } else {    // else we are entrant and should remove facility if added
                    facilityLabel.setVisibility(View.INVISIBLE);
                    editFacility.setVisibility(View.INVISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // do nothing, default is set to hidden (and entrant)
            }
        });

    }

    /**
     * Takes in all data for a user object, and validates all attributes.
     * This runs many various methods for checking if valid data.
     * @param username
     * If username is empty OR greater than 30 characters, invalid
     * @param email
     * If email is empty OR is not a valid email address (formatting wise), invalid
     * @param phone
     * If phone is not empty AND is not a valid phone number, invalid. Also checks for len > 15
     * @param facility
     * If role is not 'Entrant', 'Admin', or 'Admin and Entrant' AND (facility is empty OR more than 20 characters), invalid
     * @param role
     * Role cannot be invalid. Must be passed using "user.getRole()" or a valid role string.
     * @return
     * Returns a string with either "true" if valid,
     * Or will instead return the corresponding error message to be printed later.
     */
    // check for valid info
    private String validityCheck(String username, String email, String phone, String facility, String role) {

        // returns a string
        // string is true or an error message
        String returnString = "true";

        if (username.isEmpty()) {
            returnString = "Username box is empty.";
        } else if (username.length() > 30) {    // 30 character cap for username
            returnString = "Username is too long. Max of 30 characters.";
        } else if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            returnString = "Invalid Email Address.";
        } else if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            returnString = "Invalid Phone Number.";
        } else if (phone.length() > 15) {   // max phone len 15
            returnString = "Max Phone length is 15 numbers.";
        } else if (!role.equals("Entrant") && !role.equals("Admin") && !role.equals("Admin & Entrant")) { // else if we have facility name to eval
            if (facility.isEmpty()) {
                returnString = "Invalid Facility.";
            } else if (facility.length() > 20) {    // 20 char cap for facility
                returnString = "Facility is too long. Max of 20 characters.";
            }
        }
        if (!returnString.equals("true")) { // if we have invalid, add "please try again" text
            returnString += "\nPlease try again.";
        }
        // else our info is valid
        return returnString;
    }

    /**
     * Creates and fetches a list of strings for all facilities in the database.
     * This returns to a callback rather than a simple return.
     * The callback handles what to do with this list.
     */
    private void fetchFacilities(OnFacilitiesLoadedCallback callback) {
        db.collection("user").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {  // get facility for all users
                    List<String> facilities = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        facilities.add(doc.getString("userInfo.facility"));
                    }
                    callback.checkForFacility(facilities);
                } else {
                    Log.e("Firebase Error", "Error getting documents in EditProfileActivity", task.getException());
                    callback.checkForFacility(Collections.emptyList()); // return empty list on failure
                }
            }
        });
    }

}