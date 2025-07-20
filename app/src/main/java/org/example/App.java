package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    private static final String indexPath = "index";

    public static void main(String[] args) throws IOException {
        Bible bible = new Bible();
        bible.load();

        Analyzer analyzer  = AnalyzerBuilder.build();
        FSDirectory dir = FSDirectory.open(Paths.get(indexPath));

        new IndexBuilder(analyzer, dir).build(bible);

        Searcher searcher = new Searcher(analyzer, dir);

        new Server(searcher).start();
    }

}