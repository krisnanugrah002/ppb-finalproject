package com.example.finalprojectppb;

import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectppb.databinding.ActivityResultBinding;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;
    private TextToSpeech tts;
    private String objectName;
    private String englishName;
    private String category;
    private String fact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        objectName = getIntent().getStringExtra("NAMA_OBJEK");
        englishName = getIntent().getStringExtra("INGGRIS_OBJEK");
        category = getIntent().getStringExtra("KATEGORI_OBJEK");
        fact = getIntent().getStringExtra("FAKTA_OBJEK");

        if (imageUriString != null) {
            binding.ivResultImage.setImageURI(Uri.parse(imageUriString));
        }

        binding.tvObjectName.setText(objectName != null ? objectName : "Tidak Dikenal");
        binding.tvEnglishName.setText(englishName != null ? englishName : "Unknown");
        binding.tvCategoryContent.setText(category != null ? category : "Tidak ada data kategori.");
        binding.tvFactContent.setText(fact != null ? fact : "Tidak ada fakta menarik.");

        binding.btnBack.setOnClickListener(v -> finish());

        binding.cvCategoryHeader.setOnClickListener(v ->
                toggleDropdown(binding.cvCategoryContent, binding.ivCategoryArrow));

        binding.cvFactHeader.setOnClickListener(v ->
                toggleDropdown(binding.cvFactContent, binding.ivFactArrow));

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.forLanguageTag("id-ID"));
            }
        });

        binding.btnSpeak.setOnClickListener(v -> {
            String textToSpeak = buildSpeechText();
            if (tts != null && !textToSpeak.isEmpty()) {
                tts.setLanguage(new Locale("id", "ID"));

                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ResultTTS");
            }
        });
    }

    private String buildSpeechText() {
        StringBuilder sb = new StringBuilder();
        if (objectName != null && !objectName.isEmpty()) {
            sb.append(objectName).append(". ");
        }
        if (englishName != null && !englishName.isEmpty()) {
            sb.append("Bahasa Inggrisnya adalah, ").append(englishName).append(". ");
        }
        return sb.toString();
    }

    private boolean isEnglishWord(String text) {
        return text.matches("[A-Za-z .,!?']+");
    }

    private void toggleDropdown(View contentLayout, ImageView arrowImageView) {
        if (contentLayout.getVisibility() == View.VISIBLE) {
            contentLayout.setVisibility(View.GONE);
            arrowImageView.setRotation(0);
        } else {
            contentLayout.setVisibility(View.VISIBLE);
            arrowImageView.setRotation(180);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}