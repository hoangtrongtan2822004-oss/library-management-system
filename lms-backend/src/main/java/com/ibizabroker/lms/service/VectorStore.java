package com.ibizabroker.lms.service;

import java.util.List;
import java.util.Map;

public interface VectorStore {
    void upsert(String id, List<Double> vector, Map<String, Object> metadata);
    List<String> query(List<Double> vector, int topK);
    default void delete(String id) {}
}
