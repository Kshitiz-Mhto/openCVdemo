package com.sarvanam.opencvdemo.data;

// Import necessary libraries
import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.sarvanam.opencvdemo.data.dao.FaceImageDao;
import com.sarvanam.opencvdemo.data.modal.FaceImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {FaceImage.class}, version = 1, exportSchema = false)
public abstract class AttendenceDatabase extends RoomDatabase {
    public abstract FaceImageDao faceImageDao();

    private static volatile AttendenceDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AttendenceDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            synchronized (AttendenceDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                                    AttendenceDatabase.class, "face_image_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

