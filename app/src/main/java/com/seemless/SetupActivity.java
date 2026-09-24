package com.seemless;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SetupActivity extends AppCompatActivity {
    ActivityResultLauncher<Intent> install;
    ProgressBar progressBar;
    TextView extractedFileTV;
    Button startButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ThemeUtils.setStatusBarAppearance(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        progressBar = findViewById(R.id.progress_bar);
        extractedFileTV = findViewById(R.id.extracted_file);
        startButton = findViewById(R.id.button_start);

        File sdcardDataFolder = getExternalFilesDir(null);

        install = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        copyUriToFile(this, sourceUri, sdcardDataFolder, "model.gguf");
                    };
                });

    }

    private void copyUriToFile(Context context, Uri sourceUri, File destDir, String fileName) {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        Executor executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            File destFile = new File(destDir, fileName);

            try (InputStream input = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream output = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }

            runOnUiThread(() -> {
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.GONE);
                extractedFileTV.setVisibility(View.GONE);
                startButton.setVisibility(View.VISIBLE);
            });
        });
    }


     public void downloadModel(View v){
         Toast.makeText(this,"Download",Toast.LENGTH_SHORT).show();
         startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/mradermacher/translategemma-4b-it-GGUF/blob/main/translategemma-4b-it.Q4_K_M.gguf")));
     }
    public void installModel(View v){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType ("*/*");
        install.launch(intent);
    }

    public void startMain(View v){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
