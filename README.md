# Lucene Word Cloud

A Lucene demo app that indexes the World English Bible by verse.

It provides full text search using Lucene's standard query parser syntax. The
top matching verses are returned (scored by the default BM25 metric), along
with a total count of hits and facet counts by book.

The index includes term vectors that are used to produce a list of top terms
(scored by TF-IDF) from all matching verses, which are presented as a word
cloud visualization.

## Run it

To build the index and start the server, run:

```
./gradlew run
```

Interact with it by opening the page [http://localhost:7070](http://localhost:7070).

The Lucene index will be written to `./index/` and can be inspected with
Lucene's [Luke](https://github.com/apache/lucene/tree/main/lucene/luke) GUI
tool.

![Demo](demo.gif)

## Discussion

### Analysis and top terms

To show an interesting world cloud visualization that gives a sense of the
topics in the selected documents, we want to combine variations of one word
(using stemming or lemmatization), score them based on importance rather than
raw frequency (using TF-IDF), and exclude some words entirely (using a
stopwords list).

I experimented with several analyzer and scoring configurations.

<details>
<summary>Expand for a discussion of analysis and scoring configurations.</summary>

With the standard Lucene analyzer and TF-IDF by verse, the top terms and their
scores were:

```
you      19383
of       19016
the      17608
to       17166
and      16940
in       15067
he       14804
i        14446
shall    14251
your     13789
```

A custom analyzer with the standard tokenizer and a selection of built-in token
filters (`lowercase`, `englishpossessive`, `stop`, `kstem`) started to produce
more interesting results:

```
you      19383
he       14804
i        14446
shall    14251
your     13789
his      13644
him      12042
yahweh   11494
who      11423
them     11118
```

A TF-IDF where the IDF is calculated using chapters rather than verses is much
better at identifying more common words and reducing their score. That is
because verses are very short. Even very common words are not present in most
verses, but they are present in most chapters.

```
shall     3433
yahweh    2873
king      2675
her       2344
said      2310
offering  2031
son       2010
i         1890
my        1867
me        1843
```

An expanded stopwords list ([Stopwords ISO](https://github.com/stopwords-iso/stopwords-iso))
improves it further, removing pronouns and other common words entirely.

```
yahweh    2873
king      2675
offering  2031
son       2010
israel    1838
david     1733
jesus     1696
father    1542
house     1498
children  1496
```

Switching back to verses rather than chapters but keeping the expanded
stopwords list yields decent results. Using verses is more convenient, because
for search we want to find individual verses, so we do need that index. One
loss here is that names such as David, Jesus, Moses and Saul are ranked lower.

```
yahweh   11494
god       8904
son       8614
king      7083
israel    6721
house     6189
people    5950
father    5671
hand      5491
children  5476
```

Switching back to the standard analyzer but keeping the expanded stopwords
list is a bit worse than the customer analyzer. For example, some terms are
split into singular and plural forms and receive reduced scores (son, king).

```
yahweh   11330
god       8748
son       6764
israel    6701
king      6506
house     5724
people    5531
children  5431
land      5328
day       5180
...
sons      3866
...
kings     1528
```

</details>

The final choice was to use a custom analyzer with the token filters
`lowercase`, `englishpossessive`, `kstem` and `stop` (with the large Stopwords
ISO list). The TF-IDF score is calculated by verse.

This produces good results for the word cloud, but filtering common words out
of the index and queries makes it harder to find exact phrases. The search
experience could be improved by removing the large stopwords list from the
analyzer and applying it only to the word cloud.

### Top term calculations

To calculate top terms by TF-IDF for the word cloud, we need to know for each
term how many times it appears and in how many documents, and we need to know
the total number of documents.

For global results, we can use numbers from the Lucene index's segment
information and terms dictionary.

For results specific to documents matching a query, we can add term vectors to
the index, and read them to collect term information for the TF-IDF calculation
from each matching document.

### Querying, search and collection

The [`StandardQueryParser`](https://lucene.apache.org/core/10_2_1/queryparser/org/apache/lucene/queryparser/flexible/standard/StandardQueryParser.html)'s
syntax allows for flexible queries.

To search for top documents, a total hit count and facet counts, the main
search is run via `FacetsCollectorManager.search`.

A separate search is performed via `IndexSearcher#search`, with a custom
collector that visits all hits without scoring, and collects word cloud
information from the term vectors.

A potential optimization would be to use a `MultiCollector` to combine the
facet count collection and the custom term vector collection into a single
pass.

### Word cloud presentation

The word cloud layout is calculated by [`d3-cloud`](https://github.com/jasondavies/d3-cloud)
and the results are rendered using [D3](https://d3js.org/).
