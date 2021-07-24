package org.dssc.demo.controller;

import org.dssc.demo.storage.ArticleStorage;
import org.dssc.demo.task.ScheduledCrawlers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @Autowired
    ScheduledCrawlers scheduledCrawlers;

    @Autowired
    ArticleStorage articleStorage;

    @GetMapping("/crawl")
    public void crawl() {
        scheduledCrawlers.randCrawler();
    }
}
