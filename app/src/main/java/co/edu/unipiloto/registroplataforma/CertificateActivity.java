package co.edu.unipiloto.registroplataforma;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import co.edu.unipiloto.registroplataforma.data.AppDatabase;
import co.edu.unipiloto.registroplataforma.data.Course;
import co.edu.unipiloto.registroplataforma.data.User;
import co.edu.unipiloto.registroplataforma.databinding.ActivityCertificateBinding;

public class CertificateActivity extends AppCompatActivity {

    private ActivityCertificateBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private int cursoId;
    private int estudianteId;
    private String textoCertificado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCertificateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        cursoId = getIntent().getIntExtra("cursoId", -1);
        estudianteId = getIntent().getIntExtra("estudianteId", -1);

        binding.btnVolver.setOnClickListener(v -> finish());
        binding.btnDescargar.setOnClickListener(v -> descargarCertificado());

        cargarCertificado();
    }

    private void cargarCertificado() {
        executor.execute(() -> {
            Course curso = db.courseDao().obtenerPorId(cursoId);
            User estudiante = db.userDao().obtenerPorId(estudianteId);

            runOnUiThread(() -> {
                if (curso == null || estudiante == null) {
                    finish();
                    return;
                }

                String fechaHoy = formatoFecha.format(System.currentTimeMillis());
                String semestre = curso.getSemestre() != null && !curso.getSemestre().isEmpty()
                        ? curso.getSemestre() : "No especificado";

                textoCertificado = "CERTIFICADO DE FINALIZACIÓN\n\n" +
                        "Universidad Piloto de Colombia\n\n" +
                        "Se certifica que:\n\n" +
                        estudiante.getNombre() + "\n" +
                        estudiante.getTipoDocumento() + " No. " + estudiante.getNumeroDocumento() + "\n\n" +
                        "completó satisfactoriamente el curso:\n\n" +
                        curso.getTitulo() + "\n" +
                        "Semestre: " + semestre + "\n" +
                        "Id del curso: " + curso.getId() + "\n\n" +
                        "Fecha de finalización: " + fechaHoy;

                binding.tvCertificado.setText(textoCertificado);
            });
        });
    }

    private void descargarCertificado() {
        if (textoCertificado == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(this, "Descargar solo está disponible en Android 10 o superior", Toast.LENGTH_LONG).show();
            return;
        }

        executor.execute(() -> {
            boolean exito = true;
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, "certificado_curso_" + cursoId + ".txt");
                values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri destino = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (destino == null) throw new Exception("No se pudo crear el archivo");

                OutputStream out = getContentResolver().openOutputStream(destino);
                out.write(textoCertificado.getBytes());
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