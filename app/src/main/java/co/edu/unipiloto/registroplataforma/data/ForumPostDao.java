package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ForumPostDao {

    @Insert
    long insertar(ForumPost comentario);

    @Query("SELECT * FROM comentarios_foro WHERE foroId = :foroId ORDER BY fecha ASC")
    List<ForumPost> listarPorForo(int foroId);

    @Query("SELECT * FROM comentarios_foro WHERE id = :id LIMIT 1")
    ForumPost obtenerPorId(int id);

    @Query("SELECT COUNT(*) FROM comentarios_foro WHERE foroId = :foroId")
    int contarPorForo(int foroId);

    /** Borra todas las participaciones de un foro (se usa al eliminar el foro) */
    @Query("DELETE FROM comentarios_foro WHERE foroId = :foroId")
    void eliminarPorForo(int foroId);

    @Query("SELECT autorId, COUNT(*) as cantidad FROM comentarios_foro " +
            "WHERE foroId IN (SELECT id FROM foros WHERE cursoId = :cursoId) " +
            "AND fecha BETWEEN :desde AND :hasta " +
            "GROUP BY autorId")
    List<ParticipacionEstudiante> contarComentariosPorEstudiante(int cursoId, long desde, long hasta);

    @Query("SELECT autorId, COUNT(DISTINCT foroId) as cantidad FROM comentarios_foro " +
            "WHERE foroId IN (SELECT id FROM foros WHERE cursoId = :cursoId) " +
            "AND fecha BETWEEN :desde AND :hasta " +
            "GROUP BY autorId")
    List<ParticipacionEstudiante> contarForosParticipadosPorEstudiante(int cursoId, long desde, long hasta);
}