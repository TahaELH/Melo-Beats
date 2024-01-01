package com.example.melobeats;

import java.io.Serializable;

public class AudioModel implements Serializable {
    private String title;
    private String artist;
    private String album;
    private String Path;
    private String duration;
    private  String ID;




    public AudioModel(String Path, String title, String artist, String album, String duration, String ID) {
        this.Path = Path;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.ID = ID;
    }



    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String filePath) {
        this.Path = filePath;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}

