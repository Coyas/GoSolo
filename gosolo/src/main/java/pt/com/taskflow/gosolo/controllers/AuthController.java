package pt.com.taskflow.gosolo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import pt.com.taskflow.gosolo.config.JwtUtil;
import pt.com.taskflow.gosolo.models.dto.AuthResponse;
import pt.com.taskflow.gosolo.models.dto.LoginRequest;
import pt.com.taskflow.gosolo.services.UserService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Operation(summary = "Login e obtenção de token JWT", description = "Devolve token JWT, role e userId do utilizador autenticado.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("")
                .replace("ROLE_", "");

        String token = jwtUtil.generateToken(auth.getName(), role);
        var user = userService.findByEmail(auth.getName());
        return ResponseEntity.ok(new AuthResponse(token, role, user.getId(), user.getName()));
    }
}
