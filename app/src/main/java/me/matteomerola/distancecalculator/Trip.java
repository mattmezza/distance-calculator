package me.matteomerola.distancecalculator;

/**
 * Created by matt on 2/19/17.
 */

public class Trip {
    public Long _id;
    public String fromAddress;
    public String toAddress;
    public String duration;
    public String distance;
    public String mean;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Trip trip = (Trip) o;

        if (_id != null ? !_id.equals(trip._id) : trip._id != null) return false;
        if (fromAddress != null ? !fromAddress.equals(trip.fromAddress) : trip.fromAddress != null)
            return false;
        if (toAddress != null ? !toAddress.equals(trip.toAddress) : trip.toAddress != null)
            return false;
        if (duration != null ? !duration.equals(trip.duration) : trip.duration != null)
            return false;
        if (distance != null ? !distance.equals(trip.distance) : trip.distance != null)
            return false;
        return mean != null ? mean.equals(trip.mean) : trip.mean == null;

    }

    @Override
    public int hashCode() {
        int result = _id != null ? _id.hashCode() : 0;
        result = 31 * result + (fromAddress != null ? fromAddress.hashCode() : 0);
        result = 31 * result + (toAddress != null ? toAddress.hashCode() : 0);
        result = 31 * result + (duration != null ? duration.hashCode() : 0);
        result = 31 * result + (distance != null ? distance.hashCode() : 0);
        result = 31 * result + (mean != null ? mean.hashCode() : 0);
        return result;
    }
}
