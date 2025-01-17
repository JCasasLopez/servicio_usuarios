package init.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import init.config.security.UsuarioSecurity;
import init.model.UsuarioDto;
import init.service.CustomUserDetailsManager;
import init.utilidades.Mapeador;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
public class UsuariosController {
	
	CustomUserDetailsManager customUserDetailsManager;
	Mapeador mapeador;
	
	public UsuariosController(CustomUserDetailsManager customUserDetailsManager, Mapeador mapeador) {
		this.customUserDetailsManager = customUserDetailsManager;
		this.mapeador = mapeador;
	}
	
	//loadUserByUsername() es un método interno usado por Spring Security durante
	//el proceso de autenticación y no debe exponerse directamente a los usuarios
	
	@PostMapping(value="altaUsuario", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> altaUsuario(@Valid @RequestBody UsuarioDto usuario){
		UsuarioSecurity usuarioSecurity = new UsuarioSecurity(mapeador.usuarioDtoToUsuario(usuario));
		customUserDetailsManager.createUser(usuarioSecurity);
		return ResponseEntity.status(HttpStatus.CREATED).body("Usuario creado correctamente");
	}
	
	@PutMapping(value="actualizarUsuario", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> actualizarUsuario(@Valid @RequestBody UsuarioDto usuario){
		UsuarioSecurity usuarioSecurity = new UsuarioSecurity(mapeador.usuarioDtoToUsuario(usuario));
		customUserDetailsManager.updateUser(usuarioSecurity);
		return ResponseEntity.status(HttpStatus.OK).body("Usuario actualizado correctamente");
	}
	
	@DeleteMapping(value="borrarUsuario", consumes=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> borrarUsuario(@Valid @RequestParam String username){
		customUserDetailsManager.deleteUser(username);
		return ResponseEntity.status(HttpStatus.OK).body("Usuario borrado correctamente");
	}
	
	@PutMapping(value="cambiarPassword", consumes=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> cambiarPassword(@Valid @RequestParam  String oldPassword, 
																		@RequestParam String newPassword){
		customUserDetailsManager.changePassword(oldPassword, newPassword);
		return ResponseEntity.status(HttpStatus.OK).body("Contraseña cambiada correctamente");
	}
	
	@GetMapping(value="usuarioExiste", consumes=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> usuarioExiste(@Valid @RequestParam  String username){
		boolean response = customUserDetailsManager.userExists(username);
		return ResponseEntity.status(HttpStatus.OK).body(String.valueOf(response));
	}
	
}
