package co.edu.unipiloto.registroplataforma;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

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
import co.edu.unipiloto.registroplataforma.databinding.ActivityGradesBinding;

public class GradesActivity extends AppCompatActivity {

    private ActivityGradesBinding binding;
    private AppDatabase db;
    private SimpleListAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFechaHora =
            new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault());

    private int cursoId;
    private int estudianteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGradesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());

        binding.btnObtenerCertificado.setOnClickListener(v -> {
            Intent intent = new Intent(this, CertificateActivity.class);
            intent.putExtra("cursoId", cursoId);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });

        binding.btnCertificadoNotas.setOnClickListener(v -> {
            Intent intent = new Intent(this, GradeCertificateActivity.class);
            intent.putExtra("cursoId", cursoId);
            intent.putExtra("estudianteId", estudianteId);
            startActivity(intent);
        });

        adapter = new SimpleListAdapter(item -> { /* solo lectura */ });
        binding.rvCalificaciones.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCalificaciones.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCalificaciones();
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    private void cargarCalificaciones() {
        executor.execute(() -> {
            Course curso = db.courseDao().obtenerPorId(cursoId);
            GradeCalculator.Resultado res = GradeCalculator.calcular(db, curso, cursoId, estudianteId);

            // Lista de pantalla: un encabezado por corte y debajo sus actividades
            List<SimpleListAdapter.Item> items = new ArrayList<>();

            for (GradeCalculator.CorteResultado cr : res.cortes) {
                String encabezado;
                if (cr.registros.isEmpty()) {
                    encabezado = "Corte " + cr.corte + " — sin actividades";
                } else if (res.ponderado) {
                    if (!cr.cuentaEnNotaFinal) {
                        encabezado = "Corte " + cr.corte + " — no cuenta en la nota final";
                    } else {
                        encabezado = String.format(Locale.getDefault(), "Corte %d (%s%% del curso) — %s: %.2f",
                                cr.corte, formatearPorcentaje(cr.pesoCorte),
                                cr.pendientes > 0 ? "Nota acumulada" : "Nota", cr.nota);
                    }
                } else if (cr.calificados > 0) {
                    encabezado = String.format(Locale.getDefault(),
                            "Corte %d — Promedio: %.2f", cr.corte, cr.nota);
                } else {
                    encabezado = "Corte " + cr.corte + " — sin calificar aún";
                }
                items.add(new SimpleListAdapter.Item(encabezado));

                for (GradeCalculator.Registro r : cr.registros) {
                    String subtitulo = r.nota != null ? "Calificación: " + r.nota : "Sin calificar";
                    if (res.ponderado) {
                        subtitulo += " — vale " + formatearPorcentaje(r.porcentaje) + "% del corte";
                    }
                    items.add(new SimpleListAdapter.Item(r.id, r.titulo, subtitulo, r.nota != null));
                }
            }

            boolean cursoTermino = curso != null && curso.yaTermino();

            // Solo se aprueba (y se habilita el certificado de finalización) si el curso ya terminó
            boolean aprobado = res.todosCalificados && res.notaGeneral >= 3.0 && cursoTermino;

            String tituloCurso = curso != null ? curso.getTitulo() : "Calificaciones";

            String promedioTexto;
            if (!res.hayNotas) {
                promedioTexto = "Todavía no tienes actividades calificadas en este curso.";
            } else if (res.ponderado) {
                promedioTexto = String.format(Locale.getDefault(),
                        res.todosCalificados ? "Nota final del curso: %.2f" : "Nota acumulada del curso: %.2f",
                        res.notaGeneral);
            } else {
                promedioTexto = String.format(Locale.getDefault(),
                        "Promedio general del curso: %.2f\n(El profesor aún no define todos los porcentajes)",
                        res.notaGeneral);
            }

            // Disponibilidad del certificado de notas: solo cuando el curso termina
            String disponibilidad;
            if (cursoTermino) {
                disponibilidad = "";
            } else if (curso != null && curso.getFechaFinalizacion() > 0) {
                disponibilidad = "El certificado de notas estará disponible desde el "
                        + formatoFechaHora.format(new Date(curso.getFechaFinalizacion()))
                        + ", cuando termine el curso.";
            } else {
                disponibilidad = "El certificado de notas estará disponible cuando termine el curso.";
            }

            runOnUiThread(() -> {
                binding.tvTituloCurso.setText(tituloCurso);
                binding.tvPromedio.setText(promedioTexto);
                adapter.actualizarDatos(items);
                binding.tvVacio.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                binding.btnObtenerCertificado.setVisibility(aprobado ? View.VISIBLE : View.GONE);

                binding.btnCertificadoNotas.setEnabled(cursoTermino);
                binding.tvDisponibilidadCertificado.setText(disponibilidad);
                binding.tvDisponibilidadCertificado.setVisibility(cursoTermino ? View.GONE : View.VISIBLE);
            });
        });
    }
}