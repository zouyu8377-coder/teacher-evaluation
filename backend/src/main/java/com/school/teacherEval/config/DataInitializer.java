package com.school.teacherEval.config;

import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    private final PasswordEncoder passwordEncoder;
    
    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            // 检查是否已有管理员用户
            if (userRepository.count() == 0) {
                // 创建测试用户
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRealName("系统管理员");
                admin.setRole(User.Role.admin);
                admin.setDepartment("校办");
                admin.setStatus(1);
                
                User evaluator1 = new User();
                evaluator1.setUsername("evaluator1");
                evaluator1.setPassword(passwordEncoder.encode("eval123"));
                evaluator1.setRealName("张考核");
                evaluator1.setRole(User.Role.evaluator);
                evaluator1.setDepartment("考核组");
                evaluator1.setStatus(1);
                
                User teacher1 = new User();
                teacher1.setUsername("teacher1");
                teacher1.setPassword(passwordEncoder.encode("teacher123"));
                teacher1.setRealName("李老师");
                teacher1.setRole(User.Role.teacher);
                teacher1.setDepartment("语文组");
                teacher1.setStatus(1);
                
                User teacher2 = new User();
                teacher2.setUsername("teacher2");
                teacher2.setPassword(passwordEncoder.encode("teacher123"));
                teacher2.setRealName("王老师");
                teacher2.setRole(User.Role.teacher);
                teacher2.setDepartment("数学组");
                teacher2.setStatus(1);
                
                userRepository.save(admin);
                userRepository.save(evaluator1);
                userRepository.save(teacher1);
                userRepository.save(teacher2);
                
                System.out.println("测试用户数据初始化完成！");
                System.out.println("可用账号：");
                System.out.println("1. 管理员：admin / admin123");
                System.out.println("2. 考核员：evaluator1 / eval123");
                System.out.println("3. 教师：teacher1 / teacher123");
                System.out.println("4. 教师：teacher2 / teacher123");
            }
        };
    }
}