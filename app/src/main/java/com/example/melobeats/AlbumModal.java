package com.example.melobeats;

import java.io.Serializable;

public class AlbumModal implements Serializable {
    private String Idalbum;
    private String Namealbum;


    public String getIdalbum() {
        return Idalbum;
    }

    public void setIdalbum(String idalbum) {
        Idalbum = idalbum;
    }

    public String getNamealbum() {
        return Namealbum;
    }

    public void setNamealbum(String namealbum) {
        Namealbum = namealbum;
    }

    public AlbumModal(String Idalbum, String Namealbum) {
        this.Idalbum = Idalbum;
        this.Namealbum = Namealbum;
    }
}