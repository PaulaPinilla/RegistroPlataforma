package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

    private int unidadId;
    private int profesorId;
    private int estudianteId;
    private boolean esProfesor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAssignmentListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        unidadId = getIntent().getIntExtra("unidadId", -1);
        profesorId = getIntent().getIntExtra("profesorId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnAgregarActividad.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnAgregarActividad.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssignmentFormActivity.class);
            intent.putExtra("unidadId", unidadId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(new SimpleListAdapter.OnItemClickListener() {
            @Override
            public void onClick(SimpleListAdapter.Item item) {
                if (esProfesor) {
                    Intent intent = new Intent(AssignmentListActivity.this, SubmissionListActivity.class);
                    intent.putExtra("actividadId", item.id);
                    intent.putExtra("actividadTitulo", item.titulo);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(AssignmentListActivity.this, AssignmentDetailActivity.class);
                    intent.putExtra("actividadId", item.id);
                    intent.putExtra("estudianteId", estudianteId);
                    startActivity(intent);
                }
            }

            @Override
            public void onEditar(SimpleListAdapter.Item item) {
                Intent intent = new Intent(AssignmentListActivity.this, AssignmentFormActivity.class);
                intent.putExtra("unidadId", unidadId);
                intent.putExtra("actividadId", item.id);
                startActivity(intent);
            }

            @Override
            public void onEliminar(SimpleListAdapter.Item item) {
                confirmarEliminar(item);
            }
        });
        binding.rvActividades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvActividades.setAdapter(adapter);
    }

    private void confirmarEliminar(SimpleListAdapter.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar actividad")
                .setMessage("¿Seguro que quieres eliminar \"" + item.titulo + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarActividad(item.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarActividad(int idActividad) {
        executor.execute(() -> {
            Assignment a = db.assignmentDao().obtenerPorId(idActividad);
            if (a != null) {
                db.assignmentDao().eliminar(a);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Actividad eliminada", Toast.LENGTH_SHORT).show();
                cargarActividades();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarActividades();
    }

    private void cargarActividades() {
        executor.execute(() -> {
            List<Assignment> actividades = db.assignmentDao().listarPorUnidad(unidadId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Assignment a : actividades) {
                String subtitulo = categoriaLegible(a.getCategoria()) + " — Entrega antes del " + formato.format(a.getFechaLimite());
                boolean entregada = !esProfesor && db.submissionDao().obtenerEntrega(a.getId(), estudianteId) != null;
                items.add(new SimpleListAdapter.Item(a.getId(), a.getTitulo(), subtitulo, entregada, esProfesor));
            }
            runOnUiThread(() -> {
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }

    static String categoriaLegible(String categoria) {
        if (Assignment.CATEGORIA_PARCIAL.equals(categoria)) return "Parcial";
        if (Assignment.CATEGORIA_QUIZ.equals(categoria)) return "Quiz";
        if (Assignment.CATEGORIA_FORO.equals(categoria)) return "Foro";
        if (Assignment.CATEGORIA_VIDEO.equals(categoria)) return "Video";
        if (Assignment.CATEGORIA_PREGUNTAS.equals(categoria)) return "Preguntas";
        return " Taller";
    }
}