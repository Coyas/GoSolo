package pt.com.taskflow.gosolo.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.com.taskflow.gosolo.exceptions.BusinessException;
import pt.com.taskflow.gosolo.exceptions.ForbiddenException;
import pt.com.taskflow.gosolo.exceptions.ResourceNotFoundException;
import pt.com.taskflow.gosolo.models.Role;
import pt.com.taskflow.gosolo.models.User;
import pt.com.taskflow.gosolo.models.VacationRequest;
import pt.com.taskflow.gosolo.models.VacationStatus;
import pt.com.taskflow.gosolo.models.dto.VacationRequestRequest;
import pt.com.taskflow.gosolo.models.dto.VacationRequestResponse;
import pt.com.taskflow.gosolo.repositories.VacationRequestRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * Testes unitários do VacationRequestService.
 * Ka precisa de base de dados — tudu é mockado com Mockito,
 * so testamos a lógica de negócio, nada mais.
 */
@ExtendWith(MockitoExtension.class)
class VacationRequestServiceTest {

    @Mock
    private VacationRequestRepository vacationRequestRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private VacationRequestService vacationRequestService;

    // helpers pa montar os objetos de teste rapidamente
    private User makeUser(Long id, String name, Role role, User manager) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
        u.setPassword("hashed");
        u.setRole(role);
        u.setManager(manager);
        return u;
    }

    private VacationRequest makeVacation(Long id, User user, VacationStatus status) {
        VacationRequest v = new VacationRequest();
        v.setId(id);
        v.setUser(user);
        v.setStartDate(LocalDate.of(2026, 6, 1));
        v.setEndDate(LocalDate.of(2026, 6, 10));
        v.setStatus(status);
        return v;
    }

    private VacationRequestRequest makeRequest(Long userId, LocalDate start, LocalDate end) {
        VacationRequestRequest r = new VacationRequestRequest();
        r.setUserId(userId);
        r.setStartDate(start);
        r.setEndDate(end);
        return r;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_throwsWhenOverlappingActiveVacation() {
        // ka podi criar se ja tem ferias no mesmo período
        VacationRequestRequest req = makeRequest(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));
        when(vacationRequestRepository.existsOverlappingActiveVacation(1L, req.getStartDate(), req.getEndDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> vacationRequestService.create(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um pedido");
    }

    @Test
    void create_succeeds() {
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequestRequest req = makeRequest(1L, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10));

        when(vacationRequestRepository.existsOverlappingActiveVacation(any(), any(), any())).thenReturn(false);
        when(userService.findById(1L)).thenReturn(collaborator);
        when(vacationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VacationRequestResponse result = vacationRequestService.create(req);

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(VacationStatus.PENDING);
        verify(vacationRequestRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // approve — regras mais txeu complicadas do sistema
    // -------------------------------------------------------------------------

    @Test
    void approve_throwsWhenNotPending() {
        // só pedidos pendentes podem ser aprovados
        User admin = makeUser(10L, "Admin", Role.ADMIN, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.APPROVED);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(10L)).thenReturn(admin);

        assertThatThrownBy(() -> vacationRequestService.approve(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Só é possível aprovar pedidos pendentes");
    }

    @Test
    void approve_throwsWhenGlobalOverlapExists() {
        // regra do PDF: ka podi ter dois colaboradores de férias no mesmo período
        User admin = makeUser(10L, "Admin", Role.ADMIN, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(10L)).thenReturn(admin);
        when(vacationRequestRepository.existsOverlappingApprovedVacationGlobal(
                100L, vacation.getStartDate(), vacation.getEndDate())).thenReturn(true);

        assertThatThrownBy(() -> vacationRequestService.approve(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um colaborador");
    }

    @Test
    void approve_throwsWhenManagerApprovesOtherManagersCollaborator() {
        // manager ka tem direito de aprovar colaboradores que não são seus
        User otherManager = makeUser(20L, "Other Manager", Role.MANAGER, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, otherManager);
        User reviewer = makeUser(30L, "My Manager", Role.MANAGER, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(30L)).thenReturn(reviewer);

        assertThatThrownBy(() -> vacationRequestService.approve(100L, 30L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void approve_succeedsForAdmin() {
        // admin manda em tudu, pode aprovar qualquer pedido
        User admin = makeUser(10L, "Admin", Role.ADMIN, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(10L)).thenReturn(admin);
        when(vacationRequestRepository.existsOverlappingApprovedVacationGlobal(any(), any(), any())).thenReturn(false);
        when(vacationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VacationRequestResponse result = vacationRequestService.approve(100L, 10L);

        assertThat(result.getStatus()).isEqualTo(VacationStatus.APPROVED);
    }

    @Test
    void approve_succeedsWhenManagerOwnsCollaborator() {
        User manager = makeUser(30L, "Manager", Role.MANAGER, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, manager);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(30L)).thenReturn(manager);
        when(vacationRequestRepository.existsOverlappingApprovedVacationGlobal(any(), any(), any())).thenReturn(false);
        when(vacationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VacationRequestResponse result = vacationRequestService.approve(100L, 30L);

        assertThat(result.getStatus()).isEqualTo(VacationStatus.APPROVED);
    }

    // -------------------------------------------------------------------------
    // reject
    // -------------------------------------------------------------------------

    @Test
    void reject_throwsWhenNotPending() {
        User admin = makeUser(10L, "Admin", Role.ADMIN, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.REJECTED);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(10L)).thenReturn(admin);

        assertThatThrownBy(() -> vacationRequestService.reject(100L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Só é possível rejeitar pedidos pendentes");
    }

    @Test
    void reject_succeedsForManager() {
        User manager = makeUser(30L, "Manager", Role.MANAGER, null);
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, manager);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(userService.findById(30L)).thenReturn(manager);
        when(vacationRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VacationRequestResponse result = vacationRequestService.reject(100L, 30L);

        assertThat(result.getStatus()).isEqualTo(VacationStatus.REJECTED);
    }

    // -------------------------------------------------------------------------
    // cancel
    // -------------------------------------------------------------------------

    @Test
    void cancel_throwsWhenApproved() {
        // ferias já aprovadas ka pode cancelar — já stá confirmado
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.APPROVED);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));

        assertThatThrownBy(() -> vacationRequestService.cancel(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível cancelar pedidos aprovados");
    }

    @Test
    void cancel_succeedsForPending() {
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));

        vacationRequestService.cancel(100L);

        verify(vacationRequestRepository).delete(vacation);
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_throwsWhenNotPending() {
        // só da pa editar se ainda stá pending
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.APPROVED);
        VacationRequestRequest req = makeRequest(null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10));

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));

        assertThatThrownBy(() -> vacationRequestService.update(100L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Só é possível alterar pedidos pendentes");
    }

    @Test
    void update_throwsWhenOverlapWithApproved() {
        User collaborator = makeUser(1L, "Ana", Role.COLLABORATOR, null);
        VacationRequest vacation = makeVacation(100L, collaborator, VacationStatus.PENDING);
        VacationRequestRequest req = makeRequest(null, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10));

        when(vacationRequestRepository.findById(100L)).thenReturn(Optional.of(vacation));
        when(vacationRequestRepository.existsOverlappingApprovedVacationExcludingRequest(
                100L, 1L, req.getStartDate(), req.getEndDate())).thenReturn(true);

        assertThatThrownBy(() -> vacationRequestService.update(100L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("coincidem");
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_throwsWhenNotFound() {
        when(vacationRequestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vacationRequestService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
