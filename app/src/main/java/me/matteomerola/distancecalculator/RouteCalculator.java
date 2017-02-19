package me.matteomerola.distancecalculator;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.matteomerola.distancecalculator.exceptions.GeocodingFailureException;
import me.matteomerola.distancecalculator.exceptions.NoRouteAvailableException;


/**
 * Created by matt on 2/19/17.
 */

public class RouteCalculator {

    private Context ctx;

    public RouteCalculator(Context ctx) {
        this.ctx = ctx;
    }

    public void calculateDirection(String fromAddr, String toAddr, String transportMode, DirectionCallback callback) throws IOException, GeocodingFailureException {
        Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
        List<Address> fromAddrs = geocoder.getFromLocationName(fromAddr, 1);
        List<Address> toAddrs = geocoder.getFromLocationName(toAddr, 1);
        if (fromAddrs == null || fromAddrs.size() == 0) {
            throw new GeocodingFailureException(fromAddr);
        }
        if (toAddrs == null || toAddrs.size() == 0) {
            throw new GeocodingFailureException(toAddr);
        }
        Address fromA = fromAddrs.get(0);
        LatLng from = new LatLng(fromA.getLatitude(), fromA.getLongitude());
        Address toA = toAddrs.get(0);
        LatLng to = new LatLng(toA.getLatitude(), toA.getLongitude());
        GoogleDirection.withServerKey(Keys.DIRECTIONS)
                .from(from)
                .to(to)
                .transportMode(transportMode)
                .execute(callback);

    }

    public Observable<Direction> calculateDirectionObservable(final String fromAddr, final String toAddr, final String transportMode) {
        Observable<Direction> observable = Observable.create(new ObservableOnSubscribe<Direction>() {
            @Override
            public void subscribe(final ObservableEmitter<Direction> e) throws Exception {
                calculateDirection(fromAddr, toAddr, transportMode, new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            e.onNext(direction);
                            e.onComplete();
                        } else {
                            e.onError(new NoRouteAvailableException(fromAddr, toAddr, transportMode));
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        e.onError(t);
                    }
                });
            }
        });

        observable.subscribeOn(Schedulers.io());
        observable.observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    public Observable<TripInfo> calculateDistanceStringObservable(final String fromAddr, final String toAddr, final String transportMode) {
        Observable<TripInfo> observable = this.calculateDirectionObservable(fromAddr, toAddr, transportMode)
                .concatMap(dir -> {
                        final TripInfo tripInfo = calcTripInfo(dir);
                        return new ObservableSource<TripInfo>() {
                            @Override
                            public void subscribe(Observer<? super TripInfo> observer) {
                                observer.onNext(tripInfo);
                                observer.onComplete();
                            }
                        };
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        return observable;
    }

    private TripInfo calcTripInfo(Direction dir) {
        int minDuration = 999999999;
        String minDurationStr = "";
        int minDistance = 999999999;
        String minDistanceStr = "";
        if (dir.getRouteList().size() > 0) {
            for (Route route : dir.getRouteList()) {
                if (route.getLegList().size() > 0) {
                    for (Leg leg : route.getLegList()) {
                        Info info = leg.getDuration();
                        int seconds = Integer.parseInt(info.getValue());
                        if (seconds < minDuration) {
                            minDuration = seconds;
                            minDurationStr = info.getText();
                        }
                        Info distanceInfo = leg.getDistance();
                        int distance = Integer.parseInt(info.getValue());
                        if (distance < minDistance) {
                            minDistance = distance;
                            minDistanceStr = distanceInfo.getText();
                        }
                    }
                }
            }
        }
        TripInfo info = new TripInfo();
        info.distance = minDistanceStr;
        info.duration = minDurationStr;
        return info;
    }

    public class TripInfo {
        String duration;
        String distance;
    }
}
