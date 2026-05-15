package pt.com.taskflow.gosolo.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import pt.com.taskflow.gosolo.models.Role;
import pt.com.taskflow.gosolo.models.User;
import pt.com.taskflow.gosolo.models.VacationRequest;
import pt.com.taskflow.gosolo.models.VacationStatus;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * Testes de repositório com @DataJpaTest + H2 em memória.
 * Aqui testamos as queries JPQL customizadas — que são o coração
 * da lógica de sobreposição de férias. Flyway desligado,
 * Hibernate cria o schema a partir das entidades.
 */
@DataJpaTest
@TestPropertySource(properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VacationRequestRepositoryTest {

        @Autowired
        private TestEntityManager em;

        @Autowired
        private VacationRequestRepository repository;

        // utilizadores base criados antes de cada teste
        private User manager;
        private User collaboratorA;
        private User collaboratorB;

        @BeforeEach
        void setUp() {
                manager = persistUser("Manager", "manager@test.com", Role.MANAGER, null);
                collaboratorA = persistUser("Ana", "ana@test.com", Role.COLLABORATOR, manager);
                collaboratorB = persistUser("Bruno", "bruno@test.com", Role.COLLABORATOR, manager);
        }

        private User persistUser(String name, String email, Role role, User mgr) {
                User u = new User();
                u.setName(name);
                u.setEmail(email);
                u.setPassword("hashed");
                u.setRole(role);
                u.setManager(mgr);
                return em.persist(u);
        }

        private VacationRequest persistVacation(User user, LocalDate start, LocalDate end, VacationStatus status) {
                VacationRequest v = new VacationRequest();
                v.setUser(user);
                v.setStartDate(start);
                v.setEndDate(end);
                v.setStatus(status);
                return em.persist(v);
        }

        // -------------------------------------------------------------------------
        // existsOverlappingActiveVacation — verifica sobreposição por utilizador
        // (PENDING e APPROVED contam, REJECTED ka conta)
        // -------------------------------------------------------------------------

        @Test
        void existsOverlappingActiveVacation_trueWhenPendingOverlaps() {
                persistVacation(collaboratorA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15));

                assertThat(result).isTrue();
        }

        @Test
        void existsOverlappingActiveVacation_trueWhenApprovedOverlaps() {
                persistVacation(collaboratorA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.APPROVED);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 20));

                assertThat(result).isTrue();
        }

        @Test
        void existsOverlappingActiveVacation_falseWhenRejectedInSamePeriod() {
                // rejected ka bloqueia — colaborador pode pedir de novo no mesmo período
                persistVacation(collaboratorA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.REJECTED);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingActiveVacation_falseWhenOtherUserOverlaps() {
                // sobreposição de outro user ka afeta o collaboratorA — check é por userId
                persistVacation(collaboratorB, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingActiveVacation_falseWhenNoOverlap() {
                persistVacation(collaboratorA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 20));

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingActiveVacation_trueWhenAdjacentDatesOverlap() {
                // o dia final do pedido existente é igual ao dia inicial do novo — conta como
                // sobreposição
                persistVacation(collaboratorA, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10),
                                VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingActiveVacation(
                                collaboratorA.getId(), LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 15));

                assertThat(result).isTrue();
        }

        // -------------------------------------------------------------------------
        // existsOverlappingApprovedVacationGlobal — regra do PDF:
        // ka podi ter dois colaboradores aprovados no mesmo período, em toda a empresa
        // -------------------------------------------------------------------------

        @Test
        void existsOverlappingApprovedVacationGlobal_trueWhenAnotherUserHasApprovedOverlap() {
                VacationRequest other = persistVacation(collaboratorB,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                VacationRequest ownRequest = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15), VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationGlobal(
                                ownRequest.getId(), ownRequest.getStartDate(), ownRequest.getEndDate());

                assertThat(result).isTrue();
        }

        @Test
        void existsOverlappingApprovedVacationGlobal_falseWhenOnlyOwnRequestOverlaps() {
                // o próprio pedido é excluído do check — ka bloqueia a si mesmo
                VacationRequest ownRequest = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationGlobal(
                                ownRequest.getId(), ownRequest.getStartDate(), ownRequest.getEndDate());

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingApprovedVacationGlobal_falseWhenOtherUserHasPendingOnly() {
                // pending ka bloqueia aprovação — só APPROVED conta aqui
                persistVacation(collaboratorB,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.PENDING);
                VacationRequest ownRequest = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15), VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationGlobal(
                                ownRequest.getId(), ownRequest.getStartDate(), ownRequest.getEndDate());

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingApprovedVacationGlobal_falseWhenNoOverlap() {
                VacationRequest other = persistVacation(collaboratorB,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                VacationRequest ownRequest = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 11), LocalDate.of(2026, 6, 20), VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationGlobal(
                                ownRequest.getId(), ownRequest.getStartDate(), ownRequest.getEndDate());

                assertThat(result).isFalse();
        }

        // -------------------------------------------------------------------------
        // existsOverlappingApprovedVacationExcludingRequest — usado no update,
        // verifica sobreposição do mesmo user excluindo o próprio pedido
        // -------------------------------------------------------------------------

        @Test
        void existsOverlappingApprovedVacationExcludingRequest_trueWhenOtherApprovedOverlaps() {
                persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                VacationRequest toUpdate = persistVacation(collaboratorA,
                                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationExcludingRequest(
                                toUpdate.getId(), collaboratorA.getId(),
                                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15));

                assertThat(result).isTrue();
        }

        @Test
        void existsOverlappingApprovedVacationExcludingRequest_falseWhenSameRequestExcluded() {
                // o próprio pedido ka deve bloquear a sua própria edição
                VacationRequest request = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationExcludingRequest(
                                request.getId(), collaboratorA.getId(),
                                request.getStartDate(), request.getEndDate());

                assertThat(result).isFalse();
        }

        @Test
        void existsOverlappingApprovedVacationExcludingRequest_falseWhenOtherUserApprovedOverlaps() {
                // check é por userId — sobreposição de outro user ka importa aqui
                persistVacation(collaboratorB,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.APPROVED);
                VacationRequest toUpdate = persistVacation(collaboratorA,
                                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), VacationStatus.PENDING);
                em.flush();

                boolean result = repository.existsOverlappingApprovedVacationExcludingRequest(
                                toUpdate.getId(), collaboratorA.getId(),
                                LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 15));

                assertThat(result).isFalse();
        }

        // -------------------------------------------------------------------------
        // findByUserManagerIdOrUserId — manager vê as suas ferias + as dos seus
        // colaboradores
        // -------------------------------------------------------------------------

        @Test
        void findByUserManagerIdOrUserId_includesManagersOwnVacations() {
                VacationRequest managerVacation = persistVacation(manager,
                                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 5), VacationStatus.PENDING);
                em.flush();

                List<VacationRequest> result = repository.findByUserManagerIdOrUserId(manager.getId(), manager.getId());

                assertThat(result).extracting(VacationRequest::getId).contains(managerVacation.getId());
        }

        @Test
        void findByUserManagerIdOrUserId_includesCollaboratorsVacations() {
                VacationRequest vacA = persistVacation(collaboratorA,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.PENDING);
                VacationRequest vacB = persistVacation(collaboratorB,
                                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5), VacationStatus.APPROVED);
                em.flush();

                List<VacationRequest> result = repository.findByUserManagerIdOrUserId(manager.getId(), manager.getId());

                assertThat(result).extracting(VacationRequest::getId)
                                .containsExactlyInAnyOrder(vacA.getId(), vacB.getId());
        }

        @Test
        void findByUserManagerIdOrUserId_excludesOtherManagersCollaborators() {
                // manager ka deve ver colaboradores de outro manager — cada um no seu quintal
                User otherManager = persistUser("Other Manager", "other@test.com", Role.MANAGER, null);
                User outsider = persistUser("Carlos", "carlos@test.com", Role.COLLABORATOR, otherManager);
                VacationRequest outsiderVacation = persistVacation(outsider,
                                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10), VacationStatus.PENDING);
                em.flush();

                List<VacationRequest> result = repository.findByUserManagerIdOrUserId(manager.getId(), manager.getId());

                assertThat(result).extracting(VacationRequest::getId).doesNotContain(outsiderVacation.getId());
        }
}
