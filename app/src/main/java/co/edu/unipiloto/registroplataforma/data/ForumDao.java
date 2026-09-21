package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ForumDao {

    @Insert
    long insertar(Forum foro);

    @Query("SELECT * FROM foros WHERE cursoId = :cursoId ORDER BY id DESC")
    List<Forum> listarPorCurso(int cursoId);

    @Query("SELECT * FROM foros WHERE id = :id LIMIT 1")
    Forum obtenerPorId(int id);
}