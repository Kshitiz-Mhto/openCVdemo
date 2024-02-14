package com.sarvanam.opencvdemo.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;

import com.sarvanam.opencvdemo.data.modal.FaceImage;

// Define the Room DAO (Data Access Object)
@Dao
public interface FaceImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FaceImage faceImage);
}
