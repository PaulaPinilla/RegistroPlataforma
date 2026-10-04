package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "comentarios_foro")
public class ForumPost {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private int foroId;
    private int autorId;
    private String autorNombre;
    private String texto;
    private long fecha;
    private String uriArchivo;
    private String nombreArchivo;
    private Integer respondiendoAId;

    public ForumPost(int foroId, int autorId, String autorNombre, String texto, long fecha,
                     String uriArchivo, String nombreArchivo, Integer respondiendoAId) {
        this.foroId = foroId;
        this.autorId = autorId;
        this.autorNombre = autorNombre;
        this.texto = texto;
        this.fecha = fecha;
        this.uriArchivo = uriArchivo;
        this.nombreArchivo = nombreArchivo;
        this.respondiendoAId = respondiendoAId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getForoId() { return foroId; }

    public int getAutorId() { return autorId; }

    public String getAutorNombre() { return autorNombre; }

    public String getTexto() { return texto; }

    public long getFecha() { return fecha; }

    public String getUriArchivo() { return uriArchivo; }

    public String getNombreArchivo() { return nombreArchivo; }

    public Integer getRespondiendoAId() { return respondiendoAId; }

    public boolean tieneArchivo() { return uriArchivo != null; }

    public boolean esRespuesta() { return respondiendoAId != null; }
}