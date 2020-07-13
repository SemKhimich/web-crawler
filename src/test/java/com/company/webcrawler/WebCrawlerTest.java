package com.company.webcrawler;

import com.company.support.Pair;
import com.company.trie.Trie;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Jsoup.class, WebCrawler.class})
public class WebCrawlerTest {
    @Mock
    private Connection connection;
    @Mock
    private Document document;
    @Mock
    private Element documentBody;
    @Mock
    private Elements elements;
    @Mock
    private Element element;
    @Mock
    private Trie trie;
    @Mock
    private Iterator<Element> elementIterator;

    private final static Map<String, Integer> OCCURRENCE_NUM_FIRST_MAP;
    private final static Map<String, Integer> OCCURRENCE_NUM_SECOND_MAP;
    private final static List<String> URLS = Arrays.asList("url1", "url2", "url3", "url4");
    private final static List<String> TEXTS = Arrays.asList("text1", "text2", "text3");
    private final static String RELEVANT_HREF_ATTR = "/a";
    private final static String NOT_RELEVANT_HREF_ATTR = "#";
    private final static String EXCEPTION_MESSAGE = "exception message";

    static {
        OCCURRENCE_NUM_FIRST_MAP = new HashMap<>();
        OCCURRENCE_NUM_SECOND_MAP = new HashMap<>();
        OCCURRENCE_NUM_FIRST_MAP.put("key1", 2);
        OCCURRENCE_NUM_FIRST_MAP.put("key2", 2);
        OCCURRENCE_NUM_SECOND_MAP.put("key1", 1);
        OCCURRENCE_NUM_SECOND_MAP.put("key2", 4);
    }

    @Before
    public void setup() throws Exception {
        PowerMockito.mockStatic(Jsoup.class);
        PowerMockito.when(Jsoup.connect(Mockito.anyString())).thenReturn(connection);
        PowerMockito.whenNew(Trie.class).withAnyArguments().thenReturn(trie);
        when(connection.get()).thenReturn(document);
        when(document.body()).thenReturn(documentBody);
        when(documentBody.text()).thenReturn(TEXTS.get(0));
        when(documentBody.select("a")).thenReturn(elements);
        when(elements.iterator()).thenReturn(elementIterator);
        when(trie.getOccurrencesNum(TEXTS.get(0))).thenReturn(OCCURRENCE_NUM_FIRST_MAP);
    }

    private void mockElementIterator(int linksNum) {
        if (linksNum == 0) {
            when(elementIterator.hasNext()).thenReturn(false);
            return;
        }
        var y = when(elementIterator.hasNext()).thenReturn(true);
        for (int i = 0; i < linksNum - 1; i++) {
            y.thenReturn(true);
        }
        y.thenReturn(false);
        when(elementIterator.next()).thenReturn(element);
    }

    private void mockElement(String hrefAttr, String absUrl) {
        when(element.attr("href")).thenReturn(hrefAttr);
        when(element.absUrl("href")).thenReturn(absUrl);
    }

    @Test
    public void testSeedPageWithoutLinks() {
        mockElementIterator(0);
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testSeedPageWithNotRelevantLink() {
        mockElementIterator(1);
        mockElement(NOT_RELEVANT_HREF_ATTR, URLS.get(1));
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testSeedPageWithRelevantLink() {
        mockElementIterator(2);
        mockElement(RELEVANT_HREF_ATTR, URLS.get(1));
        when(documentBody.text()).thenReturn(TEXTS.get(0)).thenReturn(TEXTS.get(1));
        when(trie.getOccurrencesNum(TEXTS.get(0))).thenReturn(OCCURRENCE_NUM_FIRST_MAP);
        when(trie.getOccurrencesNum(TEXTS.get(1))).thenReturn(OCCURRENCE_NUM_SECOND_MAP);
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        rightResult.put(URLS.get(1), OCCURRENCE_NUM_SECOND_MAP);
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testGettingToAnalyzedPage() throws IOException {
        mockElementIterator(1);
        mockElement(RELEVANT_HREF_ATTR, URLS.get(0));
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        verify(connection, times(1)).get();
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testLinkDepth() {
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet(),
                0, WebCrawler.DEFAULT_MAX_VISITED_PAGES_LIMIT);
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        verify(documentBody, times(0)).select("a");
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testMaxVisitedPagesLimit() {
        mockElementIterator(1);
        mockElement(RELEVANT_HREF_ATTR, URLS.get(1));
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet(),
                WebCrawler.DEFAULT_LINK_DEPTH, 1);
        webCrawler.calculateStats();
        Map<String, Map<String, Integer>> rightResult = new HashMap<>();
        rightResult.put(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP);
        Assert.assertEquals(rightResult, webCrawler.getPagesStats());
    }

    @Test
    public void testConnectionException() throws IOException {
        when(connection.get()).thenThrow(new IOException(EXCEPTION_MESSAGE));
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        webCrawler.calculateStats();
        verify(trie, times(0)).getOccurrencesNum(anyString());
    }

    @Test
    public void testGetSortedPagesStats() {
        mockElementIterator(1);
        mockElement(RELEVANT_HREF_ATTR, URLS.get(1));
        WebCrawler webCrawler = new WebCrawler(URLS.get(0), OCCURRENCE_NUM_FIRST_MAP.keySet());
        when(documentBody.text()).thenReturn(TEXTS.get(0)).thenReturn(TEXTS.get(1));
        when(trie.getOccurrencesNum(TEXTS.get(0))).thenReturn(OCCURRENCE_NUM_FIRST_MAP);
        when(trie.getOccurrencesNum(TEXTS.get(1))).thenReturn(OCCURRENCE_NUM_SECOND_MAP);
        webCrawler.calculateStats();
        List<Pair<String, Map<String, Integer>>> result = webCrawler.getSortedPagesStats();
        Assert.assertEquals(result.get(0).getFirst(), URLS.get(1));
        Assert.assertEquals(result.get(0).getSecond(), OCCURRENCE_NUM_SECOND_MAP);
        Assert.assertEquals(result.get(1).getFirst(), URLS.get(0));
        Assert.assertEquals(result.get(1).getSecond(), OCCURRENCE_NUM_FIRST_MAP);
    }
}
