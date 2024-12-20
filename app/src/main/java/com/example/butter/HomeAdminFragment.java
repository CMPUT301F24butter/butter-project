package com.example.butter;

import static android.view.View.VISIBLE;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This fragment is used to set up the home screen on the admins side. Admins have access to a spinner
 * and can also access entrants page.
 *
 * @author Angela Dakay (angelcache)
 */

public class HomeAdminFragment extends Fragment implements ConfirmationDialog.ConfirmationDialogListener {
    // Need access to all events, users, posters + device ID
    private FirebaseFirestore db;
    private CollectionReference eventRef;
    private CollectionReference userRef;
    private CollectionReference userListRef;
    private CollectionReference QRCodeRef;
    private CollectionReference imagesRef;

    // Lists of events, users, and posters
    private ArrayList<Event> allEvents;
    private ArrayList<User> allUsers;
    private ArrayList<User> allFacilities;
    private ArrayList<String> allQrCodes;
    private ArrayList<String> allQrCodesEventID; // References the event ID image is attached to
    private ArrayList<String> allImages;
    private ArrayList<String> allImagesID; // References the event ID image is attached to
    private ListView adminListView;
    private EventArrayAdapter eventArrayAdapter;
    private UserArrayAdapter profileArrayAdapter;
    private UserArrayAdapter facilitiesArrayAdapter;
    private ImagesArrayAdapter QRCodeArrayAdapter;
    private ImagesArrayAdapter imageArrayAdapter;
    private Boolean isFacility;
    private String browse;
    private String deviceID;
    private FloatingActionButton deleteButton;
    User selectedOrganizer;
    String selectedQRCode;
    String selectedImage;
    User selectedUser;

    /**
     * Constructor for HomeAdminFragment, initializes array lists and reference to database
     * @param browse from HomeFragment, tells us what the spinner is currently on
     * @param deviceID the ID of the user
     */
    public HomeAdminFragment(String browse, String deviceID) {
        allEvents = new ArrayList<>();
        allUsers = new ArrayList<>();
        allFacilities = new ArrayList<>();
        allQrCodes = new ArrayList<>();
        allQrCodesEventID = new ArrayList<>();
        allImages = new ArrayList<>();
        allImagesID = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        eventRef = db.collection("event"); // event collection
        userRef = db.collection("user"); // user collection
        userListRef = db.collection("userList");
        QRCodeRef = db.collection("QRCode");
        imagesRef = db.collection("image");
        this.browse = browse;
        this.deviceID = deviceID;
    }

    /**
     * In the OnCreateView, the adapters for events, profile, facilities, and images are initialized
     * and set up. Admin List View is set up with the initial adapter, among the four depending on
     * the browse variable (this is determined by the spinner).
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     */
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the fragment's layout
        View view = inflater.inflate(R.layout.fragment_home_admin, container, false);
        adminListView = (ListView) view.findViewById(R.id.admin_list_view);
        deleteButton = view.findViewById(R.id.delete_admin_button);

        eventArrayAdapter = new EventArrayAdapter(getContext(), allEvents);
        profileArrayAdapter = new UserArrayAdapter(getContext(), allUsers, Boolean.FALSE);
        facilitiesArrayAdapter = new UserArrayAdapter(getContext(), allFacilities, Boolean.TRUE);
        QRCodeArrayAdapter = new ImagesArrayAdapter(getContext(), allQrCodes, allQrCodesEventID);
        imageArrayAdapter = new ImagesArrayAdapter(getContext(), allImages, allImagesID);

        // Used to set the right adapter when the user changes the spinner option
        switch (browse) {
            case "Browse Events":
                adminListView.setAdapter(eventArrayAdapter);
                break;
            case "Browse Facilities":
                adminListView.setAdapter(facilitiesArrayAdapter);
                deleteButton.setVisibility(VISIBLE);
                break;
            case "Browse Profiles":
                adminListView.setAdapter(profileArrayAdapter);
                deleteButton.setVisibility(VISIBLE);
                break;
            case "Browse Images":
                adminListView.setAdapter(imageArrayAdapter);
                deleteButton.setVisibility(VISIBLE);
                break;
            case "Browse QR Codes":
                adminListView.setAdapter(QRCodeArrayAdapter);
                deleteButton.setVisibility(VISIBLE);
                break;
        }

        // Used to delete Organizer, QR Code or Posters
        adminListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                adminListView.setItemChecked(position, true);

                if (browse.equals("Browse Events")) {
                    String selectedEventID = allEvents.get(position).getEventID();
                    Intent intent = new Intent(getContext(), EventDetailsActivity.class);
                    intent.putExtra("deviceID", deviceID);
                    intent.putExtra("eventID", selectedEventID);
                    intent.putExtra("adminPrivilege", Boolean.TRUE); // User has admin privileges, used in eventDetailsActivity for special privileges
                    intent.putExtra("adminBrowsing", Boolean.TRUE); // User is browsing, therefore won't see the waiting list button
                    startActivity(intent);
                } else if (browse.equals("Browse Profiles")) {
                    selectedUser = allUsers.get(position);
                } else if (browse.equals("Browse Facilities")) {
                    selectedOrganizer = allFacilities.get(position);
                } else if (browse.equals("Browse QR Codes")) {
                    selectedQRCode = allQrCodesEventID.get(position);
                } else if (browse.equals("Browse Images")) {
                    selectedImage = allImagesID.get(position);
                }
            }
        });

        // Sets up the delete facility button
        deleteButton();

        return view;
    }

    /**
     * Deletes Selected Image
     */
    private void deleteSelectedImage() {
        if (selectedImage != null) {
            int imageIndex = allImagesID.indexOf(selectedImage);

            ImageDB imageDB = new ImageDB();
            imageDB.delete(selectedImage);

            // Remove the ImageDB from the list and notifies adapter
            allImages.remove(imageIndex);
            allImagesID.remove(imageIndex);
            imageArrayAdapter.notifyDataSetChanged();
            selectedImage = null;

            try {   // sleep to avoid crashes
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Toast.makeText(getContext(), "Image successfully deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No Image selected.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes Selected QR Code
     */
    private void deleteSelectedQRCode() {
        if (selectedQRCode != null) {

            int QRIndex = allQrCodesEventID.indexOf(selectedQRCode);

            QRCodeDB QRCode = new QRCodeDB();
            QRCode.delete(selectedQRCode);

            // Remove the QR Code from the list and notifies adapter
            allQrCodes.remove(QRIndex);
            allQrCodesEventID.remove(QRIndex);
            QRCodeArrayAdapter.notifyDataSetChanged();
            selectedQRCode = null;

            try {   // sleep to avoid crashes
                Thread.sleep(300);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Toast.makeText(getContext(), "QR code successfully deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No QR code selected.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes Selected User Profile
     * @author Soopyman
     */
    private void deleteSelectedUser() {
        if (selectedUser != null) { // if we have a user
            // first grab user id
            String userID = selectedUser.getDeviceID();

            // remove user from list
            allUsers.remove(selectedUser);
            profileArrayAdapter.notifyDataSetChanged();

            // setup db objects
            UserDB userDB = new UserDB();
            UserListDB userListDB = new UserListDB();
            ImageDB imageDB = new ImageDB();
            NotificationDB notificationDB = new NotificationDB();

            // then lets remove all data associated with the user

            // remove the user from all event lists
            userListRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {  // if we have a doc
                        // lets loop over all the docs and call to delete in userListDB
                        for (DocumentSnapshot doc : task.getResult()) {
                            String listID = doc.getId();

                            // delete user from list if exists
                            userListDB.removeFromList(listID, userID);
                        }
                    } else {
                        Log.d("Firebase", "Error getting documents: ", task.getException());
                    }
                }
            });

            // remove user pfp from imageDB
            imageDB.delete(userID);
            // delete notifications sent to user
            notificationDB.deleteNotificationsToUser(userID);

            // finally, lets check if they are an organizer. if so, delete all events associated with user
            if (selectedUser.getPrivileges() != 100 && selectedUser.getPrivileges() != 400 && selectedUser.getPrivileges() != 500) { // if our user is an organizer
                // if so, we are an organizer and may have existing events
                // delete these events and all items associated with them

                // first lets query for event, search for "eventInfo.organizerID"
                eventRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {  // if we found the ref
                            // lets go over all events and search for one with corresponding org id
                            for (DocumentSnapshot doc : task.getResult()) {
                                String eventID = doc.getId();   // get event id
                                String eventOID = doc.getString("eventInfo.organizerID"); // get org id
                                if (userID.equals(eventOID)) {  // if our user is the same, delete this event and all associated data
                                    // call to deleteEvent with the corresponding eventID to delete
                                    deleteEvent(eventID);
                                }
                            }
                        } else {    // else we failed to get the doc
                            Log.d("Firebase", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }

            // finally, we delete the user from the user database
            userDB.delete(userID);

            Toast.makeText(getContext(), "User Profile successfully deleted.", Toast.LENGTH_SHORT).show();
        } else { // else, we do not currently have a user selected
            Toast.makeText(getContext(), "No User Profile selected.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes Selected Facility -- takes into account the users privileges
     * If a user is an admin and organizer, they will become only an admin
     * IF a user is an organizer only or both organizer and entrant, they will become only an entrant
     */
    private void deleteSelectedFacility() {
        if (selectedOrganizer != null) { // Check if a user is selected
            UserDB userDB = new UserDB();

            // Removes the facility from the list and notifies adapter
            allFacilities.remove(selectedOrganizer);
            facilitiesArrayAdapter.notifyDataSetChanged();

            // keep deleted facility to use in a toast
            String deletedFacility = selectedOrganizer.getFacility();
            String userID = selectedOrganizer.getDeviceID();    // get deviceID to delete events

            // Nullify the user's facility
            selectedOrganizer.setFacility(null);

            int privilege = selectedOrganizer.getPrivileges();

            if (privilege == 600 || privilege == 700) { // If they are an admin + organizer, they will become admin only
                selectedOrganizer.setPrivileges(400);
            } else {
                selectedOrganizer.setPrivileges(100); // Organizer + entrant / organizer only will become entrant only
            }

            // remove and delete all events associated with the facility
            // delete all facilities associated with this user
            eventRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {  // if we found the ref
                        // lets go over all events and search for one with corresponding org id
                        for (DocumentSnapshot doc : task.getResult()) {
                            String eventID = doc.getId();   // get event id
                            String eventOID = doc.getString("eventInfo.organizerID"); // get org id
                            if (userID.equals(eventOID)) {  // if our user is the same, delete this event and all associated data
                                // call to deleteEvent with the corresponding eventID to delete
                                deleteEvent(eventID);
                            }
                        }
                    } else {    // else we failed to get the doc
                        Log.d("Firebase", "Error getting documents: ", task.getException());
                    }
                }
            });


            // Update the user in the database
            try {
                userDB.update(selectedOrganizer);
            } catch (Exception e) {
                Log.e("DatabaseError", "Failed to update user: " + selectedOrganizer.getDeviceID(), e);
                return; // Exit if the update fails
            }

            // Clear selection and refresh the list
            selectedOrganizer = null;

            Toast.makeText(getContext(), deletedFacility + " deleted.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No Facility selected.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * When a user comes back from entrants detail activity, it ensures that the events list will be
     * refreshed, showing up to date data in case the user deleted the event.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (browse.equals("Browse Events")) {
            showEventsList(); // Refresh the event list whenever the fragment is resumed
        }
    }

    /**
     * When the delete button is clicked, it sets deleteButtonClicked to be true, which will then
     * set up the user's ability to delete either a facility, an event poster, or a QR Code they
     * click on.
     */
    private void deleteButton() {
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (browse.equals("Browse Facilities")) {
                    ConfirmationDialog dialog = new ConfirmationDialog(getContext(), HomeAdminFragment.this, "Facility");
                    dialog.showDialog();
                } else if (browse.equals("Browse QR Codes")) {
                    ConfirmationDialog dialog = new ConfirmationDialog(getContext(), HomeAdminFragment.this, "QR Code");
                    dialog.showDialog();
                } else if (browse.equals("Browse Images")) {
                    ConfirmationDialog dialog = new ConfirmationDialog(getContext(), HomeAdminFragment.this, "Image");
                    dialog.showDialog();
                } else if (browse.equals("Browse Profiles")) {
                    ConfirmationDialog dialog = new ConfirmationDialog(getContext(), HomeAdminFragment.this, "User Profile");
                    dialog.showDialog();
                }
            }
        });
    }

    /**
     * Show Events List method: populates the admin list with all events.
     */
    public void showEventsList() {
        deleteButton.setVisibility(View.INVISIBLE);
        adminListView.setAdapter(eventArrayAdapter);

        eventRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allEvents.clear();
                    int numEvents = task.getResult().size();
                    final AtomicInteger completedTasks = new AtomicInteger(0); // tracks amount of images completed

                    // Loop through the documents and create Event objects
                    for (DocumentSnapshot doc : task.getResult()) {
                        String eventID = doc.getId();
                        String eventName = doc.getString("eventInfo.name");
                        String eventDate = doc.getString("eventInfo.date");
                        String eventCapacityString = doc.getString("eventInfo.capacityString");

                        Event event = null;
                        if (eventCapacityString != null) {
                            int eventCapacity = Integer.parseInt(eventCapacityString);
                            event = new Event(eventID, eventName, eventDate, eventCapacity);
                        } else {
                            event = new Event(eventID, eventName, eventDate, -1);
                        }

                        Event finalEvent = event;
                        // Fetch poster image for event
                        imagesRef.document(event.getEventID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> imageTask) {
                                if (imageTask.isSuccessful()) {
                                    DocumentSnapshot imageDoc = imageTask.getResult();
                                    if (imageDoc.exists()) {
                                        String base64string = imageDoc.getString("imageData");
                                        finalEvent.setImageString(base64string); // setting the poster
                                    }
                                }
                                // increment counter after each image task completed
                                int completedCount = completedTasks.incrementAndGet();

                                // If all tasks completed, update list
                                if (completedCount == numEvents) {
                                    eventArrayAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                        allEvents.add(finalEvent);
                    }
                }
            }
        });
    }

    /**
     * Show Profiles List method: populates the admin list with all user profiles
     */
    public void showProfilesList() {
        deleteButton.setVisibility(VISIBLE);
        adminListView.setAdapter(profileArrayAdapter);

        userRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allUsers.clear();
                    int totalUsers = task.getResult().size();
                    final AtomicInteger completedTasks = new AtomicInteger(0); // To track how many users have been fully loaded

                    for (DocumentSnapshot doc : task.getResult()) {
                        String deviceID = doc.getString("userInfo.deviceID");
                        String email = doc.getString("userInfo.email");
                        String facility = doc.getString("userInfo.facility");
                        String name = doc.getString("userInfo.name");
                        String phone = doc.getString("userInfo.phoneNumber");
                        int privileges = Integer.parseInt(doc.getString("userInfo.privilegesString"));

                        User user = new User(deviceID, name, privileges, facility, email, phone);

                        // Retrieve image data for this user
                        imagesRef.document(user.getDeviceID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> imageTask) {
                                if (imageTask.isSuccessful()) {
                                    DocumentSnapshot imageDoc = imageTask.getResult();
                                    if (imageDoc.exists()) { // If image data exists for this user
                                        String base64string = imageDoc.getString("imageData");
                                        user.setProfilePicString(base64string);
                                    }
                                }
                                // Add the user to the list after image data is fetched
                                allUsers.add(user);

                                // Increment the counter for each loaded user
                                if (completedTasks.incrementAndGet() == totalUsers) {
                                    // Once all users are loaded, update the list in the adapter
                                    profileArrayAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } else {
                    Log.d("Firebase", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    /**
     * Show Facilities List method: populates the admin list with all user facilities
     */
    public void showFacilitiesList() {
        deleteButton.setVisibility(VISIBLE);
        adminListView.setAdapter(facilitiesArrayAdapter);
        userRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allFacilities.clear();
                    int totalFacilities = task.getResult().size();
                    final AtomicInteger completedTasks = new AtomicInteger(0); // To track how many facilities loaded

                    for (DocumentSnapshot doc : task.getResult()) {
                        String deviceID = doc.getString("userInfo.deviceID");
                        String email = doc.getString("userInfo.email");
                        String facility = doc.getString("userInfo.facility");
                        String name = doc.getString("userInfo.name");
                        String phone = doc.getString("userInfo.phoneNumber");
                        int privileges = Integer.parseInt(doc.getString("userInfo.privilegesString"));

                        User user = new User(deviceID, name, privileges, facility, email, phone);

                        // Retrieve image data for this user
                        imagesRef.document(user.getDeviceID()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> imageTask) {
                                if (imageTask.isSuccessful()) {
                                    DocumentSnapshot imageDoc = imageTask.getResult();
                                    if (imageDoc.exists()) { // If image data exists for this user
                                        String base64string = imageDoc.getString("imageData");
                                        user.setProfilePicString(base64string);
                                    }
                                }

                                if (facility != null) {
                                    // Add the user to the list after image data is fetched
                                    allFacilities.add(user);
                                }
                                // Increment the counter for each loaded user
                                if (completedTasks.incrementAndGet() == totalFacilities) {
                                    // Once all users are loaded, update the list in the adapter
                                    facilitiesArrayAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                } else {
                    Log.d("Firebase", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    /**
     * Show Posters List method: populates the admin list with event posters
     */
    private void showImagesList() {
        deleteButton.setVisibility(VISIBLE);
        adminListView.setAdapter(imageArrayAdapter);
        imagesRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allImages.clear();
                    for (DocumentSnapshot doc : task.getResult()) {
                        String posterString = doc.getString("imageData");
                        String posterEvent = doc.getId();
                        if (posterString != null) {
                            allImages.add(posterString);
                            allImagesID.add(posterEvent);
                        }
                        imageArrayAdapter.notifyDataSetChanged();
                    }
                } else {
                    Log.d("Firebase", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    /**
     * Show QR Codes List method: populates the admin list with all event QR Codes
     */
    public void showQRCodesList() {
        deleteButton.setVisibility(VISIBLE);
        adminListView.setAdapter(QRCodeArrayAdapter);

        // fetching the QR Code associated to this eventID from firebase
        QRCodeRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    allQrCodes.clear();
                    for (DocumentSnapshot doc : task.getResult()) {
                        String QRCodeString = doc.getString("QRCodeString");
                        String QRCodeEvent = doc.getId(); // Gets the name of doc which is the event id
                        if (QRCodeString != null) {
                            allQrCodes.add(QRCodeString);
                            allQrCodesEventID.add(QRCodeEvent);
                        }
                    }
                    QRCodeArrayAdapter.notifyDataSetChanged();
                } else {
                    Log.d("Firebase", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    /**
     * Used by HomeFragment to notify HomeAdminFragment about changes in the spinner the browse
     * variable keeps track of changes.
     * @param browse taken from spinner in Home Adapter, depending on changes in browse, admins list
     *               view adapter and items will change.
     */
    public void spinnerBrowseChange(String browse) {
        this.browse = browse;
        switch (browse) {
            case "Browse Events":
                showEventsList();
                break;
            case "Browse Facilities":
                showFacilitiesList();
                break;
            case "Browse Profiles":
                showProfilesList();
                break;
            case "Browse Images":
                showImagesList();
                break;
            case "Browse QR Codes":
                showQRCodesList();;
                break;
        }
    }

    /**
     * Confirming if the user wants to continue with the deletion, communication between
     * HomeAdminFragment and ConfirmationDialog
     * @param confirmDelete boolean confirms if user wants to delete
     * @param deletedItem has value "Event"
     */
    @Override
    public void deleteConfirmation(boolean confirmDelete, String deletedItem) {
        if (confirmDelete) {
            switch (deletedItem){
                case "Facility":
                    deleteSelectedFacility();
                    break;
                case "QR Code":
                    deleteSelectedQRCode();
                    break;
                case "Image":
                    deleteSelectedImage();
                    break;
                case "User Profile":
                    deleteSelectedUser();
                    break;
            }
        } else {
            Toast.makeText(getContext(), deletedItem + " deletion cancelled.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Takes in an eventID, and deletes all associated items in the database with said event.
     * @param eventID contains the ID for the corresponding event to be deleted
     */
    private void deleteEvent(String eventID) {
        // init db objects
        QRCodeDB qrCodeDB = new QRCodeDB();
        ImageDB imageDB = new ImageDB();
        MapDB mapDB = new MapDB();
        NotificationDB notificationDB = new NotificationDB();
        EventDB eventDB = new EventDB();
        UserListDB userListDB = new UserListDB();

        // init event list IDs
        String waitListID = eventID + "-wait";
        String drawListID = eventID + "-draw";
        String registerListID = eventID + "-registered";
        String cancelledListID = eventID + "-cancelled";

        // deleting all user lists associated with this event
        userListDB.deleteList(waitListID);
        userListDB.deleteList(drawListID);
        userListDB.deleteList(registerListID);
        userListDB.deleteList(cancelledListID);

        // deleting db items associated with event
        qrCodeDB.delete(eventID);
        imageDB.delete(eventID);
        mapDB.deleteMap(eventID);
        notificationDB.deleteNotificationsFromEvent(eventID);

        // deleting the event itself
        eventDB.delete(eventID);

        try {   // sleep to avoid crashes
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}