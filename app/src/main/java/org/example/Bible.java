package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Bible {
    public record Verse(String bookName, int book, int chapter, int verse, String text) {}
    public record Chapter(String bookName, int book, int chapter, String text) {}

    private final Map<Integer, String> bookNames = new HashMap<>();
    private final ArrayList<Verse> verses = new ArrayList<>();

    public List<Verse> getVerses() {
        return Collections.unmodifiableList(verses);
    }

    public List<Chapter> getChapters() {
        if (this.verses.isEmpty()) {
            return List.of();
        }

        List<Chapter> chapters = new ArrayList<>();

        Verse first = this.verses.getFirst();
        String lastBookName = first.bookName();
        int lastBook = first.book();
        int lastChapter = first.chapter();
        StringBuilder lastText = new StringBuilder(first.text());

        for (int i = 1; i < this.verses.size(); i++) {
            Verse v = this.verses.get(i);
            if (v.chapter() == lastChapter && v.book() == lastBook) {
                // the chapter continues
                lastText.append(" ");
                lastText.append(v.text());
            } else {
                // it's a new chapter - add last chapter
                chapters.add(new Chapter(lastBookName, lastBook, lastChapter, lastText.toString()));
                // start the new chapter
                lastBookName = v.bookName();
                lastBook = v.book();
                lastChapter = v.chapter();
                lastText = new StringBuilder(v.text());
            }
        }

        chapters.add(new Chapter(lastBookName, lastBook, lastChapter, lastText.toString()));

        return Collections.unmodifiableList(chapters);
    }

    public void load() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        Bible.class.getClassLoader().getResourceAsStream("pg8294.txt"),
                        StandardCharsets.UTF_8
                ))
        ) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}