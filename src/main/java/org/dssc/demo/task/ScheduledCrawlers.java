package org.dssc.demo.task;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.dssc.demo.crawler.MitchellFactory;
import org.dssc.demo.crawler.RandFactory;
import org.dssc.demo.crawler.SpaNewsCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class ScheduledCrawlers {
    private static final Logger log = LoggerFactory.getLogger(ScheduledCrawlers.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");

    @Autowired
    private MitchellFactory mitchellFactory;
    @Autowired
    private RandFactory randFactory;

    @Scheduled(cron = "0 * 2 * * ? ")
    public void spaCrawler(){
        try {
            CrawlController crawlController = initController(400, 100 ,80);
            crawlController.addSeed(SpaNewsCrawler.URL_PREFIX);
            crawlController.startNonBlocking(SpaNewsCrawler::new, 1);
        } catch (Exception e) {
            log.info("Initialize CrawlerController FAIL", e);
        }
    }

    @Scheduled(cron = "0 * 2 * * ? ")
    public void mitchellCrawler(){
        String urlStart = "https://mitchellaerospacepower.org/search/mosaic/";
        try {
            CrawlController crawlController = initController(100, 100, 1000);
            crawlController.addSeed(urlStart);
            crawlController.startNonBlocking(mitchellFactory, 1);
        } catch (Exception e) {
            log.info("Initialize CrawlerController FAIL", e);
        }
    }

    @Scheduled(cron = "0 * 2 * * ? ")
    public void randCrawler(){
        String urlStart = "https://www.rand.org/search.html?query=mosaic&sortby=relevance";
        try {
            CrawlController crawlController = initController(300, 4, 30000);
            crawlController.addSeed(urlStart);
            crawlController.startNonBlocking(randFactory, 5);
        } catch (Exception e) {
            log.info("Initialize CrawlerController FAIL", e);
        }
    }

    private CrawlController initController(int delayTime, int maxDepth, int maxFetch) throws Exception{
        String crawlStorageFolder = ".";
        CrawlConfig crawlConfig = new CrawlConfig();
        crawlConfig.setCrawlStorageFolder(crawlStorageFolder);
        crawlConfig.setPolitenessDelay(delayTime);
        crawlConfig.setMaxDepthOfCrawling(maxDepth);
        crawlConfig.setMaxPagesToFetch(maxFetch);
        // 支持爬取二进制文件（图片/pdf等）
        crawlConfig.setIncludeBinaryContentInCrawling(true);
        crawlConfig.setMaxDownloadSize(10485760);

        PageFetcher pageFetcher = new PageFetcher(crawlConfig);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        return new CrawlController(crawlConfig, pageFetcher, robotstxtServer);
    }
}
