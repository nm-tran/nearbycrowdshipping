package de.hshannover.nearbycrowdshipping.model;

public class Header {
    private String type;
    private String from;
    private String to;

    public Header(String type) {
        this.type = type;
    }

    public Header(String type, String from, String to) {
        this.type = type;
        this.from = from;
        this.to = to;
    }

    public String getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
