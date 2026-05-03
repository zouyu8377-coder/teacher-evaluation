package com.school.teacherEval.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUserVO {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String department;
}
