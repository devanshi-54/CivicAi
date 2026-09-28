package com.civicai.citizen.voice.api;

public class NluResponse {
    public String intent;
    public double confidence;
    public Entities entities;
    public Boolean confirmation;
    public String editField;
    public boolean cancel;
    public boolean restart;

    public static class Entities {
        public String category;
        public String title;
        public String description;
        public String location;
    }
}
