package com.sarvanam.opencvdemo.data.modal;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Define the Room database entity for storing images
@Entity(tableName = "face_images")
public class FaceImage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    @ColumnInfo(name = "image_data")
    public byte[] imageData;
}
