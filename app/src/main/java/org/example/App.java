package org.example;

import java.util.List;

public class App {
    public static void main(String[] args) {
        Bible bible = new Bible();
        bible.load();
        SectionIndex index = new SectionIndex();
        index.build(bible);
//        for (TopWords.ScoredWord w : index.getScoredWords()) {
//            System.out.println(w.score() + "  " + w.word());
//        }
        long start = System.nanoTime();
        List<TopWords.ScoredWord> tw = index.query();
        long end = System.nanoTime();
        for (TopWords.ScoredWord w : tw) {
            System.out.println(w.score() + "  " + w.word());
        }
        System.out.println("");
        System.out.println("query time: " + ((end-start) / 1_000_000) + " ms");
    }
}
