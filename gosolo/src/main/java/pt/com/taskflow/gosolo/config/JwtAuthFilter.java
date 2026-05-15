package pt.com.taskflow.gosolo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/*
 * Filtro que corre em cada request — extrai o token JWT do header Authorization,
 * valida-o e injeta a autenticação no SecurityContext. Se o token for inválido
 * ou estiver ausente, deixa passar sem autenticação (o Spring Security trata do resto).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        /**
         * O token JWT é extraído do header Authorization, validado e, se válido, as
         * informações de autenticação (email e role) são extraídas e injetadas no
         * SecurityContext. Se o token for inválido ou ausente, a requisição continua
         * sem autenticação,
         * permitindo que o Spring Security bloqueie o acesso a endpoints protegidos.
         * 
         * Mas acho q seria interessante usar regular expressios depois:
         * // garante que começa com "Bearer ", e (?<token>.*) captura o resto na
         * variável 'token'
         * 
         */
        // private static final Pattern token =
        // Pattern.compile("^Bearer\\s+(?<token>.*)$");

        String token = header.substring(7); // remove o prefixo "Bearer "

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
