package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Bible {

    public record Verse(String bookName, int book, int chapter, int verse, String text) {}

    private final Map<Integer, String> bookNames = new HashMap<>();
    private final ArrayList<Verse> verses = new ArrayList<>();
    private final int[] versesPerBook = new int[66];

    public List<Verse> getVerses() {
        return Collections.unmodifiableList(verses);
    }

    public int[] getVersesPerBook() {
        return versesPerBook;
    }

    public void load() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            Objects.requireNonNull(Bible.class.getClassLoader().getResourceAsStream("pg8294.txt")),
            StandardCharsets.UTF_8
        ));

        String line;
        String bookName = null;
        int book = 0;
        int chapter = 0;
        int verse = 0;

        while ((line = reader.readLine()) != null) {
            // advance to the content
            if (line.startsWith("Book ")) break;
        }

        while (line != null) {
            if (line.length() == 0) {
                // end of content
                break;
            } else if (line.startsWith("Book ")) {
                // start of a book
                String[] parts = line.split(" ", 3);
                book = Integer.parseInt(parts[1]);
                bookName = parts[2];
                this.bookNames.put(book, bookName);
            } else if (line.startsWith(" ")) {
                // continuation of a verse
                Verse v = this.verses.removeLast();
                String text = String.join(" ", v.text, line.substring(8));
                this.verses.add(new Verse(v.bookName, v.book, v.chapter, v.verse, text));
            } else {
                // start of a verse
                chapter = Integer.parseInt(line.substring(0, 3));
                verse = Integer.parseInt(line.substring(4, 7));
                String text = line.substring(8);
                this.verses.add(new Verse(bookName, book, chapter, verse, text));
                this.versesPerBook[book-1]++;
            }
            line = reader.readLine();
        }

        for (int i = 0; i < verses.size(); i++) {
            Verse v = verses.get(i);
            // strip notes
            if (v.text.contains("{")) {
                String cleanText = v.text.replaceAll("\\{[^}]*\\}", "");
                v = new Verse(v.bookName, v.book, v.chapter, v.verse, cleanText);
                verses.set(i, v);
            }
        }
    }

}