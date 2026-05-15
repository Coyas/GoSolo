package pt.com.taskflow.gosolo.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.com.taskflow.gosolo.exceptions.BusinessException;
import pt.com.taskflow.gosolo.exceptions.ResourceNotFoundException;
import pt.com.taskflow.gosolo.models.User;
import pt.com.taskflow.gosolo.models.dto.UserRequest;
import pt.com.taskflow.gosolo.repositories.UserRepository;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado: " + email));
    }

    @Transactional(readOnly = true)
    public List<User> findByManagerId(Long managerId) {
        return userRepository.findByManagerId(managerId);
    }

    public User create(UserRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException("Password obrigatória");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email já está em uso");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        if (request.getManagerId() != null) {
            user.setManager(findById(request.getManagerId()));
        }
        return userRepository.save(user);
    }

    public User update(Long id, UserRequest request) {
        User existing = findById(id);
        existing.setName(request.getName());
        existing.setEmail(request.getEmail());
        existing.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        existing.setManager(request.getManagerId() != null ? findById(request.getManagerId()) : null);
        return userRepository.save(existing);
    }

    public void delete(Long id) {
        userRepository.delete(findById(id));
    }
}
