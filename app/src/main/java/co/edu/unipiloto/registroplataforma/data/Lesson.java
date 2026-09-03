package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "lecciones")
public class Lesson {

    public static final String TIPO_VIDEO = "VIDEO";
    public static final String TIPO_DOCUMENTO = "DOCUMENTO";
    public static final String TIPO_OTRO = "OTRO";

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int unidadId;
    private String titulo;
    private String tipo;
    private String urlMaterial;

    public Lesson(int unidadId, String titulo, String tipo, String urlMaterial) {
        this.unidadId = unidadId;
        this.titulo = titulo;
        this.tipo = tipo;
        this.urlMaterial = urlMaterial;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnidadId() { return unidadId; }

    public String getTitulo() { return titulo; }

    public String getTipo() { return tipo; }

    public String getUrlMaterial() { return urlMaterial; }
}