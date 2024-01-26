package com.example.melobeats;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;

public class SongsHelper {
    public static ArrayList<AudioModel> getSongsList(Context context) {
        ArrayList<AudioModel> songsList = new ArrayList<>();

        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media._ID,
        };

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Media.DATE_MODIFIED + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                AudioModel songData = new AudioModel(
                        cursor.getString(1),
                        cursor.getString(0),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                );

                songsList.add(songData);
            }
            cursor.close();
        }

        return songsList;
    }
    public static ArrayList<AudioModel> getSongsByArtist(Context context, String artistName) {
        ArrayList<AudioModel> songsList = new ArrayList<>();

        for (AudioModel song : getSongsList(context)) {
            if (song.getArtist().equalsIgnoreCase(artistName)) {
                songsList.add(song);
            }
        }

        return songsList;
    }

}
