package org.example.quizapp.controller;

import org.example.quizapp.dto.request.AnswerSubmitRequest;
import org.example.quizapp.dto.request.QuestionGenerateRequest;
import org.example.quizapp.model.QuizQuestion;
import org.example.quizapp.model.UploadedFile;
import org.example.quizapp.service.QuizService;
import org.example.quizapp.service.RagService;
import org.example.quizapp.repository.FileRepository;
import org.example.quizapp.util.WordDocumentReader;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;
    private final RagService ragService;
    private final FileRepository fileRepository;

    public QuizController(
            QuizService quizService,
            RagService ragService,
            FileRepository fileRepository
    ) {
        this.quizService = quizService;
        this.ragService = ragService;
        this.fileRepository = fileRepository;
    }

    @PostMapping("/generate-from-document")
    public Map<String, Object> generateFromDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam String chatId,
            @RequestParam(defaultValue = "10") int numQuestions) {

        Map<String, Object> result = new HashMap<>();

        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件为空");
                return result;
            }

            if (numQuestions < 1 || numQuestions > 50) {
                result.put("success", false);
                result.put("message", "题目数量必须在 1~50 之间");
                return result;
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".docx") && !fileName.endsWith(".doc"))) {
                result.put("success", false);
                result.put("message", "仅支持 .docx 或 .doc 格式");
                return result;
            }

            String content = WordDocumentReader.extractText(file);

            if (content.isEmpty()) {
                result.put("success", false);
                result.put("message", "未能从文档中提取到文本内容");
                return result;
            }

            UploadedFile uploadedFile = new UploadedFile();
            uploadedFile.setFilename(UUID.randomUUID().toString() + "_" + fileName);
            uploadedFile.setOriginalFilename(fileName);
            uploadedFile.setFileSize(file.getSize());
            uploadedFile.setContentType(file.getContentType());
            uploadedFile.setFileData(file.getBytes());
            uploadedFile.setExtractedText(content);
            uploadedFile.setChatId(chatId);
            uploadedFile.setUploadTime(LocalDateTime.now());

            fileRepository.save(uploadedFile);

            List<QuizQuestion> questions = quizService.generateAndSaveQuestions(
                    content, chatId, fileName, numQuestions
            );

            result.put("success", true);
            result.put("requested", numQuestions);
            result.put("count", questions.size());
            result.put("questions", questions);
            if (questions.size() < numQuestions) {
                result.put("message", "成功生成 " + questions.size() + "/" + numQuestions + " 道题目（可用知识点不足）");
            } else {
                result.put("message", "成功生成 " + questions.size() + " 道题目");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @PostMapping("/generate-from-text")
    public Map<String, Object> generateFromText(
            @RequestBody QuestionGenerateRequest request,
            @RequestParam String chatId,
            @RequestParam(defaultValue = "10") int numQuestions) {

        Map<String, Object> result = new HashMap<>();

        try {
            String text = request.getText();
            String sourceDoc = request.getSourceDoc() != null ? request.getSourceDoc() : "手动输入";

            if (text == null || text.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "文本内容为空");
                return result;
            }

            if (numQuestions < 1 || numQuestions > 50) {
                result.put("success", false);
                result.put("message", "题目数量必须在 1~50 之间");
                return result;
            }

            List<QuizQuestion> questions = quizService.generateAndSaveQuestions(
                    text, chatId, sourceDoc, numQuestions
            );

            result.put("success", true);
            result.put("requested", numQuestions);
            result.put("count", questions.size());
            result.put("questions", questions);
            if (questions.size() < numQuestions) {
                result.put("message", "成功生成 " + questions.size() + "/" + numQuestions + " 道题目（可用知识点不足）");
            } else {
                result.put("message", "成功生成 " + questions.size() + " 道题目");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "生成失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getQuestions(@RequestParam String chatId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<QuizQuestion> questions = quizService.getQuestionsByChatId(chatId);
            result.put("success", true);
            result.put("questions", questions);
            result.put("count", questions.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    @PostMapping("/submit-answer")
    public Map<String, Object> submitAnswer(@RequestBody AnswerSubmitRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> answerResult = quizService.submitAnswer(
                    request.getUserId(),
                    request.getQuestionId(),
                    request.getSelectedAnswer()
            );
            result.putAll(answerResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "提交失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @PostMapping("/toggle-favorite")
    public Map<String, Object> toggleFavorite(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> favResult = quizService.toggleFavorite(
                    request.get("userId"),
                    request.get("questionId")
            );
            result.putAll(favResult);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/favorites")
    public Map<String, Object> getFavorites(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> favorites = quizService.getFavoriteQuestions(userId);
            result.put("success", true);
            result.put("favorites", favorites);
            result.put("count", favorites.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/wrong-answers")
    public Map<String, Object> getWrongAnswers(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<Map<String, Object>> wrongAnswers = quizService.getWrongQuestions(userId);
            result.put("success", true);
            result.put("wrongAnswers", wrongAnswers);
            result.put("count", wrongAnswers.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> stats = quizService.getStatistics(userId);
            result.put("success", true);
            result.put("statistics", stats);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    @DeleteMapping("/remove-wrong-answer")
    public Map<String, Object> removeWrongAnswer(
            @RequestParam String userId,
            @RequestParam String questionId) {

        Map<String, Object> result = new HashMap<>();

        try {
            quizService.removeWrongAnswer(userId, questionId);
            result.put("success", true);
            result.put("message", "已移除错题记录");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @GetMapping("/knowledge-map")
    public Map<String, Object> getKnowledgeMap(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            List<QuizQuestion> allQuestions = quizService.getQuestionsByChatId(userId);

            if (allQuestions.isEmpty()) {
                result.put("success", true);
                result.put("mapData", null);
                result.put("message", "暂无学习数据");
                return result;
            }

            Map<String, List<Map<String, Object>>> knowledgePoints = new HashMap<>();

            for (QuizQuestion q : allQuestions) {
                String sourceDoc = q.getSourceDocument();
                if (!knowledgePoints.containsKey(sourceDoc)) {
                    knowledgePoints.put(sourceDoc, new ArrayList<>());
                }

                Map<String, Object> questionInfo = new HashMap<>();
                questionInfo.put("question", q.getQuestion().substring(0, Math.min(30, q.getQuestion().length())));
                questionInfo.put("id", q.getId());
                knowledgePoints.get(sourceDoc).add(questionInfo);
            }

            List<Map<String, Object>> mapNodes = new ArrayList<>();
            knowledgePoints.forEach((doc, questions) -> {
                Map<String, Object> docNode = new HashMap<>();
                docNode.put("name", doc);
                docNode.put("type", "document");

                List<Map<String, Object>> children = new ArrayList<>();
                questions.forEach(q -> {
                    Map<String, Object> qNode = new HashMap<>();
                    qNode.put("name", (String) q.get("question"));
                    qNode.put("type", "question");
                    children.add(qNode);
                });

                docNode.put("children", children);
                mapNodes.add(docNode);
            });

            result.put("success", true);
            result.put("mapData", mapNodes);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @DeleteMapping("/delete-question")
    public Map<String, Object> deleteQuestion(
            @RequestParam String userId,
            @RequestParam String questionId) {

        Map<String, Object> result = new HashMap<>();

        try {
            quizService.deleteQuestion(userId, questionId);
            result.put("success", true);
            result.put("message", "已删除");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @PostMapping("/delete-document")
    public Map<String, Object> deleteDocument(@RequestBody Map<String, String> request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String userId = request.get("userId");
            String documentName = request.get("documentName");

            quizService.deleteDocument(userId, documentName);
            result.put("success", true);
            result.put("message", "已删除文档及所有题目");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}
