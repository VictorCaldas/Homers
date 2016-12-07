package com.homers.Model;

import android.os.Build;

import com.google.gson.Gson;

import java.util.UUID;

public class ChatMessage implements Comparable<ChatMessage> {

    public static final String TYPE_USER_CHAT = "typeUserChat";
    public static final String TYPE_BEACON = "typeBeacon";

    private String name;
    private String text;
    private String id;
    private String type;

    private Double lat;
    private Double lon;
    private Double vel;

    private long timestamp;

    public static ChatMessage fromJson(String jsonString){ return new Gson().fromJson(jsonString, ChatMessage.class); }

    public ChatMessage(String text, long timestamp, Double lat, Double lon, Double vel) {
        this(Build.MODEL, text, timestamp, UUID.randomUUID().toString(), TYPE_USER_CHAT, lat, lon, vel);
    }

    public ChatMessage(String name, String text, long timestamp, String id, String type, Double lat, Double lon, Double vel) {
        this.name = name;
        this.text = text;
        this.timestamp = timestamp;
        this.id = id;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.vel = vel;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChatMessage)) {
            return false;
        }
        return id.equals(((ChatMessage)o).id);
    }

    @Override
    public int compareTo(ChatMessage another) {
        if (timestamp < another.timestamp) {
            return -1;
        } else if (timestamp > another.timestamp) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getId() {
        return id;
    }
    public String getType() {
        return type;
    }
    public String getName() {
        return name;
    }
    public String getText() {
        return text;
    }
    public Double getLat() {
        return lat;
    }
    public Double getLon() {return lon; }
    public Double getVel() {
        return vel;
    }
    public long getTimestamp() {
        return timestamp;
    }
}