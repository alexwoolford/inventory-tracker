package io.woolford;


import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TestCrawler {

    @Autowired
    Crawler crawler;

    @Test
    public void CrawlUrlTest() {
        crawler.crawl("https://www.digikey.com/products/en/audio-products/alarms-buzzers-and-sirens/157?FV=-8%7C157&quantity=0&ColumnSort=0&page=1&stock=1&pageSize=500");
    }

}
