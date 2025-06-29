package org.example;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopWords {

    public record ScoredWord(String word, double score) {}
    public static int limit = 200;

    PriorityQueue<ScoredWord> topTerms;

    public TopWords() {
        this.topTerms = new PriorityQueue<ScoredWord>(
            Comparator.comparingDouble(sw -> sw.score)
        );
    }

    void maybeAddWord(String word, double tfidf) {
        this.topTerms.offer(new ScoredWord(word, tfidf));
        if (this.topTerms.size() > this.limit) {
            this.topTerms.poll(); // removes the term with the lowest tfidf
        }
    }

    List<ScoredWord> getWords() {
        return this.topTerms.stream().sorted(Comparator.comparingDouble(ScoredWord::score).reversed()).toList();
    }

}
