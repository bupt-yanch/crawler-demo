package org.dssc.demo.crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.dssc.demo.data.Article;
import org.dssc.demo.storage.ArticleStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MitchellNewsCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg|png|mp3|mp4|zip|gz))$");
    public final static String URL_PREFIX = "https://mitchellaerospacepower.org/";
    public final static String PAGE_URL_PREFIX = "https://mitchellaerospacepower.org/search/mosaic/";
    private final static String TITLE_PATTERN = ".*?(<title>(.*)</title>).*";
    private final static String TIME_PATTERN = ".*?(datetime=\"(.*)\">).*";
    private final static String CONTENT_PATTERN = ".*(<div class=\"prose max-w-none\">([\\S\\s]*)Share Article).*";

    private final static List<String> unCrawlList =Arrays.asList(
            "https://mitchellaerospacepower.org/about/",
            "https://mitchellaerospacepower.org/events/",
            "https://mitchellaerospacepower.org/event/",
            "https://mitchellaerospacepower.org/resources/",
            "https://mitchellaerospacepower.org/issues/",
            "https://mitchellaerospacepower.org/issue/",
            "https://mitchellaerospacepower.org/donate/",
            "https://mitchellaerospacepower.org/contact-us/",
            "https://mitchellaerospacepower.org/category/",
            "https://mitchellaerospacepower.org/wp-content/",
            "https://mitchellaerospacepower.org/our-supporters/"
    );

    //private Pattern pageClassPattern;
    private Pattern titlePattern;
    private Pattern timePattern;
    private Pattern contentPattern;
    {
        //pageClassPattern = Pattern.compile(PAGE_CLASS_PATTERN);
        titlePattern = Pattern.compile(TITLE_PATTERN);
        timePattern = Pattern.compile(TIME_PATTERN);
        contentPattern = Pattern.compile(CONTENT_PATTERN);
    }

    @Autowired
    private ArticleStorage articleStorage;

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        if (href.startsWith(PAGE_URL_PREFIX)) return true;
        return !FILTERS.matcher(href).matches() && href.startsWith(URL_PREFIX) && isCrawl(url.getURL());
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        logger.info(url);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();

            Matcher titleMatcher = titlePattern.matcher(html);
            String title = "";
            if (titleMatcher.find()) {
                try {
                    String titleContent = titleMatcher.group(2);
                    title = titleContent.split("-")[0];
                } catch (Exception e) {
                    logger.error("title match failed. raw:{}", titleMatcher.group(0), e);
                }
            } else {
                logger.info("can not find title");
                return;
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
                    abstractText = content.split("<p")[1].split("</p>")[0].split(">")[1];
                } catch (Exception e) {
                    logger.error("content match failed. raw:{}", contentMatcher.group(0), e);
                }
            } else {
                logger.info("can not find content");
                return;
            }

            Article article = new Article();
            article.setTitle(title);
            article.setDate(time);
            article.setAbstractText(abstractText);
            article.setUrl(url);
            article.setWebsite(URL_PREFIX);
            article.setKind("");

            articleStorage.insert(article);
        }
    }

    private boolean isCrawl(String url) {
        for (String webUrl : unCrawlList) {
            if (url.startsWith(webUrl)) {
                return false;
            }
        }
        return true;
    }
}
