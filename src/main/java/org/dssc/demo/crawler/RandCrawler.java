package org.dssc.demo.crawler;

import com.google.common.io.Files;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.logging.log4j.util.Strings;
import org.dssc.demo.data.Article;
import org.dssc.demo.storage.ArticleStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RandCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|mp3|mp4|zip|gz|xml))$");
    private static final Pattern imgPatterns = Pattern.compile(".*(\\.(txt|doc|pdf|ppt|bmp|gif|jpe?g|png|tiff?))$");
    private static File storageFolder;
    private static File imgStorageDir;
    private static File txtStorageDir;

    public final static String URL_PREFIX = "https://www.rand.org";
    public final static String PAGE_URL_PREFIX = "https://www.rand.org/search.html?query=mosaic";

    private final static String TYPE_PATTERN = ".*?(<meta name=\"rand-content-category\" content=\"(.*)\">).*";
    private final static String TITLE_PATTERN = ".*?(<h1 id=\"RANDTitleHeadingId(.*)</h1>).*";
    private final static String SUBTITLE_PATTERN = ".*?(<p class=\"subtitle\">(.*)</p>).*";
    private final static String TIME_PATTERN = ".*?(<meta name=\"citation_online_date\" content=\"(.*)\">).*";
    private final static String CONTENT_PATTERN = ".*(<div class=\"abstract product-page-abstract\">([\\S\\s]*)<footer id=\"footer\").*";
    private final static String PERSON_CONTENT_PATTERN = ".*(<div class=\"biography\">([\\S\\s]*)<footer id=\"footer\").*";
    private final static String BLOG_CONTENT_PATTERN = ".*(<div class=\"body-text\">([\\S\\s]*)<footer id=\"footer\").*";

    private final static String BLANK_CONTENT_PATTERN = "(\\n\\s*)";

    private final static List<String> FETCH_LIST = Arrays.asList(
            "https://www.rand.org/pubs/",
            "https://www.rand.org/about/people/",
            "https://www.rand.org/blog/"
    );

    private final Pattern typePattern;
    private final Pattern titlePattern;
    private final Pattern subtitlePattern;
    private final Pattern timePattern;
    private final Pattern contentPattern;
    private final Pattern personContentPattern;
    private final Pattern blogContentPattern;
    private final Pattern blankContentPattern;
    {
        typePattern = Pattern.compile(TYPE_PATTERN);
        titlePattern = Pattern.compile(TITLE_PATTERN);
        subtitlePattern = Pattern.compile(SUBTITLE_PATTERN);
        timePattern = Pattern.compile(TIME_PATTERN);
        contentPattern = Pattern.compile(CONTENT_PATTERN);
        personContentPattern = Pattern.compile(PERSON_CONTENT_PATTERN);
        blogContentPattern = Pattern.compile(BLOG_CONTENT_PATTERN);
        blankContentPattern = Pattern.compile(BLANK_CONTENT_PATTERN);
        storageFolder = new File("./dumps");
        if (!storageFolder.exists()) {
            storageFolder.mkdir();
        }

        imgStorageDir = new File(storageFolder.getAbsolutePath() + "/" + "IMG");
        if (!imgStorageDir.exists()) {
            imgStorageDir.mkdir();
        }

        txtStorageDir = new File(storageFolder.getAbsolutePath() + "/" + "TXT");
        if (!txtStorageDir.exists()) {
            txtStorageDir.mkdir();
        }
    }

    @Autowired
    private ArticleStorage articleStorage;

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        //System.out.println(url);
        if (href.startsWith(PAGE_URL_PREFIX)) return true;
        if (FILTERS.matcher(href).matches()) {
            return false;
        }

        if (imgPatterns.matcher(href).matches()) { // 匹配二进制文件
            System.out.println("imageUrl: "+url);
            return true;
        }

        if (!href.startsWith(URL_PREFIX)) {
            return false;
        }

        for (String fetch : FETCH_LIST) {
            if (url.getURL().startsWith(fetch)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        //logger.info(url);

        if (page.getParseData() instanceof HtmlParseData) {
            //System.out.println("skip");
            processText(page, url);
        } else if (page.getParseData() instanceof BinaryParseData) {
            fetchImage(page, url);
        }
    }

    private void fetchImage(Page page, String url) {
        String extension = url.substring(url.lastIndexOf('.'));
        String hashedName = UUID.randomUUID() + extension; // 通过uuid 拼接成唯一图片名称

        // 分文件夹存储
        String filename = imgStorageDir.getAbsolutePath() + "/" + hashedName;
        try {
            System.out.println("爬取图片的url:"+url);
            Files.write(page.getContentData(), new File(filename)); // 把爬取到的文件存储到指定文件
        } catch (IOException iox) {
            iox.printStackTrace();
        }
    }

    private void processText(Page page, String url) {
        HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
        String html = htmlParseData.getHtml();

        Matcher typeMatcher = typePattern.matcher(html);
        if (!typeMatcher.find()) {
            logger.info("can not find type. url:{}", url);
            return;
        }
        String type = typeMatcher.group(2);
        if (!type.equals("people") && !type.equals("research") && !type.equals("blog")) {
            logger.info("type:{} not support", type);
            return;
        }

        Matcher timeMatcher = timePattern.matcher(html);
        String time = "";
        if (timeMatcher.find()) {
            try {
                time = timeMatcher.group(2).replaceAll("/", "-");
            } catch (Exception e) {
                logger.error("time match failed. raw:{}", timeMatcher.group(0), e);
            }
        }

        if (!time.split("-")[0].equals("2021")) {
            return;
        }

        Matcher titleMatcher = titlePattern.matcher(html);
        Matcher subTitleMatcher = subtitlePattern.matcher(html);
        String title = "", subtitle = "";
        if (titleMatcher.find()) {
            try {
                String titleContent = titleMatcher.group(2);
                title = titleContent.split(">")[1];
            } catch (Exception e) {
                logger.error("title match failed. raw:{}", titleMatcher.group(0), e);
            }
        } else {
            logger.info("can not find title");
            return;
        }

        if (subTitleMatcher.find()) {
            try {
                subtitle = subTitleMatcher.group(2).split("</p>")[0];
            } catch (Exception e) {
                logger.error("subtitle match failed. raw:{}", subTitleMatcher.group(0), e);
            }
        }
        title = title.concat(" ").concat(subtitle).replaceAll("/", "-");
        if (Strings.isBlank(title)) {
            logger.info("title is blank.., url:{}", url);
            return;
        }

        String abstractText = "";
        if (type.equals("research")) {
            Matcher contentMatcher = contentPattern.matcher(html);
            if (contentMatcher.find()) {
                try {
                    String content = " " + contentMatcher.group(2).replaceAll("<em>", "").replaceAll("</em>", "");
                    abstractText = content.split("<p")[1].split("</p>")[0].split(">")[1];
                } catch (Exception e) {
                    logger.error("content match failed. raw:{}", contentMatcher.group(0), e);
                }
            }
        } else if (type.equals("people")){
            Matcher personContentMatcher = personContentPattern.matcher(html);
            if (personContentMatcher.find()) {
                try {
                    String content = " " + personContentMatcher.group(2);
                    abstractText = content.split("</div>")[0].replaceAll("<p>", "").replaceAll("</p>", "");
                } catch (Exception e) {
                    logger.error("person content match failed. raw:{}", personContentMatcher.group(0), e);
                }
            }
        } else {
            Matcher blogContentMatcher = blogContentPattern.matcher(html);
            if (blogContentMatcher.find()) {
                try {
                    String content = " " + blogContentMatcher.group(2);
                    abstractText = content.split("</div>")[0].split("<div")[0]
                            .replaceAll("<em>", "").replaceAll("</em>", "")
                            .replaceAll("<p>", "").replaceAll("</p>", "");
                } catch (Exception e) {
                    logger.error("person content match failed. raw:{}", blogContentMatcher.group(0), e);
                }
            }
        }
        if (Strings.isBlank(abstractText)) {
            logger.info("abstract is blank.. url:{}", url);
            return;
        }

        Article article = new Article();
        article.setTitle(title);
        article.setDate(time);
        article.setAbstractText(abstractText);
        article.setUrl(url);
        article.setWebsite(URL_PREFIX);
        article.setKind(type);

        if (type.equals("research")) {
            String name = title.replaceAll("\\s", "");
            name = name.length() > 10 ? name.substring(0, 10) : name;
            name += time;
            String hashedName = name + ".txt";

            File file = new File(txtStorageDir.getAbsolutePath() + "/" + hashedName);
            try (FileWriter writer = new FileWriter(file, true);) {
                String content = htmlParseData.getText()
                        .split("RAND Published.*")[1]
                        .split("Document Details")[0];
                Matcher matcher = blankContentPattern.matcher(content);
                writer.append(matcher.replaceAll("\n"));
                writer.flush();
            } catch (Exception e) {
                logger.error("file write failed. raw:{}", htmlParseData.getText(), e);
            }
        }

        try {
            articleStorage.insert(article);
        } catch (DuplicateKeyException e) {
            logger.warn("duplicate insert");
        }
    }
}
