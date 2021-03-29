package de.hshannover.nearbycrowdshipping;

import android.os.Build;

import com.google.android.gms.nearby.connection.Payload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

import de.hshannover.nearbycrowdshipping.model.ChildEndpointSet;
import de.hshannover.nearbycrowdshipping.model.Courier;
import de.hshannover.nearbycrowdshipping.model.Endpoint;
import de.hshannover.nearbycrowdshipping.model.GenerationTable;
import de.hshannover.nearbycrowdshipping.model.Header;
import de.hshannover.nearbycrowdshipping.model.Message;

public class Utils {

    public static String getMessageHeaderType(Payload payload) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String messageJson = new String(payload.asBytes());
        Message message = gson.fromJson(messageJson, Message.class);
        Header header = message.getHeader();
        return header.getType();
    }

    public static Courier getCourierInListByEndpoint(ArrayList<Courier> list, Endpoint endpoint) {
        for (Courier c : list) {
            if (c.getEndpoint().equals(endpoint)) {
                return c;
            }
        }
        return null;
    }

    public static Courier getCourierInListByName(ArrayList<Courier> list, String name) {
        for (Courier c : list) {
            if (c.getEndpoint().getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static Courier getCourierInListById(ArrayList<Courier> list, String id) {
        for (Courier c : list) {
            if (c.getEndpoint().getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public static ChildEndpointSet getCurrentChildEndpointSet(GenerationTable generationTable) {
        ChildEndpointSet childEndpointSet = new ChildEndpointSet();
        for (HashMap.Entry<Endpoint, ChildEndpointSet> entry : generationTable.getTable().entrySet()) {
            childEndpointSet.getSet().add(entry.getKey());
            for (Endpoint e : entry.getValue().getSet()) {
                childEndpointSet.getSet().add(e);
            }
        }
        return childEndpointSet;
    }

    public static Endpoint getEndpointFromTableById(GenerationTable generationTable, String id) {
        for(Endpoint key : generationTable.getTable().keySet()){
            if(key.getId().equals(id)) {
                return key;
            }
        }
        return null;
    }

    /**
     * get a fixed name for Endpoint object which represent this device
     * @return name of this device
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
}
