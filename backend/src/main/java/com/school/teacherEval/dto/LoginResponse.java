package com.school.teacherEval.dto;

import com.school.teacherEval.entity.User;
import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserVO user;
    
    @Data
    public static class UserVO {
        private Long id;
        private String username;
        private String realName;
        private String role;
        private String department;
        
        public static UserVO from(User user) {
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setRole(user.getRole().name());
            vo.setDepartment(user.getDepartment());
            return vo;
        }
    }
}