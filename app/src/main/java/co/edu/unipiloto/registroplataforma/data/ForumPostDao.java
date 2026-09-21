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

    @Query("SELECT COUNT(*) FROM comentarios_foro WHERE foroId = :foroId")
    int contarPorForo(int foroId);
}