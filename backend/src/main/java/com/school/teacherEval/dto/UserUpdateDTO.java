package com.school.teacherEval.dto;

import com.school.teacherEval.entity.User;
import lombok.Data;

@Data
public class UserUpdateDTO {

    private String realName;

    private User.Role role;

    private String department;

    private Integer status;

    /** 传空或 null 表示不修改密码 */
    private String password;
}
