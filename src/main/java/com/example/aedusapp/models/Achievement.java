package com.example.aedusapp.models;

public class Achievement {
    private String id;
    private String title;
    private String description;
    private int reward;
    private String iconPath;

    public Achievement(String id, String title, String description, int reward, String iconPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.iconPath = iconPath;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getReward() { return reward; }
    public String getIconPath() { return iconPath; }
}
