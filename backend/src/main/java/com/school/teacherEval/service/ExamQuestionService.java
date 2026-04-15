package com.school.teacherEval.service;

import com.school.teacherEval.entity.ExamQuestion;
import com.school.teacherEval.repository.ExamQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    
    public Page<ExamQuestion> getQuestions(ExamQuestion.QuestionType type, Boolean status, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        
        if (type != null) {
            return questionRepository.findByQuestionType(type, pageable);
        } else if (status != null) {
            return questionRepository.findByStatus(status, pageable);
        }
        return questionRepository.findAll(pageable);
    }
    
    public ExamQuestion getById(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("题目不存在"));
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
        question.setStatus(updated.getStatus());
        return questionRepository.save(question);
    }
    
    @Transactional
    public void delete(Long id) {
        questionRepository.deleteById(id);
    }
    
    public long countActive() {
        return questionRepository.countByStatus(true);
    }
    
    @Transactional
    public int importFromExcel(MultipartFile file, Long createdBy) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            
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
        } catch (Exception e) {
            throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
        }
    }
    
    private int importSheet(Sheet sheet, Long createdBy, ExamQuestion.QuestionType type) {
        int count = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isRowEmpty(row)) continue;
            
            try {
                ExamQuestion question = new ExamQuestion();
                question.setQuestionType(type);
                question.setCreatedBy(createdBy);
                question.setStatus(true);
                
                question.setQuestionText(getCellValue(row.getCell(0)));
                
                List<Map<String, String>> options = new ArrayList<>();
                String[] optionIds = {"A", "B", "C", "D", "E", "F"};
                for (int j = 0; j < 6; j++) {
                    Cell cell = row.getCell(j + 1);
                    String optionText = getCellValue(cell);
                    if (optionText != null && !optionText.trim().isEmpty()) {
                        Map<String, String> opt = new HashMap<>();
                        opt.put("id", optionIds[j]);
                        opt.put("text", optionText);
                        options.add(opt);
                    }
                }

                // 至少有2个选项才保存
                if (options.size() < 2) {
                    continue;
                }

                question.setOptions(toJson(options));

                // 正确答案在第8列（索引7）
                question.setCorrectAnswer(getCellValue(row.getCell(7)));

                // 分值在第9列（索引8）
                String scoreStr = getCellValue(row.getCell(8));
                question.setScore(scoreStr != null && !scoreStr.isEmpty() ? Integer.parseInt(scoreStr) : 5);

                // 解析在第10列（索引9）
                question.setExplanation(getCellValue(row.getCell(9)));
                
                question.setDifficulty(1);
                
                questionRepository.save(question);
                count++;
            } catch (Exception e) {
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}