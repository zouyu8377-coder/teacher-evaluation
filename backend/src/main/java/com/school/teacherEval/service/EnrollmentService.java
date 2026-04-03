package com.school.teacherEval.service;

import com.school.teacherEval.entity.EvaluationPeriod;
import com.school.teacherEval.entity.PeriodEnrollment;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.PeriodEnrollmentRepository;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    
    private final PeriodEnrollmentRepository enrollmentRepository;
    private final EvaluationPeriodService periodService;
    private final UserRepository userRepository;
    
    @Transactional
    public PeriodEnrollment enroll(Long periodId, Long teacherId) {
        EvaluationPeriod period = periodService.getPeriodById(periodId);
        
        if (period.getStatus() != EvaluationPeriod.Status.active) {
            throw new RuntimeException("该考核周期未启用，无法报名");
        }
        
        if (enrollmentRepository.existsByPeriodIdAndTeacherIdAndStatus(periodId, teacherId, PeriodEnrollment.Status.enrolled)) {
            throw new RuntimeException("您已报名该考核周期");
        }
        
        PeriodEnrollment enrollment = new PeriodEnrollment();
        enrollment.setPeriodId(periodId);
        enrollment.setTeacherId(teacherId);
        enrollment.setStatus(PeriodEnrollment.Status.enrolled);
        
        return enrollmentRepository.save(enrollment);
    }
    
    @Transactional
    public void removeEnrollment(Long periodId, Long teacherId) {
        PeriodEnrollment enrollment = enrollmentRepository.findActiveEnrollment(periodId, teacherId)
                .orElseThrow(() -> new RuntimeException("报名记录不存在"));
        
        enrollment.setStatus(PeriodEnrollment.Status.removed);
        enrollmentRepository.save(enrollment);
    }
    
    public List<User> getEnrolledTeachers(Long periodId) {
        List<PeriodEnrollment> enrollments = enrollmentRepository.findActiveEnrollmentsByPeriodId(periodId);
        List<Long> teacherIds = enrollments.stream()
                .map(PeriodEnrollment::getTeacherId)
                .collect(Collectors.toList());
        
        return userRepository.findAllById(teacherIds);
    }
    
    public List<EvaluationPeriod> getEnrolledPeriods(Long teacherId) {
        List<PeriodEnrollment> enrollments = enrollmentRepository.findActiveEnrollmentsByTeacherId(teacherId);
        List<Long> periodIds = enrollments.stream()
                .map(PeriodEnrollment::getPeriodId)
                .collect(Collectors.toList());
        
        if (periodIds.isEmpty()) {
            return List.of();
        }
        return periodService.getAllPeriods().stream()
                .filter(p -> periodIds.contains(p.getId()))
                .collect(Collectors.toList());
    }
    
    public boolean isEnrolled(Long periodId, Long teacherId) {
        return enrollmentRepository.existsByPeriodIdAndTeacherIdAndStatus(periodId, teacherId, PeriodEnrollment.Status.enrolled);
    }
    
    public List<EvaluationPeriod> getAvailablePeriods() {
        return periodService.getAllPeriods().stream()
                .filter(p -> p.getStatus() == EvaluationPeriod.Status.active)
                .collect(Collectors.toList());
    }
}