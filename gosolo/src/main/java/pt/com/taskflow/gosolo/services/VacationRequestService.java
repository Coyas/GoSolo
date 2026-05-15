package pt.com.taskflow.gosolo.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDateTime;
import java.util.List;

/*
 * Serviço central do sistema — aqui está toda a lógica de negócio
 * relacionada com pedidos de férias: criar, aprovar, rejeitar, cancelar.
 * O mapeamento para DTO é feito dentro de @Transactional para evitar
 * LazyInitializationException nos relacionamentos LAZY do JPA.
 */
@Service
@Transactional
public class VacationRequestService {

    private final VacationRequestRepository vacationRequestRepository;
    private final UserService userService;

    public VacationRequestService(VacationRequestRepository vacationRequestRepository, UserService userService) {
        this.vacationRequestRepository = vacationRequestRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<VacationRequestResponse> findAll() {
        return vacationRequestRepository.findAll().stream()
                .map(VacationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VacationRequestResponse> findByUserId(Long userId) {
        return vacationRequestRepository.findByUserId(userId).stream()
                .map(VacationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VacationRequestResponse> findByManagerId(Long managerId) {
        // manager vê as suas próprias férias e as dos seus colaboradores (OR na query)
        return vacationRequestRepository.findByUserManagerIdOrUserId(managerId, managerId).stream()
                .map(VacationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VacationRequestResponse findById(Long id) {
        return VacationRequestResponse.from(findEntity(id));
    }

    public VacationRequestResponse create(VacationRequestRequest request) {
        // PENDING e APPROVED contam como sobreposição — só REJECTED é ignorado pelo
        // sistema
        if (vacationRequestRepository.existsOverlappingActiveVacation(
                request.getUserId(), request.getStartDate(), request.getEndDate())) {
            throw new BusinessException("Já existe um pedido de férias para este período");
        }
        VacationRequest vacation = new VacationRequest();
        vacation.setUser(userService.findById(request.getUserId()));
        vacation.setStartDate(request.getStartDate());
        vacation.setEndDate(request.getEndDate());
        return VacationRequestResponse.from(vacationRequestRepository.save(vacation));
    }

    public VacationRequestResponse update(Long id, VacationRequestRequest updated) {
        VacationRequest existing = findEntity(id);
        if (existing.getStatus() != VacationStatus.PENDING) {
            throw new BusinessException("Só é possível alterar pedidos pendentes");
        }
        if (vacationRequestRepository.existsOverlappingApprovedVacationExcludingRequest(
                existing.getId(), existing.getUser().getId(), updated.getStartDate(), updated.getEndDate())) {
            throw new BusinessException("As datas coincidem com um pedido já aprovado");
        }
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        return VacationRequestResponse.from(vacationRequestRepository.save(existing));
    }

    public VacationRequestResponse approve(Long id, Long reviewerId) {
        VacationRequest request = findEntity(id);
        User reviewer = userService.findById(reviewerId);
        // manager só pode aprovar os seus colaboradores — admin aprova tudo
        if (reviewer.getRole() == Role.MANAGER) {
            validateManagerOwnership(reviewer, request);
        }
        if (request.getStatus() != VacationStatus.PENDING) {
            throw new BusinessException("Só é possível aprovar pedidos pendentes");
        }
        // regra global do PDF: não pode ter dois colaboradores aprovados no mesmo
        // período
        if (vacationRequestRepository.existsOverlappingApprovedVacationGlobal(
                request.getId(), request.getStartDate(), request.getEndDate())) {
            throw new BusinessException("Já existe um colaborador de férias neste período");
        }
        request.setStatus(VacationStatus.APPROVED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        return VacationRequestResponse.from(vacationRequestRepository.save(request));
    }

    public VacationRequestResponse reject(Long id, Long reviewerId) {
        VacationRequest request = findEntity(id);
        User reviewer = userService.findById(reviewerId);
        if (reviewer.getRole() == Role.MANAGER) {
            validateManagerOwnership(reviewer, request);
        }
        if (request.getStatus() != VacationStatus.PENDING) {
            throw new BusinessException("Só é possível rejeitar pedidos pendentes");
        }
        request.setStatus(VacationStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        return VacationRequestResponse.from(vacationRequestRepository.save(request));
    }

    public void cancel(Long id) {
        VacationRequest request = findEntity(id);
        if (request.getStatus() == VacationStatus.APPROVED) {
            throw new BusinessException("Não é possível cancelar pedidos aprovados");
        }
        vacationRequestRepository.delete(request);
    }

    private VacationRequest findEntity(Long id) {
        return vacationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido de férias não encontrado: " + id));
    }

    /*
     * Garante que o manager só gere os seus próprios colaboradores.
     * Se o colaborador ka tem manager ou o manager é diferente — forbidden.
     */
    private void validateManagerOwnership(User manager, VacationRequest request) {
        User vacationUser = request.getUser();
        if (vacationUser.getManager() == null || !vacationUser.getManager().getId().equals(manager.getId())) {
            throw new ForbiddenException("Manager só pode aprovar/rejeitar férias dos seus colaboradores directos");
        }
    }
}
