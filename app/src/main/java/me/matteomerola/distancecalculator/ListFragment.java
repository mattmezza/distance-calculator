package me.matteomerola.distancecalculator;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import nl.nl2312.rxcupboard2.OnDatabaseChange;
import nl.nl2312.rxcupboard2.RxCupboard;
import nl.nl2312.rxcupboard2.RxDatabase;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by matt on 2/19/17.
 */

public class ListFragment extends Fragment implements TripAdapter.OnClickListener {

    private RecyclerView recyclerView;
    private TripAdapter tripAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RxDatabase db;
    private List<Trip> addedTrips;
    private static final String TAG = "ListFragment";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        this.recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        this.addedTrips = new ArrayList<>();
        this.tripAdapter = new TripAdapter(new ArrayList<Trip>(), R.layout.trip_item);
        this.tripAdapter.setOnClickListener(this);
        this.recyclerView.setAdapter(this.tripAdapter);
        this.recyclerView.setItemAnimator(new DefaultItemAnimator());
        this.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        this.swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);
        this.swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        this.swipeRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                        getResources().getDisplayMetrics()));
        this.swipeRefreshLayout.setEnabled(true);
        this.swipeRefreshLayout.setOnRefreshListener(() -> swipeRefreshLayout.setRefreshing(false));
        this.swipeRefreshLayout.setRefreshing(true);
        this.recyclerView.setVisibility(View.GONE);
        this.db = RxCupboard.with(cupboard(), CupboardSQLiteOpenHelper.getConnection(getActivity()));

        this.db.changes(Trip.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnDatabaseChange<Trip>() {
                    @Override
                    public void onDelete(Trip item) {
                        tripAdapter.removeTripItem(item);
                        tripAdapter.notifyDataSetChanged();
                        Toast.makeText(getActivity(), R.string.trip_deleted, Toast.LENGTH_SHORT).show();
                    }
                }, toastErrorAction);

        this.db.query(Trip.class)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toObservable()
                .subscribe(new Observer<Trip>() {

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Trip trip) {
                        addedTrips.add(trip);
                        tripAdapter.addTripItem(addedTrips.size() - 1, trip);
                    }

                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, t.getMessage());
                        swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onComplete() {
                        swipeRefreshLayout.setRefreshing(false);
                        tripAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
        return view;
    }

    @Override
    public void onClick(View view, int pos) {
        Trip trip = this.tripAdapter.getTripItemAt(pos);
        ((MainActivity) getActivity()).setFragmentChild(RouteFragment.withInfo(
                trip.fromAddress,
                trip.toAddress,
                trip.mean
        ), getResources().getString(R.string.route_fragment_name));
    }

    @Override
    public void onLongClick(View view, int pos) {
        Trip trip = this.tripAdapter.getTripItemAt(pos);
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(R.string.deletion_confirm)
                .setPositiveButton(R.string.yes, (DialogInterface d, int i) -> {
                    db.delete(trip).toObservable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(t -> Toast.makeText(getActivity(), R.string.trip_deleted, Toast.LENGTH_SHORT).show(),
                                    toastErrorAction);
                })
                .setNegativeButton(R.string.no, (DialogInterface d, int i) -> d.dismiss())
                .create();
        dialog.show();
    }

    private Consumer<Throwable> toastErrorAction = throwable -> Toast.makeText(getActivity(), throwable.toString(), Toast.LENGTH_SHORT).show();

}
