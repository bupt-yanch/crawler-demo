package org.dssc.demo.crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpaNewsCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz))$");
    public final static String URL_PREFIX = "https://spa.com/news-insights/";
    private final static String PAGE_CLASS_PATTERN = ".*?(<span class=\"page_label\">(.*)</span>).*";
    private final static String TITLE_PATTERN = ".*?(<h1 class=\"page_title\">(.*)</h1>).*";
    private final static String TIME_PATTERN = ".*?(<p class=\"page_subtitle\">(.*)</p>).*";
    private final static String CONTENT_PATTERN = ".*?(<div class=\"wp-block-acfab-region\">(.*)</div>).*";

    private Pattern pageClassPattern;
    private Pattern titlePattern;
    private Pattern timePattern;
    private Pattern contentPattern;
    {
        pageClassPattern = Pattern.compile(PAGE_CLASS_PATTERN);
        titlePattern = Pattern.compile(TITLE_PATTERN);
        timePattern = Pattern.compile(TIME_PATTERN);
        contentPattern = Pattern.compile(CONTENT_PATTERN);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        //System.out.println(href);
        return !FILTERS.matcher(href).matches() && href.startsWith(URL_PREFIX);
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            Matcher pageClassMather = pageClassPattern.matcher(html);
            if (!pageClassMather.find()) {
                logger.info("had not page label, url:{}", url);
                return;
            }

            try {
                String label = pageClassMather.group(2);
                if (!label.equals("News")) {
                    logger.info("label is {}, not News, url:{}", label, url);
                    return;
                }
            } catch (Exception e) {
                logger.info("extract label failed, url:{}", url);
                return;
            }

            Matcher titleMatcher = titlePattern.matcher(html);
            String title = "";
            if (titleMatcher.find()) {
                try {
                    title = titleMatcher.group(2);
                } catch (Exception e) {
                    logger.error("title match failed. raw:{}", titleMatcher.group(0), e);
                }
            }

            Matcher timeMatcher = timePattern.matcher(html);
            String time = "";
            if (timeMatcher.find()) {
                try {
                    time = timeMatcher.group(2);
                } catch (Exception e) {
                    logger.error("time match failed. raw:{}", timeMatcher.group(0), e);
                }
            }

            Matcher contentMatcher = contentPattern.matcher(html);
            String abstractText = "";
            if (contentMatcher.find()) {
                try {
                    String content = " " + contentMatcher.group(2);
                    abstractText = content.split("<p>")[1].split("</p>")[0];
                } catch (Exception e) {
                    logger.error("content match failed. raw:{}", contentMatcher.group(0), e);
                }
            }

            logger.info("----------------------");
            logger.info("URL:{}", url);
            logger.info("title:{}", title);
            logger.info("time:{}", time);
            logger.info("content:{}", abstractText);
        }
    }
}
