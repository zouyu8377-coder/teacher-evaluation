package com.school.teacherEval.service;

import com.school.teacherEval.dto.UserImportResultDTO;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.repository.UserRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserExcelServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createTemplateWorkbook_shouldUseUsernameAsFirstColumnWithoutId() {
        UserExcelService service = new UserExcelService(userRepository, passwordEncoder);

        try (Workbook workbook = service.createTemplateWorkbook()) {
            Row header = workbook.getSheet("用户数据").getRow(0);

            assertEquals("用户名", header.getCell(0).getStringCellValue());
            assertFalse("ID".equalsIgnoreCase(header.getCell(0).getStringCellValue()));
            assertEquals("密码", header.getCell(1).getStringCellValue());
            assertEquals("状态", header.getCell(5).getStringCellValue());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void previewImport_shouldCountCreatesUpdatesAndDuplicateUsernamesWithoutSaving() throws Exception {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("existing_user");
        existing.setStatus(1);

        when(userRepository.findByUsername("existing_user")).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("new_user")).thenReturn(Optional.empty());

        UserExcelService service = new UserExcelService(userRepository, passwordEncoder);

        UserImportResultDTO result = service.previewImport(createImportFile());

        assertEquals(1, result.getCreatedCount());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(1, result.getErrors().size());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    private MockMultipartFile createImportFile() throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("用户数据");
            Row header = sheet.createRow(0);
            String[] headers = {"用户名", "密码", "姓名", "角色", "部门", "状态", "教师等级", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row existing = sheet.createRow(1);
            existing.createCell(0).setCellValue("existing_user");
            existing.createCell(2).setCellValue("已存在用户");
            existing.createCell(5).setCellValue("禁用");

            Row created = sheet.createRow(2);
            created.createCell(0).setCellValue("new_user");
            created.createCell(1).setCellValue("password123");
            created.createCell(2).setCellValue("新用户");
            created.createCell(3).setCellValue("teacher");
            created.createCell(4).setCellValue("语文组");
            created.createCell(5).setCellValue("启用");

            Row duplicate = sheet.createRow(3);
            duplicate.createCell(0).setCellValue("new_user");
            duplicate.createCell(1).setCellValue("password123");
            duplicate.createCell(2).setCellValue("重复用户");
            duplicate.createCell(3).setCellValue("teacher");

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "users.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
