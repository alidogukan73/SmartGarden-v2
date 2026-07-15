package com.ali.smartgarden.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.ali.smartgarden.config.AppInfo;
import com.ali.smartgarden.R;
import com.ali.smartgarden.models.Health;
import com.ali.smartgarden.viewmodels.DeviceHealthViewModel;
import com.google.android.material.button.MaterialButton;

public class AboutActivity extends AppCompatActivity {

    private DeviceHealthViewModel viewModel;

    private MaterialButton btnBack;
    private MaterialButton btnOpenGitHub;

    private TextView txtAppVersion;
    private TextView txtDeviceId;
    private TextView txtBackendVersion;

    private TextView txtDeveloperName;
    private TextView txtDeveloperRole;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_about);

        applyWindowInsets();
        initializeViews();
        initializeViewModel();
        observeViewModel();
        initializeActions();
        renderStaticInfo();
    }


    private void applyWindowInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.aboutRoot),
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


    private void initializeViews() {

        txtDeveloperName =
                findViewById(R.id.txtDeveloperName);

        txtDeveloperRole =
                findViewById(R.id.txtDeveloperRole);

        btnBack =
                findViewById(R.id.btnBack);

        btnOpenGitHub =
                findViewById(R.id.btnOpenGitHub);

        txtAppVersion =
                findViewById(R.id.txtAppVersion);

        txtDeviceId =
                findViewById(R.id.txtDeviceId);

        txtBackendVersion =
                findViewById(R.id.txtBackendVersion);
    }


    private void initializeViewModel() {

        viewModel = new ViewModelProvider(this)
                .get(DeviceHealthViewModel.class);
    }


    private void observeViewModel() {

        viewModel.getHealth().observe(
                this,
                this::renderHealth
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


    private void initializeActions() {

        btnBack.setOnClickListener(
                view -> finish()
        );

        btnOpenGitHub.setOnClickListener(
                view -> openGitHub()
        );
    }


    private void renderStaticInfo() {

        txtAppVersion.setText(
                getString(
                        R.string.about_app_version_format,
                        AppInfo.APP_VERSION
                )
        );

        txtDeviceId.setText(
                AppInfo.DEVICE_ID
        );

        txtDeveloperName.setText(
                AppInfo.DEVELOPER_NAME
        );

        txtDeveloperRole.setText(
                AppInfo.DEVELOPER_ROLE
        );
    }


    private void renderHealth(Health health) {

        if (health == null) {
            return;
        }

        String firmware =
                health.getFirmware();

        txtBackendVersion.setText(
                firmware == null || firmware.isBlank()
                        ? getString(
                        R.string.about_backend_version_waiting
                )
                        : firmware
        );
    }


    private void openGitHub() {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(AppInfo.GITHUB_URL)
                );

        try {

            startActivity(intent);

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    R.string.about_github_open_error,
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}