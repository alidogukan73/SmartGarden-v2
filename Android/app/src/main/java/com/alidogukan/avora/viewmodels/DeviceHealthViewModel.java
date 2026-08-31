package com.alidogukan.avora.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.alidogukan.avora.R;
import com.alidogukan.avora.firebase.FirebaseRepository;
import com.alidogukan.avora.language.AvoraLanguageManager;
import com.alidogukan.avora.models.Health;

/** Lifecycle-aware device health state and device commands. */
public class DeviceHealthViewModel extends AndroidViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();
    private final MediatorLiveData<Health> health = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public DeviceHealthViewModel(@NonNull Application application) {
        super(application);
        LiveData<Health> source = repository.observeHealth(databaseError -> {
            loading.setValue(false);
            error.setValue(AvoraLanguageManager.localizedContext(
                    getApplication()).getString(
                    R.string.device_health_read_error));
        });
        health.addSource(source, value -> {
            health.setValue(value);
            error.setValue(null);
            loading.setValue(false);
        });
    }

    public LiveData<Health> getHealth() { return health; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public void restartDevice() { repository.restartDevice(); }
}
