package com.sep.vox.domain.model.student;

import java.util.UUID;

import com.sep.vox.domain.valueobject.id.UserId;

public class StudentProfile {
    private UUID id;
    private String studentId;
    private UserId userId;

    public StudentProfile() {}

    public StudentProfile(UUID id, String studentId, UserId userId) {
        this.id = id;
        this.studentId = studentId;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    
}
