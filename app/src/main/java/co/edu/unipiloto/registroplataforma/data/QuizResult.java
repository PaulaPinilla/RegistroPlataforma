package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "resultados_evaluacion")
public class QuizResult {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int actividadId;
    private int estudianteId;
    private String respuestas;
    private long fechaEnvio;
    private Double calificacion;
    private String retroalimentacion;

    public QuizResult(int actividadId, int estudianteId, String respuestas, long fechaEnvio) {
        this.actividadId = actividadId;
        this.estudianteId = estudianteId;
        this.respuestas = respuestas;
        this.fechaEnvio = fechaEnvio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActividadId() { return actividadId; }
    public int getEstudianteId() { return estudianteId; }
    public String getRespuestas() { return respuestas; }
    public long getFechaEnvio() { return fechaEnvio; }

    public Double getCalificacion() { return calificacion; }
    public void setCalificacion(Double calificacion) { this.calificacion = calificacion; }

    public String getRetroalimentacion() { return retroalimentacion; }
    public void setRetroalimentacion(String retroalimentacion) { this.retroalimentacion = retroalimentacion; }

    public boolean estaCalificada() { return calificacion != null; }
}