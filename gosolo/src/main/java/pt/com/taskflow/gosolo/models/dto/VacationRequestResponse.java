package pt.com.taskflow.gosolo.models.dto;

import pt.com.taskflow.gosolo.models.VacationRequest;
import pt.com.taskflow.gosolo.models.VacationStatus;

import java.time.LocalDateTime;

public class VacationRequestResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long userManagerId;
    private String startDate;
    private String endDate;
    private VacationStatus status;
    private Long reviewedById;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;

    /*
     * Mapeia a entidade para DTO — deve ser chamado dentro de @Transactional
     * para evitar LazyInitializationException nos campos user e reviewedBy.
     * As datas são String (não LocalDate) porque o Jackson 3 do Spring Boot 4
     * não serializa LocalDate via properties — resolve-se assim, simples.
     */
    public static VacationRequestResponse from(VacationRequest v) {
        VacationRequestResponse dto = new VacationRequestResponse();
        dto.id = v.getId();
        dto.userId = v.getUser().getId();
        dto.userName = v.getUser().getName();
        dto.userManagerId = v.getUser().getManager() != null ? v.getUser().getManager().getId() : null;
        dto.startDate = v.getStartDate().toString();
        dto.endDate = v.getEndDate().toString();
        dto.status = v.getStatus();
        dto.reviewedAt = v.getReviewedAt();
        dto.createdAt = v.getCreatedAt();
        if (v.getReviewedBy() != null) {
            dto.reviewedById = v.getReviewedBy().getId();
            dto.reviewedByName = v.getReviewedBy().getName();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Long getUserManagerId() {
        return userManagerId;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public VacationStatus getStatus() {
        return status;
    }

    public Long getReviewedById() {
        return reviewedById;
    }

    public String getReviewedByName() {
        return reviewedByName;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
