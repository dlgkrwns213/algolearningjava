package com.learning.algolearningjava.repository;

import com.learning.algolearningjava.model.RoomDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomDocumentRepository extends MongoRepository<RoomDocument, String> {
}
