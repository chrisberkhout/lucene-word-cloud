package org.example;

import java.util.List;

public record QueryResult(
    long time,
    long totalHits,
    List<Hit> hits,
    List<TopWords.ScoredWord> topWords
) {
    public record Hit(
        double score,
        String book,
        long chapter,
        long verse,
        String text
    ) {}
}