package marcin.wisniewski.com.pointsofinterest;

import java.io.Serializable;

public class POI implements Serializable{

    private String name, type, description;
    private Double lat, lon;

    public POI(String name, String type, String description, Double lat, Double lon){
        this.name = name;
        this.type = type;
        this.description = description;
        this.lat = lat;
        this.lon = lon;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLon() {
        return lon;
    }

    @Override
    public String toString(){
        return this.name;
    }
}