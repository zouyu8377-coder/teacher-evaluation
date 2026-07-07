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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserExcelService {

    private static final String USER_SHEET = "用户数据";
    private static final String[] HEADERS = {
            "ID", "用户名", "密码", "姓名", "角色", "部门", "状态", "教师等级", "创建时间"
    };
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

    @Transactional
    public UserImportResultDTO importUsers(MultipartFile file) {
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
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                importRow(row, i + 1, result);
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
            teacher.createCell(0).setCellValue("");
            teacher.createCell(1).setCellValue("teacher_demo");
            teacher.createCell(2).setCellValue("teacher123");
            teacher.createCell(3).setCellValue("示例教师");
            teacher.createCell(4).setCellValue("teacher");
            teacher.createCell(5).setCellValue("语文组");
            teacher.createCell(6).setCellValue("1");
            teacher.createCell(7).setCellValue("NONE");

            Row evaluator = sheet.createRow(2);
            evaluator.createCell(0).setCellValue("");
            evaluator.createCell(1).setCellValue("evaluator_demo");
            evaluator.createCell(2).setCellValue("evaluator123");
            evaluator.createCell(3).setCellValue("示例评分员");
            evaluator.createCell(4).setCellValue("evaluator");
            evaluator.createCell(5).setCellValue("教研组");
            evaluator.createCell(6).setCellValue("1");
            evaluator.createCell(7).setCellValue("NONE");
        }

        autoSizeColumns(sheet);
        return sheet;
    }

    private void createInstructionSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("填写说明");
        String[] lines = {
                "1. 新增用户：ID 留空，用户名、密码、姓名、角色必填。",
                "2. 修改用户：ID 填现有用户 ID，只会替换本行非空字段；密码留空表示不修改密码。",
                "3. 用户名仅支持 3-50 位字母、数字、下划线、短横线和点。",
                "4. 角色可填 teacher / evaluator / admin，也可填 教师 / 评分员 / 管理员。",
                "5. 状态可填 1/0、启用/禁用；留空时新增用户默认启用。",
                "6. 教师等级可填 NONE / C / B / A，也可填 无 / C级 / B级 / A级。非教师用户建议留空或填 NONE。"
        };
        for (int i = 0; i < lines.length; i++) {
            sheet.createRow(i).createCell(0).setCellValue(lines[i]);
        }
        sheet.setColumnWidth(0, 12000);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void writeUserRow(Row row, User user) {
        row.createCell(0).setCellValue(user.getId());
        row.createCell(1).setCellValue(user.getUsername());
        row.createCell(2).setCellValue("");
        row.createCell(3).setCellValue(nullToEmpty(user.getRealName()));
        row.createCell(4).setCellValue(user.getRole().name());
        row.createCell(5).setCellValue(nullToEmpty(user.getDepartment()));
        row.createCell(6).setCellValue(user.getStatus() == null ? 1 : user.getStatus());
        row.createCell(7).setCellValue(user.getTeacherLevel() == null ? TeacherLevel.NONE.name() : user.getTeacherLevel().name());
        if (user.getCreatedAt() != null) {
            row.createCell(8).setCellValue(user.getCreatedAt().format(DATE_TIME_FORMATTER));
        }
    }

    private void importRow(Row row, int rowNumber, UserImportResultDTO result) {
        try {
            Long id = parseLong(getCellValue(row.getCell(0)));
            if (id != null) {
                updateExistingUser(id, row, rowNumber, result);
            } else {
                createNewUser(row, rowNumber, result);
            }
        } catch (Exception e) {
            result.skipped("第 " + rowNumber + " 行: " + e.getMessage());
        }
    }

    private void updateExistingUser(Long id, Row row, int rowNumber, UserImportResultDTO result) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ID " + id + " 对应用户不存在"));

        String username = trim(getCellValue(row.getCell(1)));
        if (hasText(username)) {
            validateUsername(username);
            Optional<User> sameUsername = userRepository.findByUsername(username);
            if (sameUsername.isPresent() && !sameUsername.get().getId().equals(id)) {
                throw new BusinessException("用户名已被其他用户使用: " + username);
            }
            user.setUsername(username);
        }

        String password = trim(getCellValue(row.getCell(2)));
        if (hasText(password)) {
            validatePassword(password);
            user.setPassword(passwordEncoder.encode(password));
        }

        applyNonEmptyFields(user, row, rowNumber);
        userRepository.save(user);
        result.updated();
    }

    private void createNewUser(Row row, int rowNumber, UserImportResultDTO result) {
        String username = require(row, 1, rowNumber, "用户名");
        String password = require(row, 2, rowNumber, "密码");
        String realName = require(row, 3, rowNumber, "姓名");
        User.Role role = parseRole(require(row, 4, rowNumber, "角色"));

        validateUsername(username);
        validatePassword(password);
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setDepartment(trim(getCellValue(row.getCell(5))));
        user.setStatus(parseStatus(getCellValue(row.getCell(6)), 1));
        user.setTeacherLevel(parseTeacherLevel(getCellValue(row.getCell(7)), TeacherLevel.NONE));
        userRepository.save(user);
        result.created();
    }

    private void applyNonEmptyFields(User user, Row row, int rowNumber) {
        String realName = trim(getCellValue(row.getCell(3)));
        if (hasText(realName)) {
            user.setRealName(realName);
        }

        String role = trim(getCellValue(row.getCell(4)));
        if (hasText(role)) {
            user.setRole(parseRole(role));
        }

        String department = trim(getCellValue(row.getCell(5)));
        if (hasText(department)) {
            user.setDepartment(department);
        }

        String status = trim(getCellValue(row.getCell(6)));
        if (hasText(status)) {
            user.setStatus(parseStatus(status, user.getStatus() == null ? 1 : user.getStatus()));
        }

        String teacherLevel = trim(getCellValue(row.getCell(7)));
        if (hasText(teacherLevel)) {
            user.setTeacherLevel(parseTeacherLevel(teacherLevel, user.getTeacherLevel()));
        }
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
        String normalized = trim(value);
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

    private Long parseLong(String value) {
        String text = trim(value);
        if (!hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new BusinessException("ID 必须为整数: " + value);
        }
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
