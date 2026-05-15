package pt.com.taskflow.gosolo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.com.taskflow.gosolo.exceptions.BusinessException;
import pt.com.taskflow.gosolo.exceptions.ResourceNotFoundException;
import pt.com.taskflow.gosolo.models.Role;
import pt.com.taskflow.gosolo.models.User;
import pt.com.taskflow.gosolo.models.dto.UserRequest;
import pt.com.taskflow.gosolo.repositories.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 * Testes unitários do UserService — criação, busca e remoção de utilizadores.
 * Sem Spring context, sem DB, so Mockito pa simular os comportamentos.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRequest makeRequest(String name, String email, String password, Role role) {
        UserRequest r = new UserRequest();
        r.setName(name);
        r.setEmail(email);
        r.setPassword(password);
        r.setRole(role);
        return r;
    }

    // -------------------------------------------------------------------------
    // create — validações antes de gravar na base de dados
    // -------------------------------------------------------------------------

    @Test
    void create_throwsWhenPasswordIsNull() {
        // ka da pa criar utilizador sem password
        UserRequest req = makeRequest("Ana", "ana@test.com", null, Role.COLLABORATOR);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void create_throwsWhenPasswordIsBlank() {
        // espacos em branco ka conta como password
        UserRequest req = makeRequest("Ana", "ana@test.com", "   ", Role.COLLABORATOR);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void create_throwsWhenEmailAlreadyInUse() {
        // email único no sistema — dois users ka pode ter o mesmo
        UserRequest req = makeRequest("Ana", "ana@test.com", "secret123", Role.COLLABORATOR);
        when(userRepository.existsByEmail("ana@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void create_succeeds() {
        UserRequest req = makeRequest("Ana", "ana@test.com", "secret123", Role.COLLABORATOR);
        when(userRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        User result = userService.create(req);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Ana");
        assertThat(result.getPassword()).isEqualTo("hashed"); // password tem que estar encriptada
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any());
    }

    @Test
    void create_assignsManagerWhenManagerIdProvided() {
        // colaborador pode ter manager associado na criação
        User manager = new User();
        manager.setId(99L);
        manager.setName("Manager");
        manager.setEmail("manager@test.com");
        manager.setPassword("hashed");
        manager.setRole(Role.MANAGER);

        UserRequest req = makeRequest("Ana", "ana@test.com", "secret123", Role.COLLABORATOR);
        req.setManagerId(99L);

        when(userRepository.existsByEmail("ana@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.findById(99L)).thenReturn(Optional.of(manager));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.create(req);

        assertThat(result.getManager()).isEqualTo(manager);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_throwsWhenNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void findById_returnsUserWhenFound() {
        User user = new User();
        user.setId(1L);
        user.setName("Ana");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Ana");
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_throwsWhenUserNotFound() {
        // ka pode deletar user que ka existe
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_callsRepositoryDelete() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository).delete(user);
    }
}
