USE teacher_eval;

-- 插入C级活动
INSERT INTO activities (period_id, name, level, description, max_participants, status, enrollment_start, enrollment_end, has_exam, exam_duration_minutes, created_at, updated_at) 
SELECT p.id, 'C级考核', 'C', '2024学年第一学期C级教师考核', 50, 'active', NOW() - INTERVAL 7 DAY, NOW() + INTERVAL 30 DAY, 1, 60, NOW(), NOW() 
FROM evaluation_periods p WHERE p.status='active' LIMIT 1;

-- 插入B2级活动
INSERT INTO activities (period_id, name, level, description, max_participants, status, enrollment_start, enrollment_end, created_at, updated_at) 
SELECT p.id, 'B2级考核', 'B2', '2024学年第一学期B2级教师考核', 30, 'active', NOW() - INTERVAL 7 DAY, NOW() + INTERVAL 30 DAY, NOW(), NOW() 
FROM evaluation_periods p WHERE p.status='active' LIMIT 1;

-- 为已报名的教师初始化报名记录
INSERT INTO period_enrollments (period_id, activity_id, teacher_id, status, enrolled_at)
SELECT p.id, a.id, u.id, 'enrolled', NOW()
FROM evaluation_periods p
JOIN activities a ON a.period_id = p.id AND a.level = 'C'
JOIN users u ON u.role = 'teacher' AND u.status = 1
WHERE p.status = 'active';

-- 为C级活动创建试卷
INSERT INTO exam_papers (period_id, name, description, total_score, duration_minutes, question_count, status, created_by, created_at, updated_at)
SELECT p.id, '2024学年第一学期C级考试', 'C级教师业务能力测试', 100, 60, 15, 'active', 1, NOW(), NOW()
FROM evaluation_periods p WHERE p.status='active' LIMIT 1;

-- 更新C级活动绑定试卷
UPDATE activities a 
SET a.exam_paper_id = (SELECT id FROM exam_papers WHERE period_id = a.period_id AND name LIKE '%C级%' LIMIT 1),
    a.has_exam = 1,
    a.exam_duration_minutes = 60
WHERE a.level = 'C';

-- 初始化题库
INSERT INTO exam_questions (period_id, question_text, question_type, options, correct_answer, score, explanation, difficulty, status, created_by, created_at, updated_at)
SELECT p.id, 
       CONCAT('C级单选题', n.n, '：教学设计的核心是以下哪个？'),
       'single',
       '[{"id":"A","text":"教材内容"},{"id":"B","text":"教师讲授"},{"id":"C","text":"学生学习"},{"id":"D","text":"教学环境"}]',
       CASE WHEN n.n % 4 = 1 THEN 'A' WHEN n.n % 4 = 2 THEN 'B' WHEN n.n % 4 = 3 THEN 'C' ELSE 'D' END,
       5, '教学设计应以学生的学习为中心', 1, 1, 1, NOW(), NOW()
FROM evaluation_periods p
CROSS JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n
WHERE p.status='active';

INSERT INTO exam_questions (period_id, question_text, question_type, options, correct_answer, score, explanation, difficulty, status, created_by, created_at, updated_at)
SELECT p.id,
       CONCAT('C级多选题', n.n, '：以下哪些是有效的教学策略？（多选）'),
       'multiple',
       '[{"id":"A","text":"讲授法"},{"id":"B","text":"讨论法"},{"id":"C","text":"演示法"},{"id":"D","text":"练习法"},{"id":"E","text":"提问法"},{"id":"F","text":"游戏法"}]',
       'ABCDEF', 10, '有效的教学策略包括多种方法', 2, 1, 1, NOW(), NOW()
FROM evaluation_periods p
CROSS JOIN (SELECT 1 n UNION SELECT 2 UNION SELECT 3) n
WHERE p.status='active';

-- 关联试卷题目
INSERT INTO paper_questions (paper_id, question_id, question_order)
SELECT ep.id, eq.id, eq.id
FROM exam_papers ep
JOIN exam_questions eq ON eq.period_id = ep.period_id
WHERE ep.name LIKE '%C级%';

SELECT 'Data initialized successfully!' as result;