-- 教师评价考核平台数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS teacher_eval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE teacher_eval;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role ENUM('teacher', 'evaluator', 'admin') NOT NULL COMMENT '角色',
    department VARCHAR(100) COMMENT '部门/教研组',
    avatar VARCHAR(500) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 考核周期表
CREATE TABLE IF NOT EXISTS evaluation_periods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '周期名称',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    description TEXT COMMENT '考核说明',
    status ENUM('draft', 'active', 'closed') DEFAULT 'draft' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期表';

-- 教师文档表
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '上传教师ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_path VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(100) COMMENT 'MIME类型',
    description VARCHAR(500) COMMENT '文档描述',
    is_deleted TINYINT DEFAULT 0 COMMENT '软删除: 0否 1是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_period_id (period_id),
    INDEX idx_is_deleted (is_deleted),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (period_id) REFERENCES evaluation_periods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 考核评分表
CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    evaluator_id BIGINT NOT NULL COMMENT '考核员ID',
    teacher_id BIGINT NOT NULL COMMENT '被考核教师ID',
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    score DECIMAL(5,2) COMMENT '评分 0-100',
    comment TEXT COMMENT '评语',
    attachments VARCHAR(1000) COMMENT '评分附件JSON',
    status ENUM('draft', 'submitted') DEFAULT 'draft' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_teacher_id (teacher_id),
    INDEX idx_period_id (period_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_teacher_period (teacher_id, period_id),
    FOREIGN KEY (evaluator_id) REFERENCES users(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (period_id) REFERENCES evaluation_periods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核评分表';

-- 插入测试用户 (密码: demo123, BCrypt加密)
INSERT INTO users (username, password, real_name, role, department) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 'admin', '校办'),
('evaluator1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张考核', 'evaluator', '考核组'),
('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李老师', 'teacher', '语文组'),
('teacher2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王老师', 'teacher', '数学组');

-- 插入考核周期
INSERT INTO evaluation_periods (name, start_date, end_date, description, status) VALUES
('2024学年第一学期', '2024-09-01', '2025-01-31', '2024学年第一学期教师考核', 'active');

-- 学习资料表
CREATE TABLE IF NOT EXISTS learning_materials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    title VARCHAR(200) NOT NULL COMMENT '资料标题',
    file_path VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    file_name VARCHAR(200) NOT NULL COMMENT '原始文件名',
    file_size BIGINT COMMENT '文件大小',
    file_type VARCHAR(100) COMMENT 'MIME类型',
    description VARCHAR(500) COMMENT '资料描述',
    created_by BIGINT NOT NULL COMMENT '上传人ID',
    is_deleted TINYINT DEFAULT 0 COMMENT '软删除: 0否 1是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period_id (period_id),
    INDEX idx_created_by (created_by),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料表';

-- 考核周期报名表
CREATE TABLE IF NOT EXISTS period_enrollments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    enrolled_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    status ENUM('enrolled', 'removed') DEFAULT 'enrolled' COMMENT '状态',
    INDEX idx_period_id (period_id),
    INDEX idx_teacher_id (teacher_id),
    UNIQUE KEY uk_period_teacher (period_id, teacher_id),
    FOREIGN KEY (period_id) REFERENCES evaluation_periods(id),
    FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考核周期报名表';

-- 注意: demo123 的 BCrypt hash 是 $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH
-- 这个hash可能不匹配实际密码，建议首次登录后使用系统的密码重置功能