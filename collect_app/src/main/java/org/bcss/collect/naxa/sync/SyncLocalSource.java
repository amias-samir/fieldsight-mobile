package org.bcss.collect.naxa.sync;

import android.arch.lifecycle.LiveData;

import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.common.database.FieldSightConfigDatabase;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;

public class SyncLocalSource implements BaseLocalDataSourceRX<Sync> {

    private static SyncLocalSource INSTANCE;
    private SyncDAO syncDAO;

    public static SyncLocalSource getINSTANCE() {
        if (INSTANCE == null) {
            INSTANCE = new SyncLocalSource();
        }

        return INSTANCE;
    }


    private SyncLocalSource() {

        FieldSightConfigDatabase database = FieldSightConfigDatabase.getDatabase(Collect.getInstance());//todo inject context
        this.syncDAO = database.getSyncDao();
    }


    @Override
    public LiveData<List<Sync>> getAll() {
        return syncDAO.getAll();
    }

    @Override
    public Completable save(Sync... items) {
        return Completable.fromAction(() -> {
            syncDAO.insert(items);
        });
    }

    @Override
    public Completable save(ArrayList<Sync> items) {
        return Completable.fromAction(() -> {
            syncDAO.insert(items);
        });
    }

    @Override
    public void updateAll(ArrayList<Sync> items) {

    }

    Completable toggleAllChecked() {

        return syncDAO.selectedItemsCount()
                .toObservable()
                .flatMapCompletable(integer -> Completable.fromAction(() -> {
                    if (integer > 0) {
                        syncDAO.markAllAsUnChecked();
                    } else {
                        syncDAO.markAllAsChecked();
                    }

                }));


    }

    public Single<Integer> selectedItemCount(){
        return syncDAO.selectedItemsCount();
    }

    public LiveData<Integer> selectedItemCountLive(){
        return syncDAO.selectedItemsCountLive();
    }

    public Completable toggleSingleItem(Sync sync) {
        return Completable.fromAction(() -> {
            if(sync.isChecked()){
                syncDAO.markAsUnchecked(sync.getUid());
            }else {
                syncDAO.markAsChecked(sync.getUid());
            }
         });
    }
}
