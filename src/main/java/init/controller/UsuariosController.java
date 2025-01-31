package init.controller;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import init.config.security.UsuarioSecurity;
import init.entities.StandardResponse;
import init.model.UsuarioDto;
import init.service.BlockAccountService;
import init.service.CustomUserDetailsManager;
import init.service.JwtService;
import init.utilidades.Mapeador;
import jakarta.validation.Valid;

@CrossOrigin("*")
@RestController
public class UsuariosController {

	CustomUserDetailsManager customUserDetailsManager;
	Mapeador mapeador;
	JwtService jwtService;
	BlockAccountService blockAccountService;

	public UsuariosController(CustomUserDetailsManager customUserDetailsManager, Mapeador mapeador,
			JwtService jwtService, BlockAccountService blockAccountService) {
		this.customUserDetailsManager = customUserDetailsManager;
		this.mapeador = mapeador;
		this.jwtService = jwtService;
		this.blockAccountService = blockAccountService;
	}

	//loadUserByUsername() es un método interno usado por Spring Security durante
	//el proceso de autenticación y no debe exponerse directamente a los usuarios

	//***** RECUERDA INCLUIR UNA LISTA "ROLES" VACÍA EN EL JSON ******
	@PostMapping(value="/altaUsuario", consumes=MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<StandardResponse> altaUsuario(@Valid @RequestBody UsuarioDto usuario){
		customUserDetailsManager.passwordIsValid(usuario.getPassword());
		UsuarioSecurity usuarioSecurity = new UsuarioSecurity(mapeador.usuarioDtoToUsuario(usuario));
		customUserDetailsManager.createUser(usuarioSecurity);
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), "Usuario creado correctamente", null,
																				HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
	}

	@DeleteMapping(value="/borrarUsuario", produces=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<StandardResponse> borrarUsuario(@Valid @RequestParam String username){
		customUserDetailsManager.deleteUser(username);
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), "Usuario borrado correctamente", null,
																		HttpStatus.OK);
	    return ResponseEntity.status(HttpStatus.OK).body(respuesta);
	}

	@PutMapping(value="/cambiarPassword", produces=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<StandardResponse> cambiarPassword(@Valid @RequestParam  String oldPassword, 
			@RequestParam String newPassword){
		customUserDetailsManager.changePassword(oldPassword, newPassword);
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), "Contraseña cambiada correctamente", 
																		null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(respuesta);
	}

	/*@GetMapping(value="/usuarioExiste", produces=MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> usuarioExiste(@Valid @RequestParam  String username){
		boolean response = customUserDetailsManager.userExists(username);
		return ResponseEntity.status(HttpStatus.OK).body(String.valueOf(response));
	}*/

	@PostMapping(value="/crearAdmin")
	public ResponseEntity<StandardResponse> crearAdmin(@RequestParam String username){
		customUserDetailsManager.upgradeUser(customUserDetailsManager.findUser(username));
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), "Usuario promocionado a ADMIN correctamente", 
				null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(respuesta);
	}

	@PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_SUPERADMIN')")
	@PostMapping(value="/desbloquearCuenta")
	public ResponseEntity<StandardResponse> desbloquearCuenta(@RequestParam String username){
		blockAccountService.desbloquearCuenta(username);
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), "La cuenta del usuario " + username +
				"se ha sido desbloqueada correctamente", null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(respuesta);
	}

	@GetMapping(value="/usuarioEsAdmin")
	public ResponseEntity<StandardResponse> usuarioEsAdmin(@RequestParam String username){
		boolean usuarioEsAdmin = customUserDetailsManager.isUserAdmin(username);
		String mensaje;
		if (usuarioEsAdmin) {
			mensaje = "El usuario " + username + " es administrador.";
		} else {
			mensaje = "El usuario " + username + " no es administrador.";
		}
		StandardResponse respuesta = new StandardResponse (LocalDateTime.now(), mensaje, null, HttpStatus.OK);
		return ResponseEntity.status(HttpStatus.OK).body(respuesta);
	}
}
