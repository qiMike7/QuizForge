package org.example.quizapp.repository;

import org.example.quizapp.model.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    Optional<UserProgress> findByUserIdAndQuestionId(String userId, String questionId);

    List<UserProgress> findByUserIdAndIsFavoriteTrue(String userId);

    List<UserProgress> findByUserIdOrderByLastAnsweredAtDesc(String userId);

    void deleteByUserIdAndQuestionId(String userId, String questionId);
}
