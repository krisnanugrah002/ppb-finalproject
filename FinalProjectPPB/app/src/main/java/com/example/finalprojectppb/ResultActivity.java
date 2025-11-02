package com.example.finalprojectppb;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.finalprojectppb.databinding.ActivityResultBinding;

public class ResultActivity extends AppCompatActivity {

    private ActivityResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Ambil data dari Intent
        String imageUriString = getIntent().getStringExtra("IMAGE_URI");
        String objectName = getIntent().getStringExtra("NAMA_OBJEK");
        String englishName = getIntent().getStringExtra("INGGRIS_OBJEK");
        String category = getIntent().getStringExtra("KATEGORI_OBJEK");
        String fact = getIntent().getStringExtra("FAKTA_OBJEK");

        // 2. Tampilkan gambar
        if (imageUriString != null) {
            binding.ivResultImage.setImageURI(Uri.parse(imageUriString));
        }

        // 3. Tampilkan data teks
        binding.tvObjectName.setText(objectName != null ? objectName : "Tidak Dikenal");
        binding.tvEnglishName.setText(englishName != null ? englishName : "Unknown");
        binding.tvCategoryContent.setText(category != null ? category : "Tidak ada data kategori.");
        binding.tvFactContent.setText(fact != null ? fact : "Tidak ada fakta menarik.");

        // 4. Set listener tombol kembali ("Ulangi")
        binding.btnBack.setOnClickListener(v -> {
            finish(); // Tutup ResultActivity dan kembali ke MainActivity
        });

        // 5. Set listener untuk dropdown Kategori
        binding.cvCategoryHeader.setOnClickListener(v -> {
            toggleDropdown(binding.cvCategoryContent, binding.ivCategoryArrow);
        });

        // 6. Set listener untuk dropdown Fakta Menarik
        binding.cvFactHeader.setOnClickListener(v -> {
            toggleDropdown(binding.cvFactContent, binding.ivFactArrow);
        });
    }

    /**
     * Fungsi helper untuk membuka/menutup dropdown
     * @param contentLayout CardView yang berisi konten (untuk di-toggle)
     * @param arrowImageView ImageView panah (untuk diputar)
     */
    private void toggleDropdown(View contentLayout, ImageView arrowImageView) {
        if (contentLayout.getVisibility() == View.VISIBLE) {
            // Tutup dropdown
            contentLayout.setVisibility(View.GONE);
            arrowImageView.setRotation(0); // Panah ke bawah
        } else {
            // Buka dropdown
            contentLayout.setVisibility(View.VISIBLE);
            arrowImageView.setRotation(180); // Panah ke atas
        }
    }
}

