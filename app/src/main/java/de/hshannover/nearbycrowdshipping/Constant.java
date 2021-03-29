package de.hshannover.nearbycrowdshipping;

import android.Manifest;

import com.google.android.gms.nearby.connection.Strategy;

public class Constant {
    /**
     * This service id is to find other nearby devices that are interested in the same thing.
     */
    public static final String SERVICE_ID =  "de.hshannover.nearbycrowdshipping";

    public static final String TAG = "NearbyCrowdshipping";

    public static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    /**
     * All constants related to the permissions
     */
    public static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
    public static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    /**
     * Type for Payload Message
     */
    public static final String MESSAGE_HEADER_PARCEL = "parcel";
    public static final String MESSAGE_HEADER_BID = "bid";
    public static final String MESSAGE_HEADER_CHILDENDPOINT_SET = "childEndpoint set";
    public static final String MESSAGE_HEADER_WINNER = "winner";

}
