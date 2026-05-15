package pt.com.taskflow.gosolo.models.dto;

import pt.com.taskflow.gosolo.models.Role;
import pt.com.taskflow.gosolo.models.User;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private Long managerId;
    private String managerName;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.name = user.getName();
        dto.email = user.getEmail();
        dto.role = user.getRole();
        dto.createdAt = user.getCreatedAt();
        if (user.getManager() != null) {
            dto.managerId = user.getManager().getId();
            dto.managerName = user.getManager().getName();
        }
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public Long getManagerId() {
        return managerId;
    }

    public String getManagerName() {
        return managerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
