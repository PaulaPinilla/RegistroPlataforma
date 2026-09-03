package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "progreso_lecciones")
public class LessonProgress {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int leccionId;
    private int estudianteId;
    private long fechaCompletada;

    public LessonProgress(int leccionId, int estudianteId, long fechaCompletada) {
        this.leccionId = leccionId;
        this.estudianteId = estudianteId;
        this.fechaCompletada = fechaCompletada;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLeccionId() { return leccionId; }
    public int getEstudianteId() { return estudianteId; }
    public long getFechaCompletada() { return fechaCompletada; }
}