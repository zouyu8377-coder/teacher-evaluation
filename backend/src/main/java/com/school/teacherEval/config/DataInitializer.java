package com.school.teacherEval.config;

import com.school.teacherEval.entity.*;
import com.school.teacherEval.repository.*;
import com.school.teacherEval.service.TeacherLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {
    
    private final PasswordEncoder passwordEncoder;
    private final TeacherLevelService teacherLevelService;

    @Bean
    @Transactional
    public CommandLineRunner initData(
            UserRepository userRepository,
            ActivityRepository activityRepository,
            ExamQuestionRepository questionRepository,
            ExamPaperRepository paperRepository,
            PaperQuestionRepository paperQuestionRepository,
            EnrollmentRepository enrollmentRepository,
            EvaluationRepository evaluationRepository,
            TeacherLevelHistoryRepository historyRepository) {
        return args -> {
            // 0. 数据迁移：为已有通过评分的教师回填 teacher_level
            migrateTeacherLevels(userRepository, evaluationRepository, activityRepository, historyRepository);

            // 1. 初始化用户
            // 1. 初始化用户
            if (userRepository.count() == 0) {
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

                User teacher3 = new User();
                teacher3.setUsername("teacher3");
                teacher3.setPassword(passwordEncoder.encode("teacher123"));
                teacher3.setRealName("赵老师");
                teacher3.setRole(User.Role.teacher);
                teacher3.setDepartment("英语组");
                teacher3.setStatus(1);
                
                userRepository.saveAll(List.of(admin, evaluator1, teacher1, teacher2, teacher3));
                System.out.println("测试用户数据初始化完成！");
            }
            
            // 2. 修复现有活动的hasExam值（C级=true，其他级别=false）
            List<Activity> allActivities = activityRepository.findAll();
            for (Activity activity : allActivities) {
                boolean shouldBeExam = activity.getLevel() == Activity.Level.C;
                if (activity.getHasExam() == null || activity.getHasExam() != shouldBeExam) {
                    activity.setHasExam(shouldBeExam);
                    activityRepository.save(activity);
                    System.out.println("修复活动[" + activity.getName() + "] hasExam=" + shouldBeExam);
                }
            }

            // 3. 初始化活动（直接包含周期信息）
            if (activityRepository.count() == 0) {
                List<Activity> activities = new ArrayList<>();
                
                // C级活动
                Activity cActivity = new Activity();
                cActivity.setName("2024学年第一学期C级考核");
                cActivity.setLevel(Activity.Level.C);
                cActivity.setDescription("2024学年第一学期C级教师考核");
                cActivity.setMaxParticipants(50);
                cActivity.setEnrollmentStart(LocalDateTime.now().minusDays(7));
                cActivity.setEnrollmentEnd(LocalDateTime.now().plusDays(30));
                cActivity.setStartDate(LocalDate.of(2024, 9, 1));
                cActivity.setEndDate(LocalDate.of(2025, 1, 31));
                cActivity.setReviewerCount(2);
                cActivity.setReviewerIds("[1]");  // evaluator1
                activities.add(cActivity);

                // B2级活动
                Activity b2Activity = new Activity();
                b2Activity.setName("2024学年第一学期B2级考核");
                b2Activity.setLevel(Activity.Level.B2);
                b2Activity.setDescription("2024学年第一学期B2级教师考核");
                b2Activity.setMaxParticipants(30);
                b2Activity.setEnrollmentStart(LocalDateTime.now().minusDays(7));
                b2Activity.setEnrollmentEnd(LocalDateTime.now().plusDays(30));
                b2Activity.setStartDate(LocalDate.of(2024, 9, 1));
                b2Activity.setEndDate(LocalDate.of(2025, 1, 31));
                b2Activity.setReviewerCount(2);
                b2Activity.setReviewerIds("[1]");
                activities.add(b2Activity);
                
                activityRepository.saveAll(activities);
                System.out.println("默认活动创建完成！");
            }
            
            // 3. 初始化题库（通用）
            if (questionRepository.count() == 0) {
                List<ExamQuestion> questions = new ArrayList<>();
                
                // C级单选题 (10题)
                for (int i = 1; i <= 10; i++) {
                    ExamQuestion q = new ExamQuestion();
                    q.setQuestionText("C级单选题" + i + "：教学设计的核心是以下哪个？");
                    q.setQuestionType(ExamQuestion.QuestionType.single);
                    q.setOptions("[{\"id\":\"A\",\"text\":\"教材内容\"},{\"id\":\"B\",\"text\":\"教师讲授\"},{\"id\":\"C\",\"text\":\"学生学习\"},{\"id\":\"D\",\"text\":\"教学环境\"}]");
                    q.setCorrectAnswer(i % 4 == 1 ? "A" : i % 4 == 2 ? "B" : i % 4 == 3 ? "C" : "D");
                    q.setScore(5);
                    q.setExplanation("教学设计应以学生的学习为中心");
                    q.setDifficulty(1 + (i % 3));
                    q.setCreatedBy(1L);
                    questions.add(q);
                }
                
                // C级多选题 (5题)
                for (int i = 1; i <= 5; i++) {
                    ExamQuestion q = new ExamQuestion();
                    q.setQuestionText("C级多选题" + i + "：以下哪些是有效的教学策略？（多选）");
                    q.setQuestionType(ExamQuestion.QuestionType.multiple);
                    q.setOptions("[{\"id\":\"A\",\"text\":\"讲授法\"},{\"id\":\"B\",\"text\":\"讨论法\"},{\"id\":\"C\",\"text\":\"演示法\"},{\"id\":\"D\",\"text\":\"练习法\"},{\"id\":\"E\",\"text\":\"提问法\"},{\"id\":\"F\",\"text\":\"游戏法\"}]");
                    q.setCorrectAnswer(i == 1 ? "ABC" : i == 2 ? "ABCD" : i == 3 ? "ABCDE" : i == 4 ? "BCDEF" : "ACDEF");
                    q.setScore(10);
                    q.setExplanation("有效的教学策略包括讲授、讨论、演示、练习等多种方法");
                    q.setDifficulty(2);
                    q.setCreatedBy(1L);
                    questions.add(q);
                }
                
                // B2级单选题 (10题)
                for (int i = 1; i <= 10; i++) {
                    ExamQuestion q = new ExamQuestion();
                    q.setQuestionText("B2级单选题" + i + "：关于教学评价，以下说法正确的是？");
                    q.setQuestionType(ExamQuestion.QuestionType.single);
                    q.setOptions("[{\"id\":\"A\",\"text\":\"评价只是为了排名\"},{\"id\":\"B\",\"text\":\"评价是为了促进学生发展\"},{\"id\":\"C\",\"text\":\"评价可以随意进行\"},{\"id\":\"D\",\"text\":\"评价不需要标准\"}]");
                    q.setCorrectAnswer("B");
                    q.setScore(5);
                    q.setExplanation("教学评价应以促进学生发展为目的");
                    q.setDifficulty(2 + (i % 3));
                    q.setCreatedBy(1L);
                    questions.add(q);
                }
                
                // B2级多选题 (5题)
                for (int i = 1; i <= 5; i++) {
                    ExamQuestion q = new ExamQuestion();
                    q.setQuestionText("B2级多选题" + i + "：教学反思的方法包括哪些？（多选）");
                    q.setQuestionType(ExamQuestion.QuestionType.multiple);
                    q.setOptions("[{\"id\":\"A\",\"text\":\"课后小结\"},{\"id\":\"B\",\"text\":\"教学案例分析\"},{\"id\":\"C\",\"text\":\"同行评议\"},{\"id\":\"D\",\"text\":\"学生反馈分析\"},{\"id\":\"E\",\"text\":\"观看教学录像\"},{\"id\":\"F\",\"text\":\"考试成绩分析\"}]");
                    q.setCorrectAnswer("ABCDEF");
                    q.setScore(10);
                    q.setExplanation("教学反思可以通过多种方式进行");
                    q.setDifficulty(3);
                    q.setCreatedBy(1L);
                    questions.add(q);
                }
                
                questionRepository.saveAll(questions);
                System.out.println("题库数据初始化完成！共" + questions.size() + "道题目");
            }
            
            // 4. 初始化试卷并绑定到C级活动
            List<Activity> activities = activityRepository.findAll();
            if (paperRepository.count() == 0 && !activities.isEmpty()) {
                Activity cActivity = activities.stream()
                    .filter(a -> a.getLevel() == Activity.Level.C)
                    .findFirst().orElse(null);
                
                if (cActivity != null) {
                    ExamPaper paper = new ExamPaper();
                    paper.setName("2024学年第一学期C级考试");
                    paper.setDescription("C级教师业务能力测试");
                    paper.setTotalScore(100);
                    paper.setDurationMinutes(60);
                    paper.setQuestionCount(15);
                    paper.setStatus(ExamPaper.Status.active);
                    paper.setCreatedBy(1L);
                    paperRepository.save(paper);
                    
                    cActivity.setExamPaperId(paper.getId());
                    cActivity.setHasExam(true);
                    cActivity.setExamDurationMinutes(60);
                    cActivity.setStartDate(LocalDate.of(2024, 9, 1)); // 设置开始日期
                    cActivity.setEndDate(LocalDate.of(2025, 1, 31)); // 设置结束日期
                    cActivity.setReviewerIds("[1]"); // 设置评审员ID
                    activityRepository.save(cActivity);
                    
                    List<ExamQuestion> questions = questionRepository.findAll();
                    List<ExamQuestion> cQuestions = questions.stream()
                        .filter(q -> q.getQuestionType() == ExamQuestion.QuestionType.single)
                        .limit(10)
                        .toList();
                    List<ExamQuestion> cMultiQuestions = questions.stream()
                        .filter(q -> q.getQuestionType() == ExamQuestion.QuestionType.multiple)
                        .limit(5)
                        .toList();
                    
                    List<PaperQuestion> pqs = new ArrayList<>();
                    int order = 1;
                    for (ExamQuestion q : cQuestions) {
                        PaperQuestion pq = new PaperQuestion();
                        pq.setPaperId(paper.getId());
                        pq.setQuestionId(q.getId());
                        pq.setQuestionOrder(order++);
                        pqs.add(pq);
                    }
                    for (ExamQuestion q : cMultiQuestions) {
                        PaperQuestion pq = new PaperQuestion();
                        pq.setPaperId(paper.getId());
                        pq.setQuestionId(q.getId());
                        pq.setQuestionOrder(order++);
                        pqs.add(pq);
                    }
                    paperQuestionRepository.saveAll(pqs);
                    
                    System.out.println("试卷初始化完成！C级活动已绑定试卷");
                }
            }
            
            // 5. 初始化教师报名（C级活动）
            if (enrollmentRepository.count() == 0 && !activities.isEmpty()) {
                Activity cActivity = activities.stream()
                    .filter(a -> a.getLevel() == Activity.Level.C)
                    .findFirst().orElse(null);
                
                if (cActivity != null) {
                    User teacher1 = userRepository.findByUsername("teacher1").orElse(null);
                    User teacher2 = userRepository.findByUsername("teacher2").orElse(null);
                    User teacher3 = userRepository.findByUsername("teacher3").orElse(null);
                    
                    if (teacher1 != null) {
                        PeriodEnrollment enrollment1 = new PeriodEnrollment();
                        enrollment1.setActivityId(cActivity.getId());
                        enrollment1.setTeacherId(teacher1.getId());
                        enrollment1.setStatus(PeriodEnrollment.Status.enrolled);
                        enrollmentRepository.save(enrollment1);
                    }
                    
                    if (teacher2 != null) {
                        PeriodEnrollment enrollment2 = new PeriodEnrollment();
                        enrollment2.setActivityId(cActivity.getId());
                        enrollment2.setTeacherId(teacher2.getId());
                        enrollment2.setStatus(PeriodEnrollment.Status.enrolled);
                        enrollmentRepository.save(enrollment2);
                    }

                    if (teacher3 != null) {
                        PeriodEnrollment enrollment3 = new PeriodEnrollment();
                        enrollment3.setActivityId(cActivity.getId());
                        enrollment3.setTeacherId(teacher3.getId());
                        enrollment3.setStatus(PeriodEnrollment.Status.enrolled);
                        enrollmentRepository.save(enrollment3);
                    }
                    
                    System.out.println("教师报名初始化完成！");
                }
            }
            
            System.out.println("========================================");
            System.out.println("测试数据初始化完成！");
            System.out.println("========================================");
        };
    }

    private void migrateTeacherLevels(UserRepository userRepository,
                                       EvaluationRepository evaluationRepository,
                                       ActivityRepository activityRepository,
                                       TeacherLevelHistoryRepository historyRepository) {
        List<User> teachers = userRepository.findByRole(User.Role.teacher);
        int migrated = 0;
        for (User user : teachers) {
            if (user.getTeacherLevel() != TeacherLevel.NONE) continue;

            List<Evaluation> passedEvals = evaluationRepository.findByTeacherIdAndIsPublished(user.getId())
                    .stream()
                    .filter(e -> Boolean.TRUE.equals(e.getIsPassed()))
                    .toList();

            if (passedEvals.isEmpty()) continue;

            Activity.Level highestLevel = null;
            LocalDateTime latestPassedAt = null;
            for (Evaluation eval : passedEvals) {
                Activity activity = activityRepository.findById(eval.getActivityId()).orElse(null);
                if (activity == null) continue;
                if (highestLevel == null || activity.getLevel().getTier() > highestLevel.getTier()) {
                    highestLevel = activity.getLevel();
                    latestPassedAt = eval.getUpdatedAt();
                }
            }

            if (highestLevel != null) {
                TeacherLevel newLevel = TeacherLevel.fromActivityLevel(highestLevel);
                user.setTeacherLevel(newLevel);
                user.setLevelChangedAt(latestPassedAt != null ? latestPassedAt : LocalDateTime.now());
                userRepository.save(user);

                TeacherLevelHistory history = new TeacherLevelHistory();
                history.setTeacherId(user.getId());
                history.setOldLevel(TeacherLevel.NONE);
                history.setNewLevel(newLevel);
                history.setChangeType(TeacherLevelHistory.ChangeType.AUTO);
                history.setChangedByUserId(null);
                historyRepository.save(history);

                migrated++;
                System.out.println("数据迁移：教师 " + user.getRealName() + " 等级回填为 " + newLevel.getDisplayName());
            }
        }
        if (migrated > 0) {
            System.out.println("数据迁移完成，共回填 " + migrated + " 位教师的等级");
        }
    }
}
