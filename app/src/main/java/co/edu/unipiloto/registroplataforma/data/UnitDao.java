package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UnitDao {

    @Insert
    long insertar(Unit unidad);

    @Query("SELECT * FROM unidades WHERE cursoId = :cursoId ORDER BY id ASC")
    List<Unit> listarPorCurso(int cursoId);

    @Query("SELECT * FROM unidades WHERE id = :id LIMIT 1")
    Unit obtenerPorId(int id);
}