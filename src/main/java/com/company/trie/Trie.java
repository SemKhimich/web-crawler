package com.company.trie;

import com.company.support.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class Trie which implements the Aho-Corasick algorithm
 * to search for multiple substrings in the text
 */
public class Trie {

    /**
     * Class TrieNode describes nodes which are located in the Trie
     */
    public class TrieNode {
        /**
         * Suffix link of node
         */
        private TrieNode suffixLink;

        /**
         * Terminal link of node
         */
        private TrieNode terminalLink;

        /**
         * Store transitions to the node's children
         */
        private Map<Character, TrieNode> trieTransitions;

        /**
         * Number of the word that corresponds to the or
         * -1 if node doesn't correspond any word in the dictionary
         */
        private int wordId;

        /**
         * Constructor - creating a new object
         */
        public TrieNode() {
            wordId = -1;
            suffixLink = null;
            terminalLink = null;
            trieTransitions = new HashMap<>();
        }

        /**
         * Function that returns node's child that corresponds the symbol or
         * null if there isn't child which corresponds the symbol
         *
         * @param sym - symbol
         * @return return node's son or null
         */
        TrieNode getTrieTransition(char sym) {
            if (trieTransitions.containsKey(sym)) {
                return trieTransitions.get(sym);
            }
            return null;
        }
    }

    /**
     * Trie root
     */
    private TrieNode root;
    /**
     * Trie words list
     */
    private List<String> words;

    /**
     * Constructor - creating a new object
     *
     * @param words - words to search for in the text
     */
    public Trie(Set<String> words) {
        root = new TrieNode();
        this.words = new ArrayList<>(words);
        buildNodes();
        buildSuffixLinks();
        buildTerminalLinks();
    }

    /**
     * Function for traversing the structure by characters from the text
     *
     * @param currentNode - node where we are located
     * @param character   - symbol for moving to next node
     * @return return next node
     */
    private TrieNode getNextNode(TrieNode currentNode, char character) {
        TrieNode nextNode = currentNode.getTrieTransition(character);
        if (nextNode != null) {
            return nextNode;
        }
        nextNode = currentNode.suffixLink;
        while (nextNode.getTrieTransition(character) == null && nextNode != root) {
            nextNode = nextNode.suffixLink;
        }
        TrieNode transition = nextNode.getTrieTransition(character);
        if (transition == null) {
            return root;
        }
        return transition;
    }

    /**
     * Function that updates the number of words occurrences in the text
     *
     * @param occurrences - map that stores the current number of words occurrences
     * @param node        - node where we are located
     */
    private void checkOccurrences(Map<String, Integer> occurrences, TrieNode node) {
        while (node != root) {
            if (node.wordId != -1) {
                String word = words.get(node.wordId);
                occurrences.put(word, occurrences.get(word) + 1);
            }
            node = node.terminalLink;
        }
    }

    /**
     * Function for counting the number of occurrences of words in the text
     *
     * @param text - text
     * @return map that stores number of occurrences for each word
     */
    public Map<String, Integer> getOccurrencesNum(String text) {
        Map<String, Integer> occurrences = new HashMap<>();
        words.forEach((word) -> occurrences.put(word, 0));
        TrieNode currentNode = root;
        for (int i = 0; i < text.length(); i++) {
            currentNode = getNextNode(currentNode, text.charAt(i));
            checkOccurrences(occurrences, currentNode);
        }
        return occurrences;
    }

    /**
     * Function for building trie's nodes
     */
    private void buildNodes() {
        for (int i = 0; i < words.size(); i++) {
            addWordToTrie(words.get(i), i);
        }
    }

    /**
     * Function for adding word to trie
     *
     * @param word   - word from dictionary
     * @param wordId
     */
    private void addWordToTrie(String word, int wordId) {
        if (word.isEmpty()) {
            return;
        }
        TrieNode currentNode = root;
        for (int i = 0; i < word.length(); i++) {
            TrieNode nextNode = currentNode.getTrieTransition(word.charAt(i));
            if (nextNode != null) {
                currentNode = nextNode;
            } else {
                TrieNode newNode = new TrieNode();
                currentNode.trieTransitions.put(word.charAt(i), newNode);
                currentNode = newNode;
            }
        }
        currentNode.wordId = wordId;
    }

    /**
     * Functional for finding suffix link of node's son
     *
     * @param parentNode - parent node
     * @param character
     * @return child's suffix link
     */
    private TrieNode getChildSuffixLink(TrieNode parentNode, char character) {
        if (parentNode == root) {
            return root;
        }
        TrieNode parentSuffixLink = parentNode.suffixLink;
        while (parentSuffixLink != root && parentSuffixLink.getTrieTransition(character) == null) {
            parentSuffixLink = parentSuffixLink.suffixLink;
        }
        TrieNode suffixLink = parentSuffixLink.getTrieTransition(character);
        if (suffixLink == null) {
            return root;
        }
        return suffixLink;
    }

    private void addTransitionsForCalculationSuffixLinks(LinkedList<Pair<TrieNode, Character>> notCheckedTransitions,
                                                         TrieNode parent) {
        notCheckedTransitions.addAll(
                parent.trieTransitions.keySet().stream()
                        .map(character -> new Pair<>(parent, character)).collect(Collectors.toList()));
    }

    /**
     * Function for building suffix links of nodes
     */
    private void buildSuffixLinks() {
        root.suffixLink = root;
        LinkedList<Pair<TrieNode, Character>> notCheckedTransitions = new LinkedList<>();
        addTransitionsForCalculationSuffixLinks(notCheckedTransitions, root);
        while (!notCheckedTransitions.isEmpty()) {
            Pair<TrieNode, Character> transition = notCheckedTransitions.removeFirst();
            TrieNode parentNode = transition.getFirst();
            char character = transition.getSecond();
            TrieNode currentNode = parentNode.getTrieTransition(character);
            currentNode.suffixLink = getChildSuffixLink(parentNode, character);
            addTransitionsForCalculationSuffixLinks(notCheckedTransitions, currentNode);
        }
    }

    /**
     * Function which calculates node's terminal link
     *
     * @param node - node
     */
    private void calculateTerminalLink(TrieNode node) {
        if (node.suffixLink.wordId != -1) {
            node.terminalLink = node.suffixLink;
        } else {
            node.terminalLink = node.suffixLink.terminalLink;
        }
    }

    /**
     * Function for building terminal links of nodes
     */
    private void buildTerminalLinks() {
        root.terminalLink = root;
        LinkedList<TrieNode> nodes = new LinkedList<>(root.trieTransitions.values());
        while (!nodes.isEmpty()) {
            TrieNode currentNode = nodes.removeFirst();
            calculateTerminalLink(currentNode);
            nodes.addAll(currentNode.trieTransitions.values());
        }
    }
}
