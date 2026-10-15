package org.example.quizapp.dto.request;

public class QuestionGenerateRequest {
    private String text;
    private String sourceDoc;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSourceDoc() {
        return sourceDoc;
    }

    public void setSourceDoc(String sourceDoc) {
        this.sourceDoc = sourceDoc;
    }
}
