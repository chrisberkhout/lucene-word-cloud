package org.example;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopTerms {

    public record ScoredTerm(String term, double score) {}
    public static int limit = 200;

    PriorityQueue<ScoredTerm> topTerms;

    public TopTerms() {
        this.topTerms = new PriorityQueue<ScoredTerm>(
            Comparator.comparingDouble(sw -> sw.score)
        );
    }

    void maybeAddTerm(String term, double tf, double df, int numDocs) {
        double idf = Math.log((numDocs + 1.0) / (df + 1.0));
        double tfidf = tf * idf;
        this.topTerms.offer(new ScoredTerm(term, tfidf));
        if (this.topTerms.size() > this.limit) {
            this.topTerms.poll(); // removes the term with the lowest tfidf
        }
    }

    List<ScoredTerm> getTerms() {
        return this.topTerms.stream().sorted(Comparator.comparingDouble(ScoredTerm::score).reversed()).toList();
    }

}
