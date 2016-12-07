package com.homers.Model;

import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;


public class Data {
    private boolean isRunning;
    private boolean isFirstTime;

    private long time;
    private long timeStopped;

    private double distanceKm;
    private double distanceM;
    private double curSpeed;
    private double maxSpeed;
    private double lat;
    private double lon;

    private onGpsServiceUpdate onGpsServiceUpdate;

    public interface onGpsServiceUpdate{
        public void update();
    }

    public void setOnGpsServiceUpdate(onGpsServiceUpdate onGpsServiceUpdate){
        this.onGpsServiceUpdate = onGpsServiceUpdate;
    }

    public void update(){
        onGpsServiceUpdate.update();
    }

    public Data() {
        isRunning = false;
        distanceKm = 0;
        distanceM = 0;
        curSpeed = 0;
        maxSpeed = 0;
        timeStopped = 0;
        lat = 0;
        lon = 0;
    }

    public Data(onGpsServiceUpdate onGpsServiceUpdate){
        this();
        setOnGpsServiceUpdate(onGpsServiceUpdate);
    }

    public void addDistance(double distance){
        distanceM = distanceM + distance;
        distanceKm = distanceM / 1000f;
    }

    public void getLat(double lati){
        lat = lati;
    }

    public void getLon(double longi){
        lon = longi;
    }

    public SpannableString getDistance(){
        SpannableString s;
        if (distanceKm < 1) {
            s = new SpannableString(String.format("%.0f", distanceM) + "m");
            s.setSpan(new RelativeSizeSpan(0.5f), s.length() - 1, s.length(), 0);
        }else{
            s = new SpannableString(String.format("%.3f", distanceKm) + "Km");
            s.setSpan(new RelativeSizeSpan(0.5f), s.length()-2, s.length(), 0);
        }
        return s;
    }

    public SpannableString getMaxSpeed() {
        SpannableString s = new SpannableString(String.format("%.0f", maxSpeed) + "km/h");
        s.setSpan(new RelativeSizeSpan(0.5f), s.length() - 4, s.length(), 0);
        return s;
    }

    public void setCurSpeed(double curSpeed) {
        this.curSpeed = curSpeed;
        if (curSpeed > maxSpeed){
            maxSpeed = curSpeed;
        }
    }

    public boolean isFirstTime() {
        return isFirstTime;
    }

    public void setFirstTime(boolean isFirstTime) {
        this.isFirstTime = isFirstTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void setTimeStopped(long timeStopped) {
        this.timeStopped += timeStopped;
    }

    public double getCurSpeed() {
        return curSpeed;
    }

    public double getMyLat() {
        return lat;
    }

    public double getMyLon() {
        return lon;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

