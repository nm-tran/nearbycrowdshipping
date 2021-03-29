package de.hshannover.nearbycrowdshipping.model;

public class Courier {

    private Endpoint endpoint;
    private int bid;

    public Courier(Endpoint endpoint) {
        this.endpoint = endpoint;
        this.bid = -1;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }
}
