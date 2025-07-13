package org.example;

import java.util.List;

public record SearchResult(
    long time,
    long totalHits,
    List<Hit> hits,
    List<TopTerms.ScoredTerm> topTerms,
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