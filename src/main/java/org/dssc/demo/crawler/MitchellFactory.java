package org.dssc.demo.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MitchellFactory implements CrawlController.WebCrawlerFactory<MitchellNewsCrawler> {

    @Autowired
    private MitchellNewsCrawler mitchellNewsCrawler;

    @Override
    public MitchellNewsCrawler newInstance() throws Exception {
        return mitchellNewsCrawler;
    }
}
