package com.school.teacherEval.service;

import com.school.teacherEval.entity.ExamQuestion;
import com.school.teacherEval.exception.BusinessException;
import com.school.teacherEval.repository.ExamQuestionRepository;
import com.school.teacherEval.repository.PaperQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExamQuestionService {
    
    private final ExamQuestionRepository questionRepository;
    private final PaperQuestionRepository paperQuestionRepository;

    public Page<ExamQuestion> getQuestions(ExamQuestion.QuestionType type, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));

        if (type != null) {
            return questionRepository.findByQuestionType(type, pageable);
        }
        return questionRepository.findAll(pageable);
    }
    
    public ExamQuestion getById(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new BusinessException("题目不存在"));
    }
    
    @Transactional
    public ExamQuestion create(ExamQuestion question) {
        question.setId(null);
        return questionRepository.save(question);
    }
    
    @Transactional
    public ExamQuestion update(Long id, ExamQuestion updated) {
        ExamQuestion question = getById(id);
        question.setQuestionText(updated.getQuestionText());
        question.setQuestionType(updated.getQuestionType());
        question.setOptions(updated.getOptions());
        question.setCorrectAnswer(updated.getCorrectAnswer());
        question.setScore(updated.getScore());
        question.setExplanation(updated.getExplanation());
        question.setDifficulty(updated.getDifficulty());
        return questionRepository.save(question);
    }
    
    @Transactional
    public void delete(Long id) {
        paperQuestionRepository.deleteByQuestionId(id);
        questionRepository.deleteById(id);
    }

    public int importFromExcel(MultipartFile file, Long createdBy) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            int importedCount = 0;

            Sheet singleSheet = workbook.getSheet("单选题");
            if (singleSheet != null) {
                importedCount += importSheet(singleSheet, createdBy, ExamQuestion.QuestionType.single);
            }

            Sheet multiSheet = workbook.getSheet("多选题");
            if (multiSheet != null) {
                importedCount += importSheet(multiSheet, createdBy, ExamQuestion.QuestionType.multiple);
            }

            return importedCount;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Excel导入失败: " + e.getMessage(), e);
        }
    }

    private int importSheet(Sheet sheet, Long createdBy, ExamQuestion.QuestionType type) {
        int count = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;

            try {
                ExamQuestion question = new ExamQuestion();
                question.setQuestionType(type);
                question.setCreatedBy(createdBy);

                String questionText = getCellValue(row.getCell(0));
                if (questionText == null || questionText.trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "行: 题目内容不能为空");
                    continue;
                }
                question.setQuestionText(questionText.trim());

                List<Map<String, String>> options = new ArrayList<>();
                String[] optionIds = {"A", "B", "C", "D", "E", "F"};
                for (int j = 0; j < 6; j++) {
                    Cell cell = row.getCell(j + 1);
                    String optionText = getCellValue(cell);
                    if (optionText != null && !optionText.trim().isEmpty()) {
                        Map<String, String> opt = new HashMap<>();
                        opt.put("id", optionIds[j]);
                        opt.put("text", optionText.trim());
                        options.add(opt);
                    }
                }

                if (options.size() < 2) {
                    errors.add("第" + (i + 1) + "行: 至少需要2个选项");
                    continue;
                }

                question.setOptions(toJson(options));

                String correctAnswer = getCellValue(row.getCell(7));
                if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
                    errors.add("第" + (i + 1) + "行: 正确答案不能为空");
                    continue;
                }
                question.setCorrectAnswer(correctAnswer.trim());

                String scoreStr = getCellValue(row.getCell(8));
                int score = 5;
                if (scoreStr != null && !scoreStr.isEmpty()) {
                    score = Integer.parseInt(scoreStr);
                }
                question.setScore(score);

                question.setExplanation(getCellValue(row.getCell(9)));

                question.setDifficulty(1);

                questionRepository.save(question);
                count++;
            } catch (NumberFormatException e) {
                errors.add("第" + (i + 1) + "行: 分值格式错误，必须为整数");
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行: " + e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            if (count == 0) {
                throw new BusinessException("Excel导入失败，共" + errors.size() + "处错误: " + String.join("; ", errors));
            }
        }
        return count;
    }
    
    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < 11; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValue(cell);
                if (val != null && !val.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
    
    private String toJson(List<Map<String, String>> options) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < options.size(); i++) {
            Map<String, String> opt = options.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(opt.get("id")).append("\",\"text\":\"").append(escapeJson(opt.get("text"))).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}