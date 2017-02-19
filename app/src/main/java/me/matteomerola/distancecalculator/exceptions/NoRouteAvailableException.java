package me.matteomerola.distancecalculator.exceptions;

/**
 * Created by matt on 2/19/17.
 */

public class NoRouteAvailableException extends Exception {

    private String from;
    private String to;
    private String mean;

    public NoRouteAvailableException(String from, String to, String mean) {
        super(String.format("No route available from %s to %s by %s",
                from,
                to,
                mean));
        this.from = from;
        this.to = to;
        this.mean = mean;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getMean() {
        return mean;
    }
}
