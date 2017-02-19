package me.matteomerola.distancecalculator;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.constant.TransportMode;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxRadioGroup;
import com.patloew.rxlocation.RxLocation;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.matteomerola.distancecalculator.exceptions.GeocodingFailureException;
import me.matteomerola.distancecalculator.exceptions.NoRouteAvailableException;
import nl.nl2312.rxcupboard2.RxCupboard;
import nl.nl2312.rxcupboard2.RxDatabase;
import rx.functions.Action1;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by matt on 2/19/17.
 */

public class InputFragment extends Fragment {

    @BindView(R.id.fromEdit)
    EditText fromEdit;
    @BindView(R.id.toEdit)
    EditText toEdit;
    @BindView(R.id.distanceTV)
    TextView distanceText;
    @BindView(R.id.durationTV)
    TextView durationText;
    @BindView(R.id.calculateBtn)
    Button calculateBtn;
    @BindView(R.id.currentLocationBtn)
    ImageView currentLocationBtn;
    @BindView(R.id.savebtn)
    ImageButton saveBtn;
    @BindView(R.id.transportMeanRadio)
    RadioGroup transportationMeanRadio;
    @BindView(R.id.walkRadio)
    RadioButton walkRadio;
    @BindView(R.id.carRadio)
    RadioButton carRadio;
    @BindView(R.id.busRadio)
    RadioButton busRadio;
    @BindView(R.id.bikeRadio)
    RadioButton bikeRadio;
    @BindView(R.id.showRoute)
    ImageButton showRouteBtn;
    private String transportMode = TransportMode.DRIVING;
    private RouteCalculator calculator;
    private RxLocation locationProvider;
    private TripInfoObserver tripInfoObserver;
    private TransportationRadioGroupObserver transportationRadioGroupObserver;
    private ToEditTextFocusObserver toEditTextFocusObserver;
    private RxDatabase rxDatabase;
    private static final String TAG = "InputFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        ButterKnife.bind(this, view);
        this.distanceText.setText("");
        this.durationText.setText("");
        this.rxDatabase = RxCupboard.with(cupboard(), CupboardSQLiteOpenHelper.getConnection(getActivity()));
        this.transportationRadioGroupObserver = new TransportationRadioGroupObserver();
        RxRadioGroup.checkedChanges(this.transportationMeanRadio).subscribe(this.transportationRadioGroupObserver);
        this.transportationMeanRadio.check(R.id.carRadio);
        this.calculator = new RouteCalculator(getActivity());
        this.tripInfoObserver = new TripInfoObserver();
        RxView.clicks(this.calculateBtn).subscribe(aVoid -> subscribeTripInfoObserver());
        this.toEditTextFocusObserver = new ToEditTextFocusObserver();
        RxView.focusChanges(this.toEdit).subscribe(this.toEditTextFocusObserver);
        RxView.clicks(this.currentLocationBtn).subscribe(aVoid -> subscribeLocation(fromEdit));

        this.locationProvider = new RxLocation(getActivity());
        this.locationProvider.setDefaultTimeout(15, TimeUnit.SECONDS);
        this.showRouteBtn.setVisibility(Button.GONE);
        RxView.clicks(this.showRouteBtn).subscribe(aVoid -> {
            ((MainActivity) getActivity()).setFragmentChild(RouteFragment.withInfo(
                    fromEdit.getText().toString(),
                    toEdit.getText().toString(),
                    transportMode
            ), getResources().getString(R.string.route_fragment_name));
        });
        this.saveBtn.setVisibility(Button.INVISIBLE);
        RxView.clicks(this.saveBtn).subscribe(aVoid -> {
            Trip trip = new Trip();
            trip.distance = distanceText.getText().toString();
            trip.duration = durationText.getText().toString();
            trip.fromAddress = fromEdit.getText().toString();
            trip.toAddress = toEdit.getText().toString();
            trip.mean = transportMode;
            Flowable.just(trip)
                    .flatMapSingle(t -> rxDatabase.put(t))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tr ->
                    {
                        rxDatabase.put();
                        showToast(R.string.save_successful, Toast.LENGTH_SHORT);
                        saveBtn.setVisibility(View.INVISIBLE);
                    }, throwable -> showToast(R.string.error_saving_trip, Toast.LENGTH_LONG));
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPlayServicesAvailable();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void subscribeLocation(final EditText which) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Location permission has not been granted.
            ((MainActivity) getActivity()).requestLocationPermission();
        } else {
            // Location permission is already available
            locationProvider.location().lastLocation()
                .flatMap(location -> locationProvider.geocoding().fromLocation(location))
                .subscribe(
                        address -> which.setText(getAddressText(address)),
                        throwable -> Log.d(TAG, throwable.getMessage())
                );
        }
    }

    private void subscribeTripInfoObserver() {
        this.distanceText.setText("");
        this.durationText.setText("");
        this.showRouteBtn.setVisibility(Button.GONE);
        this.saveBtn.setVisibility(Button.INVISIBLE);
        this.calculator.calculateDistanceStringObservable(
                this.fromEdit.getText().toString(),
                this.toEdit.getText().toString(),
                this.transportMode
        ).subscribe(this.tripInfoObserver);
    }

    private void showDialog(String msg) {
        if(getActivity()!=null) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(msg)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            dialog.show();
        }
    }

    private void checkPlayServicesAvailable() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int status = apiAvailability.isGooglePlayServicesAvailable(getActivity());

        if(status != ConnectionResult.SUCCESS) {
            if(apiAvailability.isUserResolvableError(status)) {
                apiAvailability.getErrorDialog(getActivity(), status, 1).show();
            } else {
                showToast(R.string.google_play_error, Toast.LENGTH_SHORT);
            }
        }
    }

    private void showToast(int id, int length) {
        if(getActivity()!=null) {
            Toast.makeText(getActivity(), id, length).show();
        }
    }

    private void showToast(String msg, int length) {
        if(getActivity()!=null) {
            Toast.makeText(getActivity(), msg, length).show();
        }
    }

    private class ToEditTextFocusObserver implements Action1<Boolean> {

        @Override
        public void call(Boolean focused) {
            if (!focused) {
                // focus lost
                if (fromEdit.getText().length() > 2) {
                    subscribeTripInfoObserver();
                }
            }
        }
    }

    private String getAddressText(Address address) {
        String addressText = "";
        final int maxAddressLineIndex = address.getMaxAddressLineIndex();

        for(int i=0; i<=maxAddressLineIndex; i++) {
            addressText += address.getAddressLine(i);
            if(i != maxAddressLineIndex) { addressText += "\n"; }
        }

        return addressText;
    }

    private class TransportationRadioGroupObserver implements Action1<Integer> {

        @Override
        public void call(Integer integer) {
            switch (integer) {
                case R.id.bikeRadio:
                    transportMode = TransportMode.BICYCLING;
                    break;
                case R.id.busRadio:
                    transportMode = TransportMode.TRANSIT;
                    break;
                case R.id.carRadio:
                    transportMode = TransportMode.DRIVING;
                    break;
                default:
                    transportMode = TransportMode.WALKING;
            }
            if (fromEdit.getText().length() > 2 && toEdit.getText().length() > 2) {
                subscribeTripInfoObserver();
            }
        }
    }

    private class TripInfoObserver implements Observer<RouteCalculator.TripInfo> {

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(RouteCalculator.TripInfo value) {
            distanceText.setText(value.distance);
            durationText.setText(value.duration);
            showRouteBtn.setVisibility(Button.VISIBLE);
            saveBtn.setVisibility(Button.VISIBLE);
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof NoRouteAvailableException) {
                showToast(e.getMessage(), Toast.LENGTH_LONG);
            } else if (e instanceof GeocodingFailureException) {
                showToast(R.string.check_addresses, Toast.LENGTH_LONG);
            } else {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onComplete() {

        }
    }

}
