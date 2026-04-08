-- C级考试系统数据库表

-- 1. 题库表
CREATE TABLE IF NOT EXISTS exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id BIGINT NOT NULL COMMENT '考核周期ID',
    question_text TEXT NOT NULL COMMENT '题目内容',
    question_type ENUM('single', 'multiple') NOT NULL COMMENT '题型:单选/多选',
    options JSON NOT NULL COMMENT '选项 [{"id":"A","text":"选项内容"},{"id":"B",...}]',
    correct_answer VARCHAR(20) NOT NULL COMMENT '正确答案: A/B/C/D 或 ABCD',
    score INT DEFAULT 5 COMMENT '分值',
    explanation TEXT COMMENT '答案解析',
    difficulty INT DEFAULT 1 COMMENT '难度1-5',
    status TINYINT DEFAULT 1 COMMENT '状态:1启用0禁用',
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period_id (period_id),
    INDEX idx_type (question_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题库表';

-- 2. 试卷表
CREATE TABLE IF NOT EXISTS exam_papers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    period_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL COMMENT '试卷名称',
    description VARCHAR(500) COMMENT '试卷说明',
    total_score INT DEFAULT 100 COMMENT '总分',
    duration_minutes INT DEFAULT 60 COMMENT '考试时长(分钟)',
    question_count INT DEFAULT 20 COMMENT '题目数量',
    status ENUM('draft', 'active', 'closed') DEFAULT 'draft',
    created_by BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_period_id (period_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 3. 试卷题目关联表
CREATE TABLE IF NOT EXISTS paper_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_order INT NOT NULL COMMENT '题号(1,2,3...)',
    FOREIGN KEY (paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES exam_questions(id),
    UNIQUE KEY uk_paper_order (paper_id, question_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- 4. 考试记录表
CREATE TABLE IF NOT EXISTS exam_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    answers JSON COMMENT '作答记录 {"1":"A","2":"BC",...}',
    score DECIMAL(5,2) COMMENT '最终得分',
    auto_score DECIMAL(5,2) COMMENT '自动判分',
    manual_adjust DECIMAL(5,2) DEFAULT 0 COMMENT '人工调整',
    correct_count INT DEFAULT 0 COMMENT '正确题数',
    wrong_count INT DEFAULT 0 COMMENT '错误题数',
    status ENUM('not_started', 'in_progress', 'submitted') DEFAULT 'not_started',
    started_at DATETIME COMMENT '开始时间',
    submitted_at DATETIME COMMENT '提交时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_teacher_activity (teacher_id, activity_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- 5. Activity表增强
ALTER TABLE activities ADD COLUMN exam_paper_id BIGINT COMMENT '关联试卷ID';
ALTER TABLE activities ADD COLUMN has_exam TINYINT DEFAULT 0 COMMENT '是否有考试';
ALTER TABLE activities ADD COLUMN exam_duration_minutes INT DEFAULT 60 COMMENT '考试时长';

-- 6. Evaluation表增强
ALTER TABLE evaluations ADD COLUMN exam_record_id BIGINT COMMENT '关联考试记录ID';
ALTER TABLE evaluations ADD COLUMN auto_score DECIMAL(5,2) COMMENT '自动判分';
ALTER TABLE evaluations ADD COLUMN manual_adjust DECIMAL(5,2) DEFAULT 0 COMMENT '人工调整';