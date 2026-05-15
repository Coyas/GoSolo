package pt.com.taskflow.gosolo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.com.taskflow.gosolo.models.User;

import java.util.List;
import java.util.Optional;

/*
findByEmail: Permite encontrar um usuário com base no seu endereço de email,
para autenticação e recuperação de senha.

findByManagerId: Permite encontrar todos os usuários que têm um gerente específico, 
útil para exibir a equipe de um gerente.

existsByEmail: Verifica se um email já está registrado no sistema, útil para validação 
durante o registro de novos usuários.

*/

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByManagerId(Long managerId);

    boolean existsByEmail(String email);
}
