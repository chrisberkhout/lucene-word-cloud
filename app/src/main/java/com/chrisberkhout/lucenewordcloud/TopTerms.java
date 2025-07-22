package com.chrisberkhout.lucenewordcloud;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopTerms {

    public record ScoredTerm(String term, double score) implements Comparable<ScoredTerm> {
        @Override
        public int compareTo(ScoredTerm other) {
            return Double.compare(this.score, other.score);
        }
    }

    private final int limit;
    private final PriorityQueue<ScoredTerm> topTerms;

    TopTerms(int n) {
        this.limit = n;
        this.topTerms = new PriorityQueue<>();
    }

    public void offerTerm(String term, double tf, double df, int numDocs) {
        double idf = Math.log((numDocs + 1.0) / (df + 1.0));
        double tfidf = tf * idf;
        this.topTerms.offer(new ScoredTerm(term, tfidf));
        if (this.topTerms.size() > this.limit) {
            this.topTerms.poll(); // removes the term with the lowest tfidf
        }
    }

    public List<ScoredTerm> getTerms() {
        return this.topTerms.stream().sorted(Comparator.comparingDouble(ScoredTerm::score).reversed()).toList();
    }

}
