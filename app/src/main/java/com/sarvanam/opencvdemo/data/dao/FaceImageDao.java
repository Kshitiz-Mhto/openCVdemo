package com.sarvanam.opencvdemo.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.sarvanam.opencvdemo.data.modal.FaceImage;

import java.util.List;

// Define the Room DAO (Data Access Object)
@Dao
public interface FaceImageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(FaceImage faceImage);

    @Transaction
    @Query("select * from face_images")
    List<FaceImage> getAllFaceImages();
}
