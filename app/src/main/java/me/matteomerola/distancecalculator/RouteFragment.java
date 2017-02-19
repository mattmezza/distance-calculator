package me.matteomerola.distancecalculator;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.patloew.rxlocation.RxLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by matt on 2/19/17.
 */

public class RouteFragment extends Fragment implements OnMapReadyCallback, DirectionCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private String[] infoStr;
    private List<LatLng> info;
    private String meanStr;
    private RxLocation locationProvider;
    public static final String FROM ="FROM";
    public static final String TO ="TO";
    public static final String MEAN = "MEAN";
    public static final String TAG = "RouteFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route, container, false);
        Bundle bundle = getArguments();
        if (bundle != null) {
            this.infoStr = new String[2];
            this.infoStr[0] = bundle.getString(FROM);
            this.infoStr[1] = bundle.getString(TO);
            this.info = new ArrayList<>(2);

            this.meanStr = bundle.getString(MEAN);
            this.locationProvider = new RxLocation(getActivity());
            this.locationProvider.setDefaultTimeout(15, TimeUnit.SECONDS);
        }
        MapsInitializer.initialize(getActivity());
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            googleMap.addMarker(new MarkerOptions().position(this.info.get(0)).title(this.infoStr[0]));
            googleMap.addMarker(new MarkerOptions().position(this.info.get(1)).title(this.infoStr[1]));

            ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
            googleMap.addPolyline(DirectionConverter.createPolyline(getActivity(), directionPositionList, 5, Color.RED));
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng : directionPositionList) {
                builder.include(latLng);
            }
            googleMap.setLatLngBoundsForCameraTarget(builder.build());
            int padding = 50; // pixel from the bounding box to the width/height of the mapView
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), padding);
            googleMap.animateCamera(cu);
        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {
        Log.e(TAG, t.getMessage());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Observable.fromArray(infoStr)
                .concatMap(addressString -> {
                    Locale locale = Locale.getDefault();
                    return this.locationProvider.geocoding().fromLocationName(locale, addressString).toObservable();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Address>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Address value) {
                        Log.d(TAG, "Address " + value.getAddressLine(0) + " translated");
                        info.add(new LatLng(value.getLatitude(), value.getLongitude()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        requestDirection(info.get(0), info.get(1));
                    }
                });
    }

    public void requestDirection(LatLng from, LatLng to) {
        GoogleDirection.withServerKey(Keys.DIRECTIONS)
                .from(from)
                .to(to)
                .transportMode(meanStr)
                .execute(this);
    }

    public static RouteFragment withInfo(String from, String to, String mean) {
        Bundle bundle = new Bundle();
        bundle.putString(FROM, from);
        bundle.putString(TO, to);
        bundle.putString(MEAN, mean);
        RouteFragment fragment = new RouteFragment();
        fragment.setArguments(bundle);
        return fragment;
    }
}
