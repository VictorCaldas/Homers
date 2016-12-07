package com.homers.Model;


import com.google.gson.annotations.SerializedName;

public class Rota {

    @SerializedName("rotasID")
    private int rotasID;
    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;
    @SerializedName("time")
    private String time;
    @SerializedName("speed")
    private int speed;
    @SerializedName("rvc_name")
    private String rvc_name;

    public int getRotasID() {
        return rotasID;
    }

    public void setRotasID(int rotasID) {
        this.rotasID = rotasID;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getRvc_name() {
        return rvc_name;
    }

    public void setRvc_name(String rvc_name) {
        this.rvc_name = rvc_name;
    }






}
