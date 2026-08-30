package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAvailableCoursesBinding;

public class AvailableCoursesActivity extends AppCompatActivity {

    private ActivityAvailableCoursesBinding binding;
    private AppDatabase db;
    private CourseAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAvailableCoursesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        adapter = new CourseAdapter(CourseAdapter.Modo.DISPONIBLE, curso -> abrirDetalle(curso.getId()));

        binding.rvCursos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCursos.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCursos();
    }

    private void abrirDetalle(int cursoId) {
        Intent intent = new Intent(this, CourseDetailActivity.class);
        intent.putExtra("cursoId", cursoId);
        intent.putExtra("estudianteId", estudianteId);
        startActivity(intent);
    }

    private void cargarCursos() {
        executor.execute(() -> {
            List<Course> cursos = db.courseDao().listarDisponiblesParaEstudiante(estudianteId);
            runOnUiThread(() -> {
                adapter.actualizarDatos(cursos);
                binding.tvVacio.setVisibility(adapter.estaVacio() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}