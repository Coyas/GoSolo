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
        return vacationRequestRepository.findByUserManagerIdOrUserId(managerId, managerId).stream()
                .map(VacationRequestResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VacationRequestResponse findById(Long id) {
        return VacationRequestResponse.from(findEntity(id));
    }

    public VacationRequestResponse create(VacationRequestRequest request) {
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
            throw new BusinessException("Only pending requests can be updated");
        }
        if (vacationRequestRepository.existsOverlappingApprovedVacationExcludingRequest(
                existing.getId(), existing.getUser().getId(), updated.getStartDate(), updated.getEndDate())) {
            throw new BusinessException("Vacation dates overlap with an approved request");
        }
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        return VacationRequestResponse.from(vacationRequestRepository.save(existing));
    }

    public VacationRequestResponse approve(Long id, Long reviewerId) {
        VacationRequest request = findEntity(id);
        User reviewer = userService.findById(reviewerId);
        if (reviewer.getRole() == Role.MANAGER) {
            validateManagerOwnership(reviewer, request);
        }
        if (request.getStatus() != VacationStatus.PENDING) {
            throw new BusinessException("Only pending requests can be approved");
        }
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
            throw new BusinessException("Only pending requests can be rejected");
        }
        request.setStatus(VacationStatus.REJECTED);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        return VacationRequestResponse.from(vacationRequestRepository.save(request));
    }

    public void cancel(Long id) {
        VacationRequest request = findEntity(id);
        if (request.getStatus() == VacationStatus.APPROVED) {
            throw new BusinessException("Approved requests cannot be cancelled");
        }
        vacationRequestRepository.delete(request);
    }

    private VacationRequest findEntity(Long id) {
        return vacationRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacation request not found: " + id));
    }

    private void validateManagerOwnership(User manager, VacationRequest request) {
        User vacationUser = request.getUser();
        if (vacationUser.getManager() == null || !vacationUser.getManager().getId().equals(manager.getId())) {
            throw new ForbiddenException("Manager can only approve/reject vacations of their own collaborators");
        }
    }
}
