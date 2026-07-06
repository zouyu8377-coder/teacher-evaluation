package com.school.teacherEval.service;

import com.school.teacherEval.config.MinioConfig;
import com.school.teacherEval.entity.Activity;
import com.school.teacherEval.entity.Document;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ActivityRepository;
import com.school.teacherEval.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MinioConfig minioConfig;
    @Mock
    private EnrollmentService enrollmentService;
    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void updateDocument_shouldRejectTeacherMutationAfterMaterialWindowEnded() {
        Document document = new Document();
        document.setId(1L);
        document.setUserId(10L);
        document.setActivityId(2L);
        document.setIsDeleted(0);

        Activity activity = new Activity();
        activity.setId(2L);
        activity.setLevel(Activity.Level.B1);
        activity.setHasExam(false);
        activity.setMaterialStart(LocalDateTime.now().minusDays(2));
        activity.setMaterialEnd(LocalDateTime.now().minusDays(1));

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(activityRepository.findById(2L)).thenReturn(Optional.of(activity));

        assertThrows(BusinessException.class,
                () -> documentService.updateDocument(1L, 10L, "new title", "new desc"));
    }
}
