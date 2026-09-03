package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Unit;
import co.edu.unipiloto.registroplataforma.databinding.ActivityUnitListBinding;

public class UnitListActivity extends AppCompatActivity {

    private ActivityUnitListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int cursoId;
    private String cursoTitulo;
    private int profesorId;
    private int estudianteId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnitListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        cursoTitulo = getIntent().getStringExtra("cursoTitulo");
        profesorId = getIntent().getIntExtra("profesorId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;

        binding.tvTitulo.setText(cursoTitulo != null ? cursoTitulo : "Unidades del curso");
        binding.btnVolver.setOnClickListener(v -> finish());

        binding.btnActividades.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignmentListActivity.class);
            intent.putExtra("cursoId", cursoId);
            if (esProfesor) {
                intent.putExtra("profesorId", profesorId);
            } else {
                intent.putExtra("estudianteId", estudianteId);
            }
            startActivity(intent);
        });

        binding.btnAgregarUnidad.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAgregarUnidad.setOnClickListener(v -> mostrarDialogoNuevaUnidad());

        adapter = new SimpleListAdapter(item -> abrirLecciones(item.id, item.titulo));
        binding.rvUnidades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUnidades.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUnidades();
    }

    private void abrirLecciones(int unidadId, String unidadTitulo) {
        Intent intent = new Intent(this, LessonListActivity.class);
        intent.putExtra("unidadId", unidadId);
        intent.putExtra("unidadTitulo", unidadTitulo);
        if (esProfesor) {
            intent.putExtra("profesorId", profesorId);
        } else {
            intent.putExtra("estudianteId", estudianteId);
        }
        startActivity(intent);
    }

    private void mostrarDialogoNuevaUnidad() {
        final EditText input = new EditText(this);
        input.setHint("Título de la unidad (ej: Unidad 1 - Introducción)");

        new AlertDialog.Builder(this)
                .setTitle("Nueva unidad")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String titulo = input.getText().toString().trim();
                    if (titulo.isEmpty()) return;
                    executor.execute(() -> {
                        db.unitDao().insertar(new Unit(cursoId, titulo));
                        runOnUiThread(this::cargarUnidades);
                    });
                })
                .show();
    }

    private void cargarUnidades() {
        executor.execute(() -> {
            List<Unit> unidades = db.unitDao().listarPorCurso(cursoId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Unit u : unidades) {
                items.add(new SimpleListAdapter.Item(u.getId(), u.getTitulo(), null, false));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}