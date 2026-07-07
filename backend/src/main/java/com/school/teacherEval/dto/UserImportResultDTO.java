package com.school.teacherEval.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserImportResultDTO {
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
    private List<String> errors = new ArrayList<>();

    public void created() {
        createdCount++;
    }

    public void updated() {
        updatedCount++;
    }

    public void skipped(String message) {
        skippedCount++;
        errors.add(message);
    }
}
