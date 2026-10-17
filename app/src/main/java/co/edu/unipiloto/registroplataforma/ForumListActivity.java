package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.Forum;
import co.edu.unipiloto.registroplataforma.databinding.ActivityForumListBinding;

public class ForumListActivity extends AppCompatActivity {

    private ActivityForumListBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int cursoId;
    private int usuarioId;
    private boolean esProfesor;

    // Estado del curso (se consulta cada vez que la pantalla vuelve a mostrarse)
    private volatile boolean cursoTerminado = false;
    private volatile long fechaFinCurso = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForumListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        int profesorId = getIntent().getIntExtra("profesorId", -1);
        int estudianteId = getIntent().getIntExtra("estudianteId", -1);
        esProfesor = profesorId != -1;
        usuarioId = esProfesor ? profesorId : estudianteId;

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnCrearForo.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnCrearForo.setOnClickListener(v -> {
            if (cursoTerminado) {
                mostrarAvisoCursoTerminado();
                return;
            }
            Intent intent = new Intent(this, ForumFormActivity.class);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        binding.btnReporte.setVisibility(esProfesor ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnReporte.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForumReportActivity.class);
            intent.putExtra("cursoId", cursoId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(new SimpleListAdapter.OnItemClickListener() {
            @Override
            public void onClick(SimpleListAdapter.Item item) {
                Intent intent = new Intent(ForumListActivity.this, ForumDetailActivity.class);
                intent.putExtra("foroId", item.id);
                intent.putExtra("usuarioId", usuarioId);
                startActivity(intent);
            }

            @Override
            public void onEditar(SimpleListAdapter.Item item) {
                if (cursoTerminado) {
                    mostrarAvisoCursoTerminado();
                    return;
                }
                Intent intent = new Intent(ForumListActivity.this, ForumFormActivity.class);
                intent.putExtra("cursoId", cursoId);
                intent.putExtra("foroId", item.id);
                startActivity(intent);
            }

            @Override
            public void onEliminar(SimpleListAdapter.Item item) {
                if (cursoTerminado) {
                    mostrarAvisoCursoTerminado();
                    return;
                }
                confirmarEliminarForo(item);
            }
        });
        binding.rvForos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvForos.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarForos();
    }

    private void mostrarAvisoCursoTerminado() {
        String fin = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(fechaFinCurso));
        new AlertDialog.Builder(this)
                .setTitle("Curso finalizado")
                .setMessage("Este curso finalizó el " + fin
                        + ". Ya no se pueden crear, editar ni eliminar foros.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private void confirmarEliminarForo(SimpleListAdapter.Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar foro")
                .setMessage("¿Seguro que quieres eliminar el foro \"" + item.titulo + "\"? "
                        + "También se borrarán todas las participaciones y calificaciones de ese foro. "
                        + "Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarForo(item.id))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarForo(int idForo) {
        executor.execute(() -> {
            Forum foro = db.forumDao().obtenerPorId(idForo);
            if (foro != null) {
                // Se borra todo junto: si algo falla no queda a medias
                db.runInTransaction(() -> {
                    db.forumPostDao().eliminarPorForo(idForo);
                    db.forumGradeDao().eliminarPorForo(idForo);
                    db.forumDao().eliminar(foro);
                });
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "Foro eliminado", Toast.LENGTH_SHORT).show();
                cargarForos();
            });
        });
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    private void cargarForos() {
        executor.execute(() -> {
            // Primero se consulta si el curso ya terminó (mismo hilo, en orden)
            if (cursoId != -1) {
                Course curso = db.courseDao().obtenerPorId(cursoId);
                cursoTerminado = esProfesor && curso != null && curso.yaTermino();
                fechaFinCurso = curso != null ? curso.getFechaFinalizacion() : 0;
            }
            boolean mostrarAcciones = esProfesor && !cursoTerminado;

            List<Forum> foros = db.forumDao().listarPorCurso(cursoId);
            List<SimpleListAdapter.Item> items = new ArrayList<>();
            for (Forum f : foros) {
                int cantidad = db.forumPostDao().contarPorForo(f.getId());
                String estado;
                if (!f.yaAbrio()) {
                    estado = "Aún no abre";
                } else if (f.yaCerro()) {
                    estado = "Cerrado";
                } else {
                    estado = cantidad + (cantidad == 1 ? " participación" : " participaciones");
                }
                String valor = f.getPorcentaje() > 0
                        ? "Corte " + f.getCorte() + " - vale " + formatearPorcentaje(f.getPorcentaje()) + "%"
                        : "Corte " + f.getCorte() + " - sin porcentaje definido";
                items.add(new SimpleListAdapter.Item(f.getId(), f.getTema(), valor + " - " + estado,
                        false, mostrarAcciones));
            }
            runOnUiThread(() -> {
                binding.btnCrearForo.setAlpha(cursoTerminado ? 0.5f : 1f);
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            });
        });
    }
}