package com.example.butter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Text;

import java.util.ArrayList;

/**
 * This is an array adapter for QR codes and Poster objects
 * This is used to display the posters and QR objects in "Browse QR Codes" and "Browse Event Posters".
 *
 * @author Angela Dakay (angelcache)
 */
public class ImagesArrayAdapter extends ArrayAdapter<String> {
    private ArrayList<String> images;
    private final Context context;
    private ArrayList<String> events;

    /**
     * Constructor for ImagesArrayAdapter, instantiates images, context, and events variables.
     * @param context activity or fragment adapter is being used in
     * @param images list of image strings to be converted into bitmap
     * @param events list of event names associated with the images
     */
    public ImagesArrayAdapter(Context context, ArrayList<String> images, ArrayList<String> events) {
        super(context, 0, images);
        this.images = images;
        this.context = context;
        this.events = events;
    }

    /**
     * Finds the ImageView and TextView and gives them an image (Poster or QRCode) and event name
     * @param position The position of the item within the adapter's data set of the item whose view
     *        we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *        is non-null and of an appropriate type before using. If it is not possible to convert
     *        this view to display the correct data, this method can create a new view.
     *        Heterogeneous lists can specify their number of view types, so that this View is
     *        always of the right type (see {@link #getViewTypeCount()} and
     *        {@link #getItemViewType(int)}).
     * @param parent The parent that this view will eventually be attached to
     * @return View
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.poster_content, parent,false);
        }

        String image = images.get(position);
        String eventName = getEventName(events.get(position));

        Bitmap bitmap = stringToBitmap(image); // turning the string into a bitmap

        ImageView bitmapImage = view.findViewById(R.id.poster_image);
        bitmapImage.setImageBitmap(bitmap); // displaying the bitmap

        TextView imageInfo = view.findViewById(R.id.poster_name);
        imageInfo.setText(String.format("From Event: %s", eventName)); // tells us which event image is from

        return view;
    }

    /**
     * Gets the event name from eventID by removing everything after the dash and replacing
     * underscores with spaces.
     * @param eventID the event ID that wil be cleaned up to get the event name
     * @return Event Name
     */
    private String getEventName(String eventID) {
        int dashIndex = eventID.indexOf("-");
        if (dashIndex != -1) {
            eventID = eventID.substring(0, dashIndex);
        }

        // Getting Rid of the _ in the eventID so we get just the eventName
        return eventID.replace("_", " ");
    }

    /**
     * Converts a string into a bitmap.
     * @param base64String the string version of the image that will be converted into bitmap
     * @return Bitmap
     */
    private Bitmap stringToBitmap(String base64String) {
        byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        return bitmap;
    }
}