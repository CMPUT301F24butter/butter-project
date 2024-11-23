package com.example.butter;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class NotificationDB {
    private FirebaseFirestore db;
    private CollectionReference notificationRef;

    public NotificationDB() {
        db = FirebaseFirestore.getInstance();
        notificationRef = db.collection("notification");
    }

    public void add(Notification notification) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        String formattedDate = formatter.format(date);

        notification.setDatetime(formattedDate);

        HashMap<String, Notification> data = new HashMap<>();
        data.put("notificationInfo", notification);

        notificationRef.document(notification.getNotificationID()).set(data);

        notificationRef
                .document(notification.getNotificationID())
                .set(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firebase", "Notification added successfully");
                    }
                });
    }

    public void delete(String notificationID) {

        notificationRef.document(notificationID).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {

                        notificationRef.document(notificationID).delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d("Firebase", "Notification deleted successfully");
                                    }
                                });
                    } else {
                        Log.d("Firebase", "Notification with this ID does not exist");
                    }
                }
            }
        });
    }
}