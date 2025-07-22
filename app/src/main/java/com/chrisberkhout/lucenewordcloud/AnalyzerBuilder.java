package com.chrisberkhout.lucenewordcloud;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AnalyzerBuilder {

    public static Analyzer build() throws IOException {
        return CustomAnalyzer.builder()
            .withTokenizer("standard")
            .addTokenFilter("lowercase")
            .addTokenFilter("englishpossessive")
            .addTokenFilter("stop", new HashMap<>(Map.of(
                "format", "wordset",
                "ignoreCase", "true",
                "words", "stopwords-en.txt" // https://github.com/stopwords-iso/stopwords-iso
            )))
            .addTokenFilter("kstem")
            .build();
    }

}
