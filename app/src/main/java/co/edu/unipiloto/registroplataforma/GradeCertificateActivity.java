package co.edu.unipiloto.registroplataforma;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityGradeCertificateBinding;

public class GradeCertificateActivity extends AppCompatActivity {

    private ActivityGradeCertificateBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat formatoFechaHora =
            new SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault());

    private int cursoId;
    private int estudianteId;
    private String textoCertificado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGradeCertificateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnDescargarNotas.setOnClickListener(v -> descargarCertificado());

        cargarCertificado();
    }

    private String formatearPorcentaje(double valor) {
        if (Math.abs(valor - Math.rint(valor)) < 0.0001) {
            return String.valueOf((long) Math.rint(valor));
        }
        return String.format(Locale.US, "%.1f", valor);
    }

    private void cargarCertificado() {
        executor.execute(() -> {
            Course curso = db.courseDao().obtenerPorId(cursoId);
            User estudiante = db.userDao().obtenerPorId(estudianteId);

            if (curso == null || estudiante == null) {
                runOnUiThread(this::finish);
                return;
            }

            // El certificado de notas solo existe cuando el curso ya terminó
            if (!curso.yaTermino()) {
                String mensaje = curso.getFechaFinalizacion() > 0
                        ? "El certificado de notas estará disponible desde el "
                          + formatoFechaHora.format(new Date(curso.getFechaFinalizacion()))
                          + ", cuando termine el curso."
                        : "El certificado de notas estará disponible cuando termine el curso.";
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    new AlertDialog.Builder(this)
                            .setTitle("Certificado no disponible")
                            .setMessage(mensaje)
                            .setCancelable(false)
                            .setPositiveButton("Entendido", (d, w) -> finish())
                            .show();
                });
                return;
            }

            GradeCalculator.Resultado res = GradeCalculator.calcular(db, curso, cursoId, estudianteId);
            String texto = construirTexto(curso, estudiante, res);
            textoCertificado = texto;

            runOnUiThread(() -> binding.tvCertificadoNotas.setText(texto));
        });
    }

    private String construirTexto(Course curso, User estudiante, GradeCalculator.Resultado res) {
        String semestre = curso.getSemestre() != null && !curso.getSemestre().isEmpty()
                ? curso.getSemestre() : "No especificado";

        StringBuilder sb = new StringBuilder();
        sb.append("CERTIFICADO DE NOTAS\n\n");
        sb.append("Se certifica que:\n\n");
        sb.append(estudiante.getNombre()).append("\n");
        sb.append(estudiante.getTipoDocumento()).append(" No. ").append(estudiante.getNumeroDocumento()).append("\n\n");
        sb.append("obtuvo las siguientes calificaciones en el curso:\n\n");
        sb.append(curso.getTitulo()).append("\n");
        sb.append("Semestre: ").append(semestre).append("\n");
        sb.append("Id del curso: ").append(curso.getId()).append("\n");
        sb.append("Finalización del curso: ")
                .append(formatoFechaHora.format(new Date(curso.getFechaFinalizacion()))).append("\n\n");

        sb.append("CALIFICACIONES\n");

        for (GradeCalculator.CorteResultado cr : res.cortes) {
            sb.append("\n");
            if (res.ponderado && cr.cuentaEnNotaFinal) {
                sb.append("Corte ").append(cr.corte).append(" (")
                        .append(formatearPorcentaje(cr.pesoCorte)).append("% de la nota final)\n");
            } else {
                sb.append("Corte ").append(cr.corte).append("\n");
            }

            if (cr.registros.isEmpty()) {
                sb.append("Sin actividades registradas\n");
                continue;
            }

            for (GradeCalculator.Registro r : cr.registros) {
                sb.append("- ").append(r.titulo);
                if (res.ponderado) {
                    sb.append(" (").append(formatearPorcentaje(r.porcentaje)).append("%)");
                }
                sb.append(": ");
                sb.append(r.nota != null ? String.valueOf(r.nota) : "Sin calificar");
                sb.append("\n");
            }

            if (res.ponderado && !cr.cuentaEnNotaFinal) {
                sb.append("Este corte no cuenta en la nota final\n");
            } else if (res.ponderado) {
                sb.append(String.format(Locale.getDefault(), "Nota del corte %d: %.2f\n", cr.corte, cr.nota));
            } else if (cr.calificados > 0) {
                sb.append(String.format(Locale.getDefault(), "Promedio del corte %d: %.2f\n", cr.corte, cr.nota));
            }
        }

        sb.append("\n");
        if (res.hayNotas) {
            sb.append(String.format(Locale.getDefault(),
                    res.ponderado ? "NOTA FINAL (ponderada): %.2f\n" : "NOTA FINAL (promedio): %.2f\n",
                    res.notaGeneral));
        } else {
            sb.append("NOTA FINAL: sin calificaciones registradas\n");
        }

        String resultado;
        if (!res.todosCalificados) {
            resultado = "Calificaciones incompletas";
        } else if (res.notaGeneral >= 3.0) {
            resultado = "Aprobado";
        } else {
            resultado = "No aprobado";
        }
        sb.append("Resultado: ").append(resultado).append("\n");
        sb.append("Nota mínima aprobatoria: 3.0\n\n");
        sb.append("Fecha de emisión: ").append(formatoFecha.format(new Date()));

        return sb.toString();
    }

    private void descargarCertificado() {
        if (textoCertificado == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Descargar solo está disponible en Android 10 o superior", Toast.LENGTH_LONG).show();
            return;
        }

        final String contenido = textoCertificado;

        executor.execute(() -> {
            boolean exito = true;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, "certificado_notas_curso_" + cursoId + ".txt");
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri destino = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (destino == null) throw new Exception("No se pudo crear el archivo");

                OutputStream out = getContentResolver().openOutputStream(destino);
                if (out == null) throw new Exception("No se pudo abrir el archivo");
                out.write(contenido.getBytes(StandardCharsets.UTF_8));
                out.close();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                getContentResolver().update(destino, values, null, null);
            } catch (Exception e) {
                exito = false;
            }

            boolean finalExito = exito;
            runOnUiThread(() -> Toast.makeText(this,
                    finalExito ? "Certificado descargado a tu carpeta de Descargas" : "No se pudo descargar el certificado",
                    Toast.LENGTH_LONG).show());
        });
    }
}