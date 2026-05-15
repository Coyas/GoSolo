package pt.com.taskflow.gosolo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pt.com.taskflow.gosolo.exceptions.ForbiddenException;
import pt.com.taskflow.gosolo.models.Role;
import pt.com.taskflow.gosolo.models.User;
import pt.com.taskflow.gosolo.models.dto.VacationRequestRequest;
import pt.com.taskflow.gosolo.models.dto.VacationRequestResponse;
import pt.com.taskflow.gosolo.services.UserService;
import pt.com.taskflow.gosolo.services.VacationRequestService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vacation-requests")
@Tag(name = "Vacation Requests")
@SecurityRequirement(name = "bearerAuth")
public class VacationRequestController {

    private final VacationRequestService vacationRequestService;
    private final UserService userService;

    public VacationRequestController(VacationRequestService vacationRequestService, UserService userService) {
        this.vacationRequestService = vacationRequestService;
        this.userService = userService;
    }

    @Operation(summary = "Listar pedidos de férias")
    @GetMapping
    public ResponseEntity<List<VacationRequestResponse>> findAll(Authentication auth) {
        User currentUser = currentUser(auth);
        List<VacationRequestResponse> requests = switch (currentUser.getRole()) {
            case ADMIN -> vacationRequestService.findAll();
            case MANAGER -> vacationRequestService.findByManagerId(currentUser.getId());
            case COLLABORATOR -> vacationRequestService.findByUserId(currentUser.getId());
        };
        return ResponseEntity.ok(requests);
    }

    @Operation(summary = "Obter pedido por ID")
    @GetMapping("/{id}")
    public ResponseEntity<VacationRequestResponse> findById(@PathVariable Long id, Authentication auth) {
        User currentUser = currentUser(auth);
        VacationRequestResponse request = vacationRequestService.findById(id);
        validateReadAccess(currentUser, request);
        return ResponseEntity.ok(request);
    }

    @Operation(summary = "Criar pedido de férias")
    @PostMapping
    public ResponseEntity<VacationRequestResponse> create(@Valid @RequestBody VacationRequestRequest body,
            Authentication auth) {
        User currentUser = currentUser(auth);
        if (currentUser.getRole() != Role.ADMIN) {
            body.setUserId(currentUser.getId());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(vacationRequestService.create(body));
    }

    @Operation(summary = "Actualizar pedido de férias", description = "Apenas o próprio utilizador pode editar o seu pedido. Só é possível editar pedidos com estado PENDING.")
    @PutMapping("/{id}")
    public ResponseEntity<VacationRequestResponse> update(@PathVariable Long id,
            @Valid @RequestBody VacationRequestRequest body,
            Authentication auth) {
        User currentUser = currentUser(auth);
        VacationRequestResponse existing = vacationRequestService.findById(id);
        if (!existing.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(vacationRequestService.update(id, body));
    }

    @Operation(summary = "Cancelar pedido de férias")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication auth) {
        User currentUser = currentUser(auth);
        VacationRequestResponse existing = vacationRequestService.findById(id);
        validateWriteAccess(currentUser, existing);
        vacationRequestService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Aprovar pedido de férias")
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VacationRequestResponse> approve(@PathVariable Long id, Authentication auth) {
        User currentUser = currentUser(auth);
        return ResponseEntity.ok(vacationRequestService.approve(id, currentUser.getId()));
    }

    @Operation(summary = "Rejeitar pedido de férias")
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VacationRequestResponse> reject(@PathVariable Long id, Authentication auth) {
        User currentUser = currentUser(auth);
        return ResponseEntity.ok(vacationRequestService.reject(id, currentUser.getId()));
    }

    private User currentUser(Authentication auth) {
        return userService.findByEmail(auth.getName());
    }

    private void validateReadAccess(User currentUser, VacationRequestResponse request) {
        if (currentUser.getRole() == Role.ADMIN)
            return;
        if (currentUser.getRole() == Role.MANAGER) {
            boolean isOwn = request.getUserId().equals(currentUser.getId());
            boolean isCollaborator = request.getUserManagerId() != null &&
                    request.getUserManagerId().equals(currentUser.getId());
            if (!isOwn && !isCollaborator)
                throw new ForbiddenException("Access denied");
            return;
        }
        if (!request.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private void validateWriteAccess(User currentUser, VacationRequestResponse request) {
        if (currentUser.getRole() == Role.ADMIN)
            return;
        if (!request.getUserId().equals(currentUser.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }
}
