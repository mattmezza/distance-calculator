package me.matteomerola.distancecalculator.exceptions;

/**
 * Created by matt on 2/19/17.
 */

public class GeocodingFailureException extends Exception {
    public GeocodingFailureException(String addr) {
        super("Impossible to go from address " + addr + " to lat lng");
    }
}
