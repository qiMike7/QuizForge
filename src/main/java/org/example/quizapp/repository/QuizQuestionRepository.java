package org.example.quizapp.repository;

import org.example.quizapp.model.QuizQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface QuizQuestionRepository extends MongoRepository<QuizQuestion, String> {

    List<QuizQuestion> findByChatIdOrderByCreatedAtDesc(String chatId);

    List<QuizQuestion> findBySourceDocument(String sourceDocument);

    void deleteByChatId(String chatId);
}
