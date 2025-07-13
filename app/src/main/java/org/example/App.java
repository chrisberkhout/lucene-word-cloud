package org.example;

import io.javalin.Javalin;

public class App {
    public static void main(String[] args) {
        Bible bible = new Bible();
        bible.load();
        SectionIndex index = new SectionIndex();
        index.build(bible);
//        long start = System.nanoTime();
//        List<TopWords.ScoredWord> tw = index.query();
//        long end = System.nanoTime();
//        for (TopWords.ScoredWord w : tw) {
//            System.out.println(w.score() + "  " + w.word());
//        }
//        System.out.println("");
//        System.out.println("query time: " + ((end-start) / 1_000_000) + " ms");

        var app = Javalin.create(config -> {
                config.staticFiles.add("/public");
            })
            .get("/search", ctx -> {
                String q = ctx.queryParam("q");
                QueryResult qr = index.query(q);
                ctx.json(qr);
            })
            .start(7070);
    }
}
