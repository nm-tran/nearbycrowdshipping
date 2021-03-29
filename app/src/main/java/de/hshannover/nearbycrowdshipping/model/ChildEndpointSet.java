package de.hshannover.nearbycrowdshipping.model;

import java.util.HashSet;

public class ChildEndpointSet {

    private HashSet<Endpoint> set;

    public ChildEndpointSet() {
        this.set = new HashSet<>();
    }

    public HashSet<Endpoint> getSet() {
        return set;
    }

    @Override
    public String toString() {
        return "ChildEndpointSet{" +
                "set=" + set +
                '}';
    }
}
