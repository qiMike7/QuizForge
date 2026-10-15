package org.example.quizapp.repository;

import org.example.quizapp.model.WrongAnswer;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface WrongAnswerRepository extends MongoRepository<WrongAnswer, String> {

    List<WrongAnswer> findByUserIdOrderByWrongAtDesc(String userId);

    void deleteByUserIdAndQuestionId(String userId, String questionId);

    List<WrongAnswer> findByQuestionId(String questionId);
}
