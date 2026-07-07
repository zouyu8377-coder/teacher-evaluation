package com.school.teacherEval.service;

import com.school.teacherEval.dto.UserImportResultDTO;
import com.school.teacherEval.entity.TeacherLevel;
import com.school.teacherEval.entity.User;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserExcelService {

    private static final String USER_SHEET = "用户数据";
    private static final String[] HEADERS = {
            "用户名", "密码", "姓名", "角色", "部门", "状态", "教师等级", "创建时间"
    };
    private static final int COL_USERNAME = 0;
    private static final int COL_PASSWORD = 1;
    private static final int COL_REAL_NAME = 2;
    private static final int COL_ROLE = 3;
    private static final int COL_DEPARTMENT = 4;
    private static final int COL_STATUS = 5;
    private static final int COL_TEACHER_LEVEL = 6;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_.-]{3,50}$");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Workbook createTemplateWorkbook() {
        Workbook workbook = new XSSFWorkbook();
        createUserSheet(workbook, true);
        createInstructionSheet(workbook);
        return workbook;
    }

    public Workbook exportUsers(String role, String keyword) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = createUserSheet(workbook, false);
        List<User> users = userRepository.findAll().stream()
                .filter(user -> role == null || role.isBlank() || user.getRole().name().equals(role))
                .filter(user -> keyword == null || keyword.isBlank()
                        || contains(user.getUsername(), keyword)
                        || contains(user.getRealName(), keyword)
                        || contains(user.getDepartment(), keyword))
                .sorted(Comparator.comparing(User::getId))
                .toList();

        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            writeUserRow(row, user);
        }
        autoSizeColumns(sheet);
        return workbook;
    }

    public UserImportResultDTO previewImport(MultipartFile file) {
        return processImport(file, false);
    }

    @Transactional
    public UserImportResultDTO importUsers(MultipartFile file) {
        return processImport(file, true);
    }

    private UserImportResultDTO processImport(MultipartFile file, boolean persist) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheet(USER_SHEET);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            UserImportResultDTO result = new UserImportResultDTO();
            Set<String> seenUsernames = new HashSet<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                importRow(row, i + 1, result, seenUsernames, persist);
            }
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    private Sheet createUserSheet(Workbook workbook, boolean withSamples) {
        Sheet sheet = workbook.createSheet(USER_SHEET);
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        if (withSamples) {
            Row teacher = sheet.createRow(1);
            teacher.createCell(COL_USERNAME).setCellValue("teacher_demo");
            teacher.createCell(COL_PASSWORD).setCellValue("teacher123");
            teacher.createCell(COL_REAL_NAME).setCellValue("示例教师");
            teacher.createCell(COL_ROLE).setCellValue("teacher");
            teacher.createCell(COL_DEPARTMENT).setCellValue("语文组");
            teacher.createCell(COL_STATUS).setCellValue("启用");
            teacher.createCell(COL_TEACHER_LEVEL).setCellValue("NONE");

            Row evaluator = sheet.createRow(2);
            evaluator.createCell(COL_USERNAME).setCellValue("evaluator_demo");
            evaluator.createCell(COL_PASSWORD).setCellValue("evaluator123");
            evaluator.createCell(COL_REAL_NAME).setCellValue("示例评分员");
            evaluator.createCell(COL_ROLE).setCellValue("evaluator");
            evaluator.createCell(COL_DEPARTMENT).setCellValue("教研组");
            evaluator.createCell(COL_STATUS).setCellValue("启用");
            evaluator.createCell(COL_TEACHER_LEVEL).setCellValue("NONE");
        }

        autoSizeColumns(sheet);
        return sheet;
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("填写说明");
        String[] lines = {
                "1. 用户名是唯一校验字段，模板内不能重复；模板不使用 ID 列。",
                "2. 用户名不存在时新增用户，用户名、密码、姓名、角色为必填。",
                "3. 用户名已存在时更新该用户，仅覆盖本行非空字段；密码留空表示不修改密码。",
                "4. 用户名仅支持 3-50 位字母、数字、下划线、短横线和点。",
                "5. 角色可填 teacher / evaluator / admin，也可填 教师 / 评分员 / 管理员。",
                "6. 状态建议填 启用 / 禁用；也兼容 1 / 0，其中 1=启用，0=禁用；新增用户留空默认启用。",
                "7. 教师等级可填 NONE / C / B / A，也可填 无 / C级 / B级 / A级；非教师用户建议留空或填 NONE。",
                "8. 创建时间仅用于导出查看，导入时会被忽略。"
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 16000);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeUserRow(Row row, User user) {
        row.createCell(COL_USERNAME).setCellValue(user.getUsername());
        row.createCell(COL_PASSWORD).setCellValue("");
        row.createCell(COL_REAL_NAME).setCellValue(nullToEmpty(user.getRealName()));
        row.createCell(COL_ROLE).setCellValue(user.getRole().name());
        row.createCell(COL_DEPARTMENT).setCellValue(nullToEmpty(user.getDepartment()));
        row.createCell(COL_STATUS).setCellValue(user.getStatus() == null || user.getStatus() == 1 ? "启用" : "禁用");
        row.createCell(COL_TEACHER_LEVEL).setCellValue(user.getTeacherLevel() == null ? TeacherLevel.NONE.name() : user.getTeacherLevel().name());
        if (user.getCreatedAt() != null) {
            row.createCell(7).setCellValue(user.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
    }

    private void importRow(Row row, int rowNumber, UserImportResultDTO result, Set<String> seenUsernames, boolean persist) {
        try {
            String username = require(row, COL_USERNAME, rowNumber, "用户名");
            validateUsername(username);
            String usernameKey = username.toLowerCase(Locale.ROOT);
            if (!seenUsernames.add(usernameKey)) {
                throw new BusinessException("用户名在模板中重复: " + username);
            }

            User existingUser = userRepository.findByUsername(username).orElse(null);
            if (existingUser == null) {
                createNewUser(row, rowNumber, result, username, persist);
            } else {
                updateExistingUser(existingUser, row, result, persist);
            }
        } catch (Exception e) {
            result.skipped("第 " + rowNumber + " 行: " + e.getMessage());
        }
    }

    private void updateExistingUser(User user, Row row, UserImportResultDTO result, boolean persist) {
        if (!hasAnyUpdateField(row)) {
            throw new BusinessException("没有可更新的非空字段");
        }

        if (persist) {
            String password = trim(getCellValue(row.getCell(COL_PASSWORD)));
            if (hasText(password)) {
                validatePassword(password);
                user.setPassword(passwordEncoder.encode(password));
            }
            applyNonEmptyFields(user, row);
            userRepository.save(user);
        }
        result.updated();
    }

    private void createNewUser(Row row, int rowNumber, UserImportResultDTO result, String username, boolean persist) {
        String password = require(row, COL_PASSWORD, rowNumber, "密码");
        String realName = require(row, COL_REAL_NAME, rowNumber, "姓名");
        User.Role role = parseRole(require(row, COL_ROLE, rowNumber, "角色"));

        validatePassword(password);

        if (persist && userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在: " + username);
        }

        if (persist) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRealName(realName);
            user.setRole(role);
            user.setDepartment(trim(getCellValue(row.getCell(COL_DEPARTMENT))));
            user.setStatus(parseStatus(getCellValue(row.getCell(COL_STATUS)), 1));
            user.setTeacherLevel(parseTeacherLevel(getCellValue(row.getCell(COL_TEACHER_LEVEL)), TeacherLevel.NONE));
            userRepository.save(user);
        }
        result.created();
    }

    private void applyNonEmptyFields(User user, Row row) {
        String realName = trim(getCellValue(row.getCell(COL_REAL_NAME)));
        if (hasText(realName)) {
            user.setRealName(realName);
        }

        String role = trim(getCellValue(row.getCell(COL_ROLE)));
        if (hasText(role)) {
            user.setRole(parseRole(role));
        }

        String department = trim(getCellValue(row.getCell(COL_DEPARTMENT)));
        if (hasText(department)) {
            user.setDepartment(department);
        }

        String status = trim(getCellValue(row.getCell(COL_STATUS)));
        if (hasText(status)) {
            user.setStatus(parseStatus(status, user.getStatus() == null ? 1 : user.getStatus()));
        }

        String teacherLevel = trim(getCellValue(row.getCell(COL_TEACHER_LEVEL)));
        if (hasText(teacherLevel)) {
            user.setTeacherLevel(parseTeacherLevel(teacherLevel, user.getTeacherLevel()));
        }
    }

    private boolean hasAnyUpdateField(Row row) {
        for (int i = COL_PASSWORD; i <= COL_TEACHER_LEVEL; i++) {
            if (hasText(getCellValue(row.getCell(i)))) {
                return true;
            }
        }
        return false;
    }

    private User.Role parseRole(String value) {
        String normalized = trim(value).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "teacher", "教师" -> User.Role.teacher;
            case "evaluator", "评分员", "考核员" -> User.Role.evaluator;
            case "admin", "管理员" -> User.Role.admin;
            default -> throw new BusinessException("角色无效: " + value);
        };
    }

    private Integer parseStatus(String value, Integer defaultValue) {
        String normalized = trim(value).toLowerCase(Locale.ROOT);
        if (!hasText(normalized)) {
            return defaultValue;
        }
        return switch (normalized) {
            case "1", "启用", "正常", "active", "enabled" -> 1;
            case "0", "禁用", "停用", "inactive", "disabled" -> 0;
            default -> throw new BusinessException("状态无效: " + value);
        };
    }

    private TeacherLevel parseTeacherLevel(String value, TeacherLevel defaultValue) {
        String normalized = trim(value).toUpperCase(Locale.ROOT);
        if (!hasText(normalized)) {
            return defaultValue == null ? TeacherLevel.NONE : defaultValue;
        }
        normalized = normalized.replace("级", "");
        return switch (normalized) {
            case "NONE", "无", "无等级" -> TeacherLevel.NONE;
            case "C" -> TeacherLevel.C;
            case "B" -> TeacherLevel.B;
            case "A" -> TeacherLevel.A;
            default -> throw new BusinessException("教师等级无效: " + value);
        };
    }

    private String require(Row row, int cellIndex, int rowNumber, String fieldName) {
        String value = trim(getCellValue(row.getCell(cellIndex)));
        if (!hasText(value)) {
            throw new BusinessException("第 " + rowNumber + " 行 " + fieldName + " 不能为空");
        }
        return value;
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new BusinessException("用户名需为 3-50 位，且仅支持字母、数字、下划线、短横线和点");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 6 || password.length() > 64) {
            throw new BusinessException("密码长度需为 6-64 位");
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < HEADERS.length; i++) {
            if (hasText(getCellValue(row.getCell(i)))) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (value == Math.floor(value)) {
                return String.valueOf((long) value);
            }
            return String.valueOf(value);
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        return cell.getStringCellValue();
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
            int width = Math.max(sheet.getColumnWidth(i), 3200);
            sheet.setColumnWidth(i, Math.min(width, 8000));
        }
    }

    private boolean contains(String source, String keyword) {
        return source != null && source.contains(keyword);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
