package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Assignment;
import co.edu.unipiloto.registroplataforma.databinding.ActivityAssignmentListBinding;

public class AssignmentListActivity extends AppCompatActivity {

    private ActivityAssignmentListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int cursoId;
    private int profesorId;
    private int estudianteId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        profesorId = getIntent().getIntExtra("profesorId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAgregarActividad.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAgregarActividad.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignmentFormActivity.class);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(item -> {
            if (esProfesor) {
                Intent intent = new Intent(this, SubmissionListActivity.class);
                intent.putExtra("actividadId", item.id);
                intent.putExtra("actividadTitulo", item.titulo);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, AssignmentDetailActivity.class);
                intent.putExtra("actividadId", item.id);
                intent.putExtra("estudianteId", estudianteId);
                startActivity(intent);
            }
        });
        binding.rvActividades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActividades.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarActividades();
    }

    private void cargarActividades() {
        executor.execute(() -> {
            List<Assignment> actividades = db.assignmentDao().listarPorCurso(cursoId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Assignment a : actividades) {
                String subtitulo = "Entrega antes del " + formato.format(a.getFechaLimite());
                boolean entregada = !esProfesor && db.submissionDao().obtenerEntrega(a.getId(), estudianteId) != null;
                items.add(new SimpleListAdapter.Item(a.getId(), a.getTitulo(), subtitulo, entregada));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}