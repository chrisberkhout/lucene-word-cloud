package com.chrisberkhout.lucenewordcloud;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.search.SimpleCollector;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


class TermFrequenciesCollector extends SimpleCollector {
    private final Map<String, Frequencies> termFrequencies = new HashMap<>();
    private final IndexReader reader;
    private int docBase;
    private int totalHits = 0;

    TermFrequenciesCollector(IndexReader reader) {
        this.reader = reader;
    }

    public Map<String, Frequencies> getTermFrequencies() {
        return this.termFrequencies;
    }

    public int getTotalHits() {
        return this.totalHits;
    }

    @Override
    protected void doSetNextReader(LeafReaderContext context) {
        this.docBase = context.docBase;
    }

    @Override
    public void collect(int doc) throws IOException {
        this.totalHits++;
        int globalDocId = docBase + doc;
        Terms terms = reader.termVectors().get(globalDocId, "text");
        if (terms != null) {
            TermsEnum termsEnum = terms.iterator();
            BytesRef term;
            while ((term = termsEnum.next()) != null) {
                String termStr = term.utf8ToString();
                Frequencies c = termFrequencies.computeIfAbsent(termStr, k -> new Frequencies(0,0));
                c.total += termsEnum.totalTermFreq();
                c.docs += 1;
            }
        }
    }

    @Override
    public ScoreMode scoreMode() {
        return ScoreMode.COMPLETE_NO_SCORES;
    }

    static class Frequencies {
        long total;
        long docs;
        Frequencies(long total, long docs) {
            this.total = total;
            this.docs = docs;
        }
    }
}