package org.example.quizapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "wrong_answers")
public class WrongAnswer {

    @Id
    private String id;

    private String userId;

    private String questionId;

    private Integer userSelectedAnswer;

    private Integer correctAnswer;

    private LocalDateTime wrongAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public Integer getUserSelectedAnswer() {
        return userSelectedAnswer;
    }

    public void setUserSelectedAnswer(Integer userSelectedAnswer) {
        this.userSelectedAnswer = userSelectedAnswer;
    }

    public Integer getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(Integer correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public LocalDateTime getWrongAt() {
        return wrongAt;
    }

    public void setWrongAt(LocalDateTime wrongAt) {
        this.wrongAt = wrongAt;
    }
}
