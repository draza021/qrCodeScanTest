package com.example.drago.barcodescanner.events;

/**
 * Created by Drago on 2/22/2017.
 */

public class PhotoTakenEvent {
    public String imageName;
    public String currentPhotoPath;
    public String amazonUrl;
    public boolean sendingToAmazon;

    public PhotoTakenEvent() {}

    public PhotoTakenEvent(String imageName, String path) {
        this.imageName = imageName;
        this.currentPhotoPath = path;
    }

    public PhotoTakenEvent(String imageName, String path, String amazon, boolean sendingToAmazon) {
        this.imageName = imageName;
        this.currentPhotoPath = path;
        this.amazonUrl = amazon;
        this.sendingToAmazon = sendingToAmazon;
    }
}
