package com.company.trie;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class TrieTest {
    private String generateString(Random random, int len, int alphabetSize) {
        StringBuilder builder = new StringBuilder();
        for (int j = 0; j < len; j++) {
            int m = random.nextInt(1000) % alphabetSize + 'a';
            char sym = (char) m;
            builder.append(sym);
        }
        return builder.toString();
    }

    private Set<String> generateTerms(Random random, int termsNum, int alphabetSize, int termMaxLen) {
        Set<String> terms = new HashSet<>();
        for (int i = 0; i < termsNum; i++) {
            int termLen = random.nextInt(100) % termMaxLen + 1;
            terms.add(generateString(random, termLen, alphabetSize));
        }
        return terms;
    }

    private boolean isCorrect(Set<String> terms, String text) {
        Trie trie = new Trie(terms);
        Map<String, Integer> trieResult = trie.getOccurrencesNum(text);
        Map<String, Integer> simpleAlgorithmResult = getOccurrencesNumSimpleAlgorithm(text, terms);
        return trieResult.equals(simpleAlgorithmResult);
    }

    @Test
    public void randomTests() {
        Random random = new Random();
        int alphabetSize = 5;
        int numTests = 50000;
        for (int iteration = 0; iteration < numTests; iteration++) {
            if (iteration % 10000 == 0) {
                System.out.println("iteration: " + iteration);
            }
            int termsNum = random.nextInt(100) % 100 + 5;
            Set<String> terms = generateTerms(random, termsNum, alphabetSize, 10);
            int textLen = random.nextInt(100) % 9000 + 500;
            String text = generateString(random, textLen, alphabetSize);
            Assert.assertTrue(isCorrect(terms, text));
        }
    }

    @Test(timeout = 10000)
    public void stressTest() {
        Random random = new Random();
        int termsNum = 10000;
        Set<String> terms = generateTerms(random, termsNum, 3, 10000);
        int texLen = 10000000;
        String text = generateString(random, texLen, 3);
        long firstTime = System.currentTimeMillis();
        Trie trie = new Trie(terms);
        int totalHints = trie.getOccurrencesNum(text).values().stream().reduce(0, Integer::sum);
        long secondTime = System.currentTimeMillis();
        System.out.println("total hints: " + totalHints);
        System.out.println("trie operating time " + (secondTime - firstTime) / 1000.0);
    }

    private Map<String, Integer> getOccurrencesNumSimpleAlgorithm(String text, Set<String> words) {
        Map<String, Integer> occurrences = new HashMap<>();
        for (String word : words) {
            int index = 0;
            int matchNum = 0;
            index = text.indexOf(word, index);
            while (index != -1) {
                matchNum++;
                index++;
                index = text.indexOf(word, index);
            }
            occurrences.put(word, matchNum);
        }
        return occurrences;
    }
}
