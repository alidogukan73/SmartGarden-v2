package com.ali.smartgarden.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.ali.smartgarden.models.Statistics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

public class StatisticsViewModel extends ViewModel {

    private final FirebaseRepository repository;

    private final MutableLiveData<Statistics> statistics =
            new MutableLiveData<>();

    private final MutableLiveData<String> error =
            new MutableLiveData<>();

    public StatisticsViewModel() {

        repository = new FirebaseRepository();

        observeStatistics();
    }

    /**
     * Firebase statistics düğümünü dinler.
     */
    private void observeStatistics() {

        repository.observeStatistics(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(
                            @NonNull DataSnapshot snapshot
                    ) {

                        Statistics value =
                                snapshot.getValue(
                                        Statistics.class
                                );

                        statistics.postValue(
                                value
                        );
                    }

                    @Override
                    public void onCancelled(
                            @NonNull DatabaseError databaseError
                    ) {

                        error.postValue(
                                databaseError.getMessage()
                        );
                    }

                }

        );

    }

    /**
     * Statistics LiveData
     */
    public LiveData<Statistics> getStatistics() {

        return statistics;
    }

    /**
     * Error LiveData
     */
    public LiveData<String> getError() {

        return error;
    }

}