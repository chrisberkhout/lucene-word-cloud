package org.example;

import java.util.List;

public record QueryResult(
    long time,
    long totalHits,
    List<Hit> hits,
    List<TopTerms.ScoredTerm> topWords,
    int[] hitsByBook
) {
    public record Hit(
        double score,
        String book,
        long chapter,
        long verse,
        String text
    ) {}
}