package de.hshannover.nearbycrowdshipping.model;

import java.util.HashMap;

public class GenerationTable {

    private HashMap<Endpoint, ChildEndpointSet> table;

    public GenerationTable() {
        this.table = new HashMap<>();
    }

    public HashMap<Endpoint, ChildEndpointSet> getTable() {
        return table;
    }

}
