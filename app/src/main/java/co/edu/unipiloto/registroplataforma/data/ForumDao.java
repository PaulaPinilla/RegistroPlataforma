package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ForumDao {

    @Insert
    long insertar(Forum foro);

    @Update
    void actualizar(Forum foro);

    @Delete
    void eliminar(Forum foro);

    @Query("SELECT * FROM foros WHERE cursoId = :cursoId ORDER BY id DESC")
    List<Forum> listarPorCurso(int cursoId);

    @Query("SELECT * FROM foros WHERE id = :id LIMIT 1")
    Forum obtenerPorId(int id);
}