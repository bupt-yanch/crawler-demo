package org.dssc.demo.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RandFactory implements CrawlController.WebCrawlerFactory<RandCrawler>{

    @Autowired
    private RandCrawler randCrawler;

    @Override
    public RandCrawler newInstance() throws Exception {
        return randCrawler;
    }
}
