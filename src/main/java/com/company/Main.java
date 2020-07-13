package com.company;

import com.company.webcrawler.WebCrawler;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));) {
            System.out.println("Enter seed URL");
            String seedURL = reader.readLine();
            System.out.println("Enter terms comma separated without spaces");
            Set<String> terms = new HashSet<>(Arrays.asList(reader.readLine().split(",")));
            System.out.println("Enter link depth");
            int linkDepth = Integer.parseInt(reader.readLine());
            System.out.println("Enter max visited pages limit");
            int maxVisitedPagesLimit = Integer.parseInt(reader.readLine());

            WebCrawler webCrawler = new WebCrawler(seedURL, terms, linkDepth, maxVisitedPagesLimit);
            webCrawler.calculateStats();

            webCrawler.serializeAllStatsToCSV("all_stats.csv");
            int numTopPages = 10;
            System.out.println("Top " + numTopPages + " pages by total hints");
            webCrawler.printTopPages(numTopPages);
            webCrawler.serializeTopPagesToCSV("top_10_pages_stats.csv", numTopPages);
        } catch (IOException | NumberFormatException exception) {
            System.out.println(exception.getClass() + " " + exception.getMessage());
        }
    }
}
