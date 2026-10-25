
package com.securevault.securevault.dto;

public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String role;
    private boolean emailVerified;

    public UserResponse() {
    }

    public UserResponse(
            Long id,
            String fullName,
            String email,
            String role,
            boolean emailVerified) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.emailVerified = emailVerified;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}
