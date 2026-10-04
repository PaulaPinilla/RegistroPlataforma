package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "calificaciones_foro",
        indices = {@Index(value = {"foroId", "estudianteId"}, unique = true)})
public class ForumGrade {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int foroId;
    private int estudianteId;
    private String estudianteNombre;
    private double calificacion;
    private String comentario;

    public ForumGrade(int foroId, int estudianteId, String estudianteNombre, double calificacion, String comentario) {
        this.foroId = foroId;
        this.estudianteId = estudianteId;
        this.estudianteNombre = estudianteNombre;
        this.calificacion = calificacion;
        this.comentario = comentario;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getForoId() { return foroId; }
    public int getEstudianteId() { return estudianteId; }
    public String getEstudianteNombre() { return estudianteNombre; }
    public double getCalificacion() { return calificacion; }
    public void setCalificacion(double calificacion) { this.calificacion = calificacion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}