package init.service;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import init.dao.TokensDao;
import init.entities.Rol;
import init.entities.TokenJwt;
import init.entities.TokenStatus;
import init.entities.Usuario;
import init.exception.TokenNotFoundException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
	TokensDao tokensDao;
	
	public JwtService(TokensDao tokensDao) {
		this.tokensDao = tokensDao;
	}
	
	//El token normal expira a los 30 minutos (30*60*1000 milisegundos)
	private final long expiration = 30 * 60 * 1000;
	
	//El token para el reseteo de contraseña expira a los 5 minutos (5*60*1000 milisegundos)
	private final long expirationReset = 5 * 60 * 1000;

	//@Value("{jwt.secret.key}")
	private String secretKey = "RXN0YWVzbWljbGF2ZXNlY3JldGFwZXJmZWN0YWNvbnVubW9udG9uZGVieXRlc1ZpbmRlbDM5ISE=";
	byte[] keyBytes = Base64.getDecoder().decode(secretKey);
    SecretKey clave = Keys.hmacShaKeyFor(keyBytes);
	
    @Transactional
	public String createToken() {
		     
        Authentication authenticated = SecurityContextHolder.getContext().getAuthentication();
		
		String token =  Jwts
						.builder()
						.header() 
						.type("JWT")
							.and()
						.subject(authenticated.getName()) 
						.claim("roles", authenticated.getAuthorities().stream()
				                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
						.issuedAt(new Date(System.currentTimeMillis()))
						.expiration(new Date(System.currentTimeMillis() + expiration))
						.signWith(clave, Jwts.SIG.HS256)
						.compact();
		
		tokensDao.save(new TokenJwt(token));	
		return token;
		}
    
    @Transactional
    public String createTokenResetPassword(Usuario usuario) {
    //La lógica de este método es ligeramente diferente a la de createToken(), ya que aquí 
    //1) no hay ningún usuario autenticado, así que el nombre y las credenciales hay que sacarlas del 
    //usuario, y que se pasa como parámetro y, 2) expira a los 5' (30' para el token "normal")
    	
    	String token =  Jwts
    			.builder()
    			.header() 
    			.type("JWT")
    			.and()
    			.subject(usuario.getUsername()) 
    			.claim("roles", usuario.getRoles().stream()
    					.map(Rol::getNombreRol).collect(Collectors.toList()))
    			.issuedAt(new Date(System.currentTimeMillis()))
    			.expiration(new Date(System.currentTimeMillis() + expirationReset))
    			.signWith(clave, Jwts.SIG.HS256)
    			.compact();
    	
    	tokensDao.save(new TokenJwt(token));	
    	return token;
    }
    
	public Claims extractPayload(String token) {
		try {

			Jws<Claims> tokenParseado = Jwts.parser() 	//Configura cómo queremos verificar el token
					.verifyWith(clave) 					//Establece la clave que se usará para verificar la firma
					.build() 							//Construye el parser JWT con la configuración especificada
					.parseSignedClaims(token); 			//Aquí es donde ocurren TODAS las verificaciones 

			return tokenParseado.getPayload();

		} catch (JwtException ex) {
			
			if (ex instanceof io.jsonwebtoken.ExpiredJwtException) {
				throw new JwtException("Token expirado");
			} else if (ex instanceof io.jsonwebtoken.MalformedJwtException) {
				throw new JwtException("Token malformado");
			} else if (ex instanceof io.jsonwebtoken.security.SecurityException) {
				throw new JwtException("Firma inválida");
			}
			throw new JwtException("Error al verificar el token: " + ex.getMessage());
		}
	}
	
	public String logUserOut(String token) {
		/*En los comentarios de más abajo, la palabra "sesión" está entre comillas porque con los 
		tokens JWT no hay sesiones como tal, son stateless*/
		Optional<TokenJwt> tokenJwtOptional = tokensDao.findByToken(token);
		TokenJwt tokenJwt = tokenJwtOptional.orElseThrow(
				() -> new TokenNotFoundException("El token no existe en la base de datos"));
		
		if(!(tokenJwt.getValidez() == TokenStatus.ACTIVO)) {
			//La "sesión" ya estaba inactiva
			return "La sesión ya estaba inactiva";
		} else {
			//La "sesión" aún estaba activa. Se cambia su campo "isLoggedOut" a true, se graba 
			//y se vacía el SecurityContext -> no hay usuario autenticado
			tokenJwt.setValidez(TokenStatus.LOGGED_OUT);
			tokensDao.save(tokenJwt);
			SecurityContextHolder.getContext().setAuthentication(null);
			return "El usuario ha abandonado la sesión";
		}
	}
	
	public void invalidateResetToken(String token) {
		//Los tokens para resetear la contraseña solo se pueden usar 1 vez por motivos de seguridad
		Optional<TokenJwt> tokenJwtOptional = tokensDao.findByToken(token);
		TokenJwt tokenJwt = tokenJwtOptional.orElseThrow(
				() -> new TokenNotFoundException("El token no existe en la base de datos"));
		tokenJwt.setValidez(TokenStatus.GASTADO);
		tokensDao.save(tokenJwt);
	}
}