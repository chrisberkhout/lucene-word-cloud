package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class WEB {

    public static void load() {
        try {
            InputStream input = WEB.class.getClassLoader().getResourceAsStream("pg8294.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            HashMap<Integer,String> bookNames = new HashMap<>();

            ArrayList<ArrayList> books = new ArrayList<>();

            String line;
            ArrayList<ArrayList> book = null;
            ArrayList<String> chapter = null;
            String verse = null;
            int chapterNumber;
            int verseNumber;
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
                    int bookNumber = Integer.parseInt(parts[1]);
                    String bookName = parts[2];

                    book = new ArrayList<ArrayList>();
                    books.add(book);
                    assert books.size() == bookNumber;

                    bookNames.put(bookNumber, bookName);
                    assert bookNames.size() == bookNumber;

                } else if (line.startsWith(" ")) {
                    // continuation of a verse

                    verse += " " + line.substring(8);
                    chapter.set(chapter.size()-1, verse);

                } else {
                    // start of a verse

                    chapterNumber = Integer.parseInt(line.substring(0,3));
                    verseNumber = Integer.parseInt(line.substring(4,7));
                    verse = line.substring(8);

                    if (book.size() < chapterNumber) {
                        // new chapter
                        chapter = new ArrayList<String>();
                        book.add(chapter);
                    }
                    chapter.add(verse);
                    assert book.size() == chapterNumber;
                    assert chapter.size() == verseNumber;

                }

                line = reader.readLine();
            }

            // books
            System.out.println("done");

            // TODO: remove verse notes (between curly braces)

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
