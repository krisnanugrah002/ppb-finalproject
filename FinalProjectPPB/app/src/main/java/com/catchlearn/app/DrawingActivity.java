package com.catchlearn.app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.catchlearn.app.databinding.ActivityDrawingBinding;

public class DrawingActivity extends AppCompatActivity {

    private ActivityDrawingBinding binding;
    private String objectName;
    private String englishName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDrawingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        objectName = getIntent().getStringExtra("NAMA_OBJEK");
        englishName = getIntent().getStringExtra("INGGRIS_OBJEK");

        binding.tvDrawingTitle.setText("Latihan Menulis");

        String instruction = "Tulis ";
        if (objectName != null && !objectName.isEmpty()) {
            instruction += objectName;
        } else {
            instruction += "nama objek";
        }

        if (englishName != null && !englishName.isEmpty()) {
            instruction += " / " + englishName;
        }

        instruction += " pada kanvas di bawah ini";
        binding.tvDrawingInstruction.setText(instruction);

        // Tombol kembali
        binding.btnBackDrawing.setOnClickListener(v -> finish());

        // Tombol Undo (Hapus coretan terakhir)
        binding.btnUndo.setOnClickListener(v -> {
            if (binding.drawingCanvas.canUndo()) {
                binding.drawingCanvas.undoLastStroke();
                Toast.makeText(this, "Coretan terakhir dihapus", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tidak ada yang bisa dihapus", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol hapus semua
        binding.btnClear.setOnClickListener(v -> {
            binding.drawingCanvas.clearCanvas();
            Toast.makeText(this, "Semua coretan dihapus!", Toast.LENGTH_SHORT).show();
        });

        // Tombol pilih warna
        binding.btnColorWhite.setOnClickListener(v -> {
            binding.drawingCanvas.setDrawColor(Color.WHITE);
            highlightSelectedColor(0);
            Toast.makeText(this, "Warna Putih", Toast.LENGTH_SHORT).show();
        });

        binding.btnColorRed.setOnClickListener(v -> {
            binding.drawingCanvas.setDrawColor(Color.parseColor("#FF5252"));
            highlightSelectedColor(1);
            Toast.makeText(this, "Warna Merah", Toast.LENGTH_SHORT).show();
        });

        binding.btnColorYellow.setOnClickListener(v -> {
            binding.drawingCanvas.setDrawColor(Color.parseColor("#FFD740"));
            highlightSelectedColor(2);
            Toast.makeText(this, "Warna Kuning", Toast.LENGTH_SHORT).show();
        });

        binding.btnColorGreen.setOnClickListener(v -> {
            binding.drawingCanvas.setDrawColor(Color.parseColor("#69F0AE"));
            highlightSelectedColor(3);
            Toast.makeText(this, "Warna Hijau", Toast.LENGTH_SHORT).show();
        });

        binding.btnColorBlue.setOnClickListener(v -> {
            binding.drawingCanvas.setDrawColor(Color.parseColor("#448AFF"));
            highlightSelectedColor(4);
            Toast.makeText(this, "Warna Biru", Toast.LENGTH_SHORT).show();
        });

        // Tombol ukuran brush
        binding.btnBrushSmall.setOnClickListener(v -> {
            binding.drawingCanvas.setStrokeWidth(8);
            highlightSelectedBrush(0);
            Toast.makeText(this, "Ukuran Kecil", Toast.LENGTH_SHORT).show();
        });

        binding.btnBrushMedium.setOnClickListener(v -> {
            binding.drawingCanvas.setStrokeWidth(12);
            highlightSelectedBrush(1);
            Toast.makeText(this, "Ukuran Sedang", Toast.LENGTH_SHORT).show();
        });

        binding.btnBrushLarge.setOnClickListener(v -> {
            binding.drawingCanvas.setStrokeWidth(20);
            highlightSelectedBrush(2);
            Toast.makeText(this, "Ukuran Besar", Toast.LENGTH_SHORT).show();
        });

        highlightSelectedColor(0);
        highlightSelectedBrush(1);
    }

    private void highlightSelectedColor(int index) {

        binding.btnColorWhite.setAlpha(0.5f);
        binding.btnColorRed.setAlpha(0.5f);
        binding.btnColorYellow.setAlpha(0.5f);
        binding.btnColorGreen.setAlpha(0.5f);
        binding.btnColorBlue.setAlpha(0.5f);

        // Highlight warna yang dipilih
        switch (index) {
            case 0:
                binding.btnColorWhite.setAlpha(1f);
                binding.btnColorWhite.setScaleX(1.1f);
                binding.btnColorWhite.setScaleY(1.1f);
                resetOtherColorScale(0);
                break;
            case 1:
                binding.btnColorRed.setAlpha(1f);
                binding.btnColorRed.setScaleX(1.1f);
                binding.btnColorRed.setScaleY(1.1f);
                resetOtherColorScale(1);
                break;
            case 2:
                binding.btnColorYellow.setAlpha(1f);
                binding.btnColorYellow.setScaleX(1.1f);
                binding.btnColorYellow.setScaleY(1.1f);
                resetOtherColorScale(2);
                break;
            case 3:
                binding.btnColorGreen.setAlpha(1f);
                binding.btnColorGreen.setScaleX(1.1f);
                binding.btnColorGreen.setScaleY(1.1f);
                resetOtherColorScale(3);
                break;
            case 4:
                binding.btnColorBlue.setAlpha(1f);
                binding.btnColorBlue.setScaleX(1.1f);
                binding.btnColorBlue.setScaleY(1.1f);
                resetOtherColorScale(4);
                break;
        }
    }

    private void resetOtherColorScale(int exceptIndex) {
        if (exceptIndex != 0) {
            binding.btnColorWhite.setScaleX(1f);
            binding.btnColorWhite.setScaleY(1f);
        }
        if (exceptIndex != 1) {
            binding.btnColorRed.setScaleX(1f);
            binding.btnColorRed.setScaleY(1f);
        }
        if (exceptIndex != 2) {
            binding.btnColorYellow.setScaleX(1f);
            binding.btnColorYellow.setScaleY(1f);
        }
        if (exceptIndex != 3) {
            binding.btnColorGreen.setScaleX(1f);
            binding.btnColorGreen.setScaleY(1f);
        }
        if (exceptIndex != 4) {
            binding.btnColorBlue.setScaleX(1f);
            binding.btnColorBlue.setScaleY(1f);
        }
    }

    private void highlightSelectedBrush(int index) {

        binding.btnBrushSmall.setAlpha(0.6f);
        binding.btnBrushMedium.setAlpha(0.6f);
        binding.btnBrushLarge.setAlpha(0.6f);

        // Highlight ukuran yang dipilih
        switch (index) {
            case 0:
                binding.btnBrushSmall.setAlpha(1f);
                break;
            case 1:
                binding.btnBrushMedium.setAlpha(1f);
                break;
            case 2:
                binding.btnBrushLarge.setAlpha(1f);
                break;
        }
    }
}