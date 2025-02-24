package init.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import init.entities.Usuario;

@Repository
public interface UsuariosDao extends JpaRepository<Usuario, Integer> {
	/*
	Estos métodos sirven para nuestra implementación personalizada de UserDetailsManager (Spring Security):
	       DAO     ->     UserDetailsManager
	- findByUsername() -> loadUserByUsername() 
	- save(), que no hace falta declarar -> createUser(), updateUser() 
	- deleteByUsername() -> deleteUser() 
	- updatePassword() -> changePassword() 
	- existsByUsername() -> userExists()
	*/
	
	Optional<Usuario> findByUsername(String username);
	
	Optional<Usuario> findByEmail(String email);
	
	void deleteByUsername(String username);
	
	@Modifying
    @Query("UPDATE Usuario u SET u.password = ?2 WHERE u.username = ?1")
    void updatePassword(String username, String newPassword);
	
	boolean existsByUsername(String username);
		
}
