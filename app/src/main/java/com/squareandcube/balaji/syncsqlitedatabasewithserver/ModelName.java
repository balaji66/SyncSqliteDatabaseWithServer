package com.squareandcube.balaji.syncsqlitedatabasewithserver;

public class ModelName {
    private String name;
    private int status;

    public ModelName(String name, int status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }
}
