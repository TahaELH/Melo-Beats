package com.example.melobeats;

import java.io.Serializable;

public class ArtistModal implements Serializable {
    private String Idartist;
    private String Nameartist;


    public String getIdartist() {
        return Idartist;
    }

    public void setIdartist(String idartist) {
        Idartist = idartist;
    }

    public String getNameartist() {
        return Nameartist;
    }

    public void setNameartist(String nameartist) {
        Nameartist = nameartist;
    }

    public ArtistModal(String Idartist, String Nameartist) {
        this.Idartist = Idartist;
        this.Nameartist = Nameartist;
    }
}
