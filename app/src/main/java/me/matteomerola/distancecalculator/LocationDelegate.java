package me.matteomerola.distancecalculator;

import android.location.Address;
import android.location.Location;

/**
 * Created by matt on 2/19/17.
 */

public interface LocationDelegate {

    void onLocationUpdate(Location location);
    void onAddressUpdate(Address address);
    void onLocationSettingsUnsuccessful();

}
