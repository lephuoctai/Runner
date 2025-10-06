package com.taile.runner.models;

import java.io.Serializable;

public class RunRecord implements Serializable {
    private long id;
    private long startTime;
    private long endTime;
    private float distance; // in km
    private int steps;
    private float avgSpeed; // in m/s

    public RunRecord() {
    }

    public RunRecord(long id, long startTime, long endTime, float distance, int steps, float avgSpeed) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
        this.steps = steps;
        this.avgSpeed = avgSpeed;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    // Calculate duration in milliseconds
    public long getDuration() {
        return endTime - startTime;
    }
}
