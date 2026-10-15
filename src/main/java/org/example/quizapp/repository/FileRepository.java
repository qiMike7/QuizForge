package org.example.quizapp.repository;

import org.example.quizapp.model.UploadedFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FileRepository extends MongoRepository<UploadedFile, String> {

    List<UploadedFile> findByChatIdOrderByUploadTimeDesc(String chatId);

    void deleteByChatId(String chatId);
}
