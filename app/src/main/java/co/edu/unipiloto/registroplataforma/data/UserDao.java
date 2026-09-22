package co.edu.unipiloto.registroplataforma.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    User buscarPorCorreo(String correo);

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario LIMIT 1")
    User buscarPorUsuario(String usuario);

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    User obtenerPorId(int id);

    @Insert
    long insertar(User user);

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario AND password = :password LIMIT 1")
    User login(String usuario, String password);

    @Query("UPDATE usuarios SET password = :nuevaPassword WHERE correo = :correo")
    void actualizarPassword(String correo, String nuevaPassword);
}