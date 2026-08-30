package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "inscripciones")
public class Enrollment {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int cursoId;
    private int estudianteId;
    private long fechaInscripcion;

    public Enrollment(int cursoId, int estudianteId, long fechaInscripcion) {
        this.cursoId = cursoId;
        this.estudianteId = estudianteId;
        this.fechaInscripcion = fechaInscripcion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCursoId() { return cursoId; }
    public int getEstudianteId() { return estudianteId; }
    public long getFechaInscripcion() { return fechaInscripcion; }
}
