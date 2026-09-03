package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "entregas")
public class Submission {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int actividadId;
    private int estudianteId;
    private String uriArchivo;
    private String nombreArchivo;
    private long fechaEntrega;

    public Submission(int actividadId, int estudianteId, String uriArchivo, String nombreArchivo, long fechaEntrega) {
        this.actividadId = actividadId;
        this.estudianteId = estudianteId;
        this.uriArchivo = uriArchivo;
        this.nombreArchivo = nombreArchivo;
        this.fechaEntrega = fechaEntrega;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getActividadId() { return actividadId; }
    public int getEstudianteId() { return estudianteId; }
    public String getUriArchivo() { return uriArchivo; }
    public String getNombreArchivo() { return nombreArchivo; }
    public long getFechaEntrega() { return fechaEntrega; }
}
