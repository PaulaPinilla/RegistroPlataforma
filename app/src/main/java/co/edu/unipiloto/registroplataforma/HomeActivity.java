package co.edu.unipiloto.registroplataforma;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import co.edu.unipiloto.registroplataforma.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String nombre = getIntent().getStringExtra("nombre");
        if (nombre == null) nombre = "";
        binding.tvBienvenida.setText("¡Hola, " + nombre + "! 👋");
    }
}