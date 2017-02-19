package me.matteomerola.distancecalculator;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;

/**
 * created by matt on 02/19/2017
 */
public class MainActivity extends MaterialNavigationDrawer<Fragment> {

    private MaterialSection calcDistanceSection;
    private MaterialSection savedSection;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION = 0;
    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    public void init(Bundle savedInstanceState) {
        MaterialAccount account = new MaterialAccount(
                this.getResources(),
                getResources().getString(R.string.company),
                getResources().getString(R.string.app_name),
                R.drawable.taxfix_logo,
                R.drawable.bg_drawer);
        this.addAccount(account);

        this.calcDistanceSection = newSection(getString(R.string.distance), getResources().getDrawable(R.mipmap.ic_car_black_24dp), new InputFragment());
        this.savedSection = newSection(getString(R.string.saved_title), getResources().getDrawable(R.mipmap.ic_content_save_black_24dp), new ListFragment());
        this.addSection(this.calcDistanceSection);
        this.addDivisor();
        this.addSection(this.savedSection);
        disableLearningPattern();
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION) {
            Log.i(TAG, "Received response for Camera permission request.");

            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission has been granted, preview can be displayed
                Log.i(TAG, "LOCATION permission has now been granted.");
                Toast.makeText(this, R.string.permision_available_location, Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "LOCATION permission was NOT granted.");
                Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_SHORT).show();

            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Requests the Location permission.
     * If the permission has been denied previously, a Dialog will prompt the user to grant the
     * permission, otherwise it is requested directly.
     */
    public void requestLocationPermission() {
        Log.i(TAG, "LOCATION permission has NOT been granted. Requesting permission.");

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Log.i(TAG,
                    "Displaying location permission rationale to provide additional context.");
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.permissions_location_rationale)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    PERMISSIONS_LOCATION,
                                    REQUEST_LOCATION);
                        }
                    })
                    .create();
            dialog.show();
        } else {

            // Location permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, PERMISSIONS_LOCATION,
                    REQUEST_LOCATION);
        }
    }
}
