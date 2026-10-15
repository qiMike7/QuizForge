package org.example.quizapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "user_progress")
public class UserProgress {

    @Id
    private String id;

    private String userId;

    private String questionId;

    private Boolean isFavorite;

    private Integer timesAnswered;

    private Integer timesCorrect;

    private LocalDateTime lastAnsweredAt;

    private LocalDateTime createdAt;

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

    public Boolean getFavorite() {
        return isFavorite;
    }

    public void setFavorite(Boolean favorite) {
        isFavorite = favorite;
    }

    public Integer getTimesAnswered() {
        return timesAnswered;
    }

    public void setTimesAnswered(Integer timesAnswered) {
        this.timesAnswered = timesAnswered;
    }

    public Integer getTimesCorrect() {
        return timesCorrect;
    }

    public void setTimesCorrect(Integer timesCorrect) {
        this.timesCorrect = timesCorrect;
    }

    public LocalDateTime getLastAnsweredAt() {
        return lastAnsweredAt;
    }

    public void setLastAnsweredAt(LocalDateTime lastAnsweredAt) {
        this.lastAnsweredAt = lastAnsweredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
