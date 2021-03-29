package de.hshannover.nearbycrowdshipping.model;


public class Parcel {
    private String destination;
    private String deadline;
    private String meetingLocation;
    private String meetingTime;
    private int price;

    public Parcel(String destination, String deadline, String meetingLocation, String meetingTime, int price) {
        this.destination = destination;
        this.deadline = deadline;
        this.meetingLocation = meetingLocation;
        this.meetingTime = meetingTime;
        this.price = price;
    }

    public String getDestination() {
        return destination;
    }

    public String getDeadline() {
        return deadline;
    }

    public String getMeetingLocation() {
        return meetingLocation;
    }

    public String getMeetingTime() {
        return meetingTime;
    }

    public int getPrice() {
        return price;
    }
}