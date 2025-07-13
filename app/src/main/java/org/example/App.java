package org.example;

import io.javalin.Javalin;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    private static String indexPath = "index";

    public static void main(String[] args) {
        Searcher searcher;
        try {

            Bible bible = new Bible();
            bible.load();

            Analyzer analyzer  = AnalyzerBuilder.build();
            FSDirectory dir = FSDirectory.open(Paths.get(indexPath));

            new IndexBuilder(analyzer, dir).build(bible);

            searcher = new Searcher(analyzer, dir);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Javalin.create(config -> {
            config.staticFiles.add("/public");
        })
        .get("/search", ctx -> {
            String q = ctx.queryParam("q");
            SearchResult qr = searcher.query(q);
            ctx.json(qr);
        })
        .start(7070);
    }

}