package org.example.quizapp.service;

import org.example.quizapp.model.*;
import org.example.quizapp.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final UserProgressRepository progressRepository;
    private final WrongAnswerRepository wrongAnswerRepository;
    private final RagService ragService;

    public QuizService(
            QuizQuestionRepository questionRepository,
            UserProgressRepository progressRepository,
            WrongAnswerRepository wrongAnswerRepository,
            RagService ragService
    ) {
        this.questionRepository = questionRepository;
        this.progressRepository = progressRepository;
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.ragService = ragService;
    }

    public List<QuizQuestion> generateAndSaveQuestions(String text, String chatId, String sourceDoc, int numQuestions) {
        if (numQuestions <= 0) return Collections.emptyList();

        List<Map<String, Object>> generatedQuestions =
                ragService.generateQuestionsFromText(text, numQuestions);

        List<QuizQuestion> savedQuestions = new ArrayList<>();

        for (Map<String, Object> qData : generatedQuestions) {
            QuizQuestion question = new QuizQuestion();
            question.setQuestion(asString(qData.get("question")));

            Object optionsRaw = qData.get("options");
            if (optionsRaw instanceof List<?> list) {
                List<String> opts = new ArrayList<>(list.size());
                for (Object o : list) opts.add(o == null ? "" : o.toString());
                question.setOptions(opts);
            }

            Object correctRaw = qData.get("correctAnswer");
            if (correctRaw instanceof Number n) {
                question.setCorrectAnswer(n.intValue());
            } else if (correctRaw instanceof String s) {
                try { question.setCorrectAnswer(Integer.parseInt(s.trim())); }
                catch (NumberFormatException ignored) { }
            }

            question.setExplanation(asString(qData.get("explanation")));
            question.setSourceDocument(sourceDoc);
            question.setChatId(chatId);
            question.setCreatedAt(LocalDateTime.now());

            savedQuestions.add(questionRepository.save(question));
        }

        return savedQuestions;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    public List<QuizQuestion> getQuestionsByChatId(String chatId) {
        return questionRepository.findByChatIdOrderByCreatedAtDesc(chatId);
    }

    public Map<String, Object> submitAnswer(String userId, String questionId, Integer selectedAnswer) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("题目不存在"));

        boolean isCorrect = selectedAnswer.equals(question.getCorrectAnswer());

        UserProgress progress = getOrCreateProgress(userId, questionId);
        updateProgress(progress, isCorrect);
        progressRepository.save(progress);

        handleWrongAnswer(userId, questionId, selectedAnswer, question.getCorrectAnswer(), isCorrect);

        return buildAnswerResult(isCorrect, question, progress);
    }

    private UserProgress getOrCreateProgress(String userId, String questionId) {
        return progressRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseGet(() -> {
                    UserProgress progress = new UserProgress();
                    progress.setUserId(userId);
                    progress.setQuestionId(questionId);
                    progress.setTimesAnswered(0);
                    progress.setTimesCorrect(0);
                    progress.setCreatedAt(LocalDateTime.now());
                    return progress;
                });
    }

    private void updateProgress(UserProgress progress, boolean isCorrect) {
        progress.setTimesAnswered(progress.getTimesAnswered() + 1);
        if (isCorrect) {
            progress.setTimesCorrect(progress.getTimesCorrect() + 1);
        }
        progress.setLastAnsweredAt(LocalDateTime.now());
    }

    private void handleWrongAnswer(String userId, String questionId, Integer selectedAnswer,
                                   Integer correctAnswer, boolean isCorrect) {
        if (!isCorrect) {
            WrongAnswer wrongAnswer = new WrongAnswer();
            wrongAnswer.setUserId(userId);
            wrongAnswer.setQuestionId(questionId);
            wrongAnswer.setUserSelectedAnswer(selectedAnswer);
            wrongAnswer.setCorrectAnswer(correctAnswer);
            wrongAnswer.setWrongAt(LocalDateTime.now());
            wrongAnswerRepository.save(wrongAnswer);
        } else {
            wrongAnswerRepository.deleteByUserIdAndQuestionId(userId, questionId);
        }
    }

    private Map<String, Object> buildAnswerResult(boolean isCorrect, QuizQuestion question, UserProgress progress) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("isCorrect", isCorrect);
        result.put("correctAnswer", question.getCorrectAnswer());
        result.put("explanation", question.getExplanation());
        result.put("progress", progress);
        return result;
    }

    public Map<String, Object> toggleFavorite(String userId, String questionId) {
        UserProgress progress = getOrCreateProgress(userId, questionId);
        progress.setFavorite(!Boolean.TRUE.equals(progress.getFavorite()));
        progressRepository.save(progress);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("isFavorite", progress.getFavorite());
        return result;
    }

    public List<Map<String, Object>> getFavoriteQuestions(String userId) {
        List<UserProgress> favorites = progressRepository.findByUserIdAndIsFavoriteTrue(userId);

        return favorites.stream()
                .map(progress -> {
                    QuizQuestion question = questionRepository.findById(progress.getQuestionId()).orElse(null);
                    if (question != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("question", question);
                        map.put("progress", progress);
                        return map;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getWrongQuestions(String userId) {
        List<WrongAnswer> wrongAnswers = wrongAnswerRepository.findByUserIdOrderByWrongAtDesc(userId);

        return wrongAnswers.stream()
                .map(wrong -> {
                    QuizQuestion question = questionRepository.findById(wrong.getQuestionId()).orElse(null);
                    if (question != null) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("question", question);
                        map.put("wrongAnswer", wrong);
                        return map;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getStatistics(String userId) {
        List<UserProgress> allProgress = progressRepository.findByUserIdOrderByLastAnsweredAtDesc(userId);
        List<WrongAnswer> wrongAnswers = wrongAnswerRepository.findByUserIdOrderByWrongAtDesc(userId);

        int totalAnswered = allProgress.stream().mapToInt(UserProgress::getTimesAnswered).sum();
        int totalCorrect = allProgress.stream().mapToInt(UserProgress::getTimesCorrect).sum();
        double accuracy = totalAnswered > 0 ? (double) totalCorrect / totalAnswered * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", allProgress.size());
        stats.put("totalAnswered", totalAnswered);
        stats.put("totalCorrect", totalCorrect);
        stats.put("accuracy", String.format("%.2f", accuracy));
        stats.put("wrongCount", wrongAnswers.size());
        stats.put("favoriteCount", progressRepository.findByUserIdAndIsFavoriteTrue(userId).size());

        return stats;
    }

    public void removeWrongAnswer(String userId, String questionId) {
        wrongAnswerRepository.deleteByUserIdAndQuestionId(userId, questionId);
    }

    public void deleteQuestion(String userId, String questionId) {
        wrongAnswerRepository.deleteByUserIdAndQuestionId(userId, questionId);
        progressRepository.deleteByUserIdAndQuestionId(userId, questionId);
        questionRepository.deleteById(questionId);
    }

    public void deleteDocument(String userId, String documentName) {
        List<QuizQuestion> questions = questionRepository.findBySourceDocument(documentName);

        for (QuizQuestion q : questions) {
            wrongAnswerRepository.deleteByUserIdAndQuestionId(userId, q.getId());
            progressRepository.deleteByUserIdAndQuestionId(userId, q.getId());
            questionRepository.delete(q);
        }
    }
}
