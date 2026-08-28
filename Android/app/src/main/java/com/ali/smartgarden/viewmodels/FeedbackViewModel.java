package com.ali.smartgarden.viewmodels;

import androidx.lifecycle.ViewModel;

import com.ali.smartgarden.firebase.FirebaseRepository;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;

/** Builds the privacy-limited feedback payload and delegates persistence. */
public final class FeedbackViewModel extends ViewModel {
    private final FirebaseRepository repository = new FirebaseRepository();

    public Task<Void> submit(String type, String areaCode, String areaLabel,
                             String subject, String description, String contact,
                             Map<String, Object> diagnostics) {
        Map<String, Object> values = new HashMap<>();
        values.put("type", type);
        values.put("area", areaCode);
        values.put("area_label", areaLabel);
        values.put("subject", subject);
        values.put("description", description);
        if (contact != null && !contact.isBlank()) {
            values.put("contact_email", contact);
        }
        if (diagnostics != null && !diagnostics.isEmpty()) {
            values.put("diagnostics", new HashMap<>(diagnostics));
        }
        return repository.submitFeedback(values);
    }
}
