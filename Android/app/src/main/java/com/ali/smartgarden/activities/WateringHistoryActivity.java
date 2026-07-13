package com.ali.smartgarden.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.smartgarden.R;
import com.ali.smartgarden.adapters.WateringHistoryAdapter;
import com.ali.smartgarden.models.WateringHistory;
import com.ali.smartgarden.viewmodels.WateringHistoryViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class WateringHistoryActivity extends AppCompatActivity {

    private WateringHistoryViewModel viewModel;
    private WateringHistoryAdapter adapter;

    private MaterialButton btnBack;
    private RecyclerView recyclerHistory;
    private LinearLayout layoutLoading;
    private LinearLayout layoutEmpty;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_watering_history
        );

        applyWindowInsets();
        initializeViews();
        initializeRecyclerView();
        initializeViewModel();
        observeViewModel();
        initializeActions();
    }


    /**
     * Edge-to-edge sistem çubuğu boşluklarını uygular.
     */
    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.historyRoot),
                (view, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );
    }


    /**
     * XML bileşenlerini Java değişkenlerine bağlar.
     */
    private void initializeViews() {

        btnBack =
                findViewById(R.id.btnBack);

        recyclerHistory =
                findViewById(R.id.recyclerHistory);

        layoutLoading =
                findViewById(R.id.layoutLoading);

        layoutEmpty =
                findViewById(R.id.layoutEmpty);
    }


    /**
     * RecyclerView ve adapter yapılandırmasını hazırlar.
     */
    private void initializeRecyclerView() {

        adapter =
                new WateringHistoryAdapter();

        recyclerHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerHistory.setAdapter(
                adapter
        );

        /*
         * RecyclerView kartlarının boyutu sabitse küçük bir
         * performans iyileştirmesi sağlar.
         */
        recyclerHistory.setHasFixedSize(
                true
        );
    }


    /**
     * WateringHistoryViewModel oluşturur.
     */
    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(WateringHistoryViewModel.class);
    }


    /**
     * Geçmiş, yükleniyor ve hata LiveData değerlerini gözlemler.
     */
    private void observeViewModel() {

        viewModel.getHistory().observe(
                this,
                this::renderHistory
        );

        viewModel.getLoading().observe(
                this,
                loading -> {

                    boolean isLoading =
                            Boolean.TRUE.equals(loading);

                    layoutLoading.setVisibility(
                            isLoading
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    if (isLoading) {

                        recyclerHistory.setVisibility(
                                View.GONE
                        );

                        layoutEmpty.setVisibility(
                                View.GONE
                        );
                    }
                }
        );

        viewModel.getError().observe(
                this,
                message -> {

                    if (
                            message == null
                                    || message.isBlank()
                    ) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }


    /**
     * Firebase geçmiş listesini RecyclerView'e aktarır.
     */
    private void renderHistory(
            List<WateringHistory> historyItems
    ) {

        if (historyItems == null) {
            return;
        }

        adapter.submitList(
                historyItems
        );

        boolean isEmpty =
                historyItems.isEmpty();

        recyclerHistory.setVisibility(
                isEmpty
                        ? View.GONE
                        : View.VISIBLE
        );

        layoutEmpty.setVisibility(
                isEmpty
                        ? View.VISIBLE
                        : View.GONE
        );

        layoutLoading.setVisibility(
                View.GONE
        );
    }


    /**
     * Kullanıcı işlemlerini başlatır.
     */
    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );
    }
}