package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.WateringHistory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WateringHistoryViewModel extends ViewModel {

    private static final int HISTORY_LIMIT = 50;

    private final FirebaseRepository repository;

    private final MutableLiveData<List<WateringHistory>> history =
            new MutableLiveData<>(Collections.emptyList());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(true);

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    private Query historyQuery;
    private ValueEventListener historyListener;


    public WateringHistoryViewModel() {

        repository = new FirebaseRepository();

        observeHistory();
    }


    /**
     * Firebase'deki son sulama kayıtlarını gerçek zamanlı dinler.
     */
    private void observeHistory() {

        loading.setValue(true);

        /*
         * Kayıt anahtarları tarih tabanlı olduğu için
         * key üzerinden sıralayıp son 50 kaydı alıyoruz.
         */
        historyQuery = repository
                .getHistoryRef()
                .orderByKey()
                .limitToLast(HISTORY_LIMIT);

        historyListener = new ValueEventListener() {

            @Override
            public void onDataChange(
                    @NonNull DataSnapshot snapshot
            ) {

                List<WateringHistory> historyItems =
                        new ArrayList<>();

                for (DataSnapshot childSnapshot
                        : snapshot.getChildren()) {

                    WateringHistory item =
                            childSnapshot.getValue(
                                    WateringHistory.class
                            );

                    if (item == null) {
                        continue;
                    }

                    item.setRecordId(
                            childSnapshot.getKey()
                    );

                    historyItems.add(
                            item
                    );
                }

                /*
                 * Firebase ascending sıralı döndürür.
                 * En yeni kayıt üstte görünsün diye ters çeviriyoruz.
                 */
                Collections.reverse(
                        historyItems
                );

                history.setValue(
                        historyItems
                );

                loading.setValue(
                        false
                );
            }

            @Override
            public void onCancelled(
                    @NonNull DatabaseError databaseError
            ) {

                loading.setValue(
                        false
                );

                error.setValue(
                        databaseError.getMessage()
                );
            }
        };

        historyQuery.addValueEventListener(
                historyListener
        );
    }


    /**
     * Sulama geçmişi listesi.
     */
    public LiveData<List<WateringHistory>> getHistory() {

        return history;
    }


    /**
     * Yükleniyor durumu.
     */
    public LiveData<Boolean> getLoading() {

        return loading;
    }


    /**
     * Firebase hata mesajı.
     */
    public LiveData<String> getError() {

        return error;
    }


    /**
     * ViewModel yok edilirken Firebase listener temizlenir.
     */
    @Override
    protected void onCleared() {

        super.onCleared();

        if (
                historyQuery != null
                        && historyListener != null
        ) {

            historyQuery.removeEventListener(
                    historyListener
            );
        }
    }
}