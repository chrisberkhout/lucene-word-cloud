package com.chrisberkhout.lucenewordcloud;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    private static final String INDEX_PATH = "index";

    public static void main(String[] args) {
        try {
            Bible bible = new Bible();
            bible.load();

            Analyzer analyzer = AnalyzerBuilder.build();
            FSDirectory dir = FSDirectory.open(Paths.get(INDEX_PATH));

            new IndexBuilder(analyzer, dir).build(bible);

            Searcher searcher = new Searcher(analyzer, dir);

            new Server(searcher, bible.getVersesPerBook()).start();
        } catch (IOException e) {
            System.err.println("An I/O error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

}