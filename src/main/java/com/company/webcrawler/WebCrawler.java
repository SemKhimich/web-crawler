package com.company.webcrawler;

import com.company.support.Pair;
import com.company.trie.Trie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for crawling web pages and collecting terms statistics
 */
public class WebCrawler {
    /**
     * Page from which starts collection of statistics
     */
    private String seedURL;

    /**
     * Words to search for on pages
     */
    private Set<String> terms;

    private int linkDepth;

    /**
     * Maximum number of pages that can be analyzed
     */
    private int maxVisitedPagesLimit;

    public static int DEFAULT_LINK_DEPTH = 8;
    public static int DEFAULT_MAX_VISITED_PAGES_LIMIT = 10000;
    public static String CSV_SEPARATOR = ",";

    private Map<String, Map<String, Integer>> pagesStats;

    /**
     * Trie for search terms on pages
     */
    private Trie trie;

    public WebCrawler(String seedURL, Set<String> terms, int linkDepth, int maxVisitedPagesLimit) {
        this.seedURL = seedURL;
        this.terms = terms;
        this.linkDepth = linkDepth;
        this.maxVisitedPagesLimit = maxVisitedPagesLimit;
        trie = new Trie(terms.stream().map(String::toLowerCase).collect(Collectors.toSet()));
        pagesStats = new HashMap<>();
    }

    public WebCrawler(String seedURL, Set<String> terms) {
        this(seedURL, terms, DEFAULT_LINK_DEPTH, DEFAULT_MAX_VISITED_PAGES_LIMIT);
    }

    public Map<String, Map<String, Integer>> getPagesStats() {
        return pagesStats;
    }

    public void calculateStats() {
        LinkedList<Pair<String, Integer>> notAnalyzedPages = new LinkedList<>();
        notAnalyzedPages.add(new Pair<>(seedURL, linkDepth));
        while (!notAnalyzedPages.isEmpty() && maxVisitedPagesLimit != 0) {
            Pair<String, Integer> currentPage = notAnalyzedPages.removeFirst();
            if (pagesStats.containsKey(currentPage.getFirst())) {
                continue;
            }
            Document document;
            try {
                document = Jsoup.connect(currentPage.getFirst()).get();
            } catch (IOException exc) {
                System.out.println("Page request failed");
                System.out.println("Requested page " + currentPage.getFirst());
                System.out.println("Exception message " + exc.getMessage());
                continue;
            }
            Element documentBody = document.body();
            pagesStats.put(currentPage.getFirst(), trie.getOccurrencesNum(documentBody.text().toLowerCase()));
            maxVisitedPagesLimit--;
            if (currentPage.getSecond() == 0) {
                continue;
            }
            addPagesToVisit(notAnalyzedPages, documentBody, currentPage.getSecond());
        }
    }

    private void addPagesToVisit(LinkedList<Pair<String, Integer>> notAnalyzedPages, Element documentBody,
                                 int currentLinkDepth) {
        for (Element element : documentBody.select("a")) {
            String hrefAttr = element.attr("href");
            if (!isSamePage(hrefAttr)) {
                continue;
            }
            notAnalyzedPages.add(new Pair<>(element.absUrl("href"), currentLinkDepth - 1));
        }
    }

    private boolean isSamePage(String hrefAttr) {
        return !(hrefAttr.isEmpty() || hrefAttr.startsWith("#"));
    }

    private void writeHeaderToCSVFile(FileWriter fileWriter) throws IOException {
        fileWriter.write("Page");
        for (String term : terms) {
            fileWriter.write(CSV_SEPARATOR + term);
        }
        fileWriter.write(CSV_SEPARATOR + "Total");
        fileWriter.write("\n");
    }

    private void writePageStatsToCSVFile(FileWriter fileWriter, String page, Map<String, Integer> pageStats) throws IOException {
        fileWriter.write(page);
        for (int value : pageStats.values()) {
            fileWriter.write(CSV_SEPARATOR + value);
        }
        fileWriter.write(CSV_SEPARATOR + getValuesSum(pageStats));
        fileWriter.write("\n");
    }

    public void serializeAllStatsToCSV(String filename) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filename)) {
            writeHeaderToCSVFile(fileWriter);
            for (var pageStats : pagesStats.entrySet()) {
                writePageStatsToCSVFile(fileWriter, pageStats.getKey(), pageStats.getValue());
            }
        }
    }

    public void serializeTopPagesToCSV(String filename, int numPages) throws IOException {
        List<Pair<String, Map<String, Integer>>> sortedPagesStats = getSortedPagesStats();
        try (FileWriter fileWriter = new FileWriter(filename)) {
            writeHeaderToCSVFile(fileWriter);
            for (var pair : sortedPagesStats.subList(0, numPages)) {
                writePageStatsToCSVFile(fileWriter, pair.getFirst(), pair.getSecond());
            }
        }
    }

    private int getValuesSum(Map<String, Integer> map) {
        return map.values().stream().reduce(0, Integer::sum);
    }

    /**
     * Sorting pages by total hints
     *
     * @return
     */
    public List<Pair<String, Map<String, Integer>>> getSortedPagesStats() {
        return pagesStats.entrySet().stream().map((entry) -> new Pair<>(entry.getKey(), entry.getValue()))
                .sorted(((p1, p2) -> getValuesSum(p2.getSecond()) - getValuesSum(p1.getSecond())
                )).collect(Collectors.toList());
    }

    /**
     * Print statistics of pages with the most total hints to the console
     *
     * @param numPages
     */
    public void printTopPages(int numPages) {
        System.out.print("Page");
        terms.forEach((term) -> System.out.print(" " + term));
        System.out.println(" Total");
        List<Pair<String, Map<String, Integer>>> sortedPagesStats = getSortedPagesStats();
        sortedPagesStats.subList(0, numPages).forEach((pair -> {
            System.out.print(pair.getFirst());
            pair.getSecond().values().forEach((value) -> System.out.print(" " + value));
            int totalHits = getValuesSum(pair.getSecond());
            System.out.println(" " + totalHits);
        }));
    }
}
