package init.config.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import init.dao.UsuariosDao;
import init.entities.Usuario;
import init.service.BlockAccountService;
import init.utilidades.Constants;
import init.utilidades.StandardResponseHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

	BlockAccountService blockAccountService;
	UsuariosDao usuariosDao; 
	CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	StandardResponseHandler standardResponseHandler;

	public CustomAuthenticationFailureHandler(BlockAccountService blockAccountService, 
			UsuariosDao usuariosDao, CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
			StandardResponseHandler standardResponseHandler) {
		this.blockAccountService = blockAccountService;
		this.usuariosDao = usuariosDao;
		this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
		this.standardResponseHandler = standardResponseHandler;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String username = (String) request.getParameter("username");
		Usuario usuario = usuariosDao.findByUsername(username);
		if(usuario == null) {
			//Aunque esta excepción debería manejarse en el CustomAuthenticationEntryPoint, 
			//Spring Security no la propaga correctamente, así que se maneja directamente 
			//en este punto para garantizar que el flujo funcione correctamente 
			standardResponseHandler.handleResponse(response, 404, "No existe el usuario " + username, 
					null);
			return;

		} else {
			if(usuario.isCuentaBloqueada()) {
				standardResponseHandler.handleResponse(response, 401, 
						"La cuenta está bloqueada. Contacte con soporte", null);
			} else {
				blockAccountService.incrementarIntentosFallidos(usuario);
				int intentosRestantes = Constants.MAX_INTENTOS_FALLIDOS - usuario.getIntentosFallidos();

				if (usuario.getIntentosFallidos() >= Constants.MAX_INTENTOS_FALLIDOS) {
					standardResponseHandler.handleResponse(response, 401, 
							"Credenciales incorrectas. Su cuenta ha sido bloqueada por seguridad", null);
				} else {
					standardResponseHandler.handleResponse(response, 401, 
							"Credenciales incorrectas. Le quedan " + intentosRestantes + " intentos", null);
				}
			}
			SecurityContextHolder.clearContext(); // Limpiamos la autenticación fallida
		}
	}
}
