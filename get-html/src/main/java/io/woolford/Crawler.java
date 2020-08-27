package io.woolford;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.RateLimiter;
import kong.unirest.*;
import org.apache.kafka.streams.kstream.KStream;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.binder.kafka.streams.annotations.KafkaStreamsProcessor;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;


@Component
@EnableBinding(KafkaStreamsProcessor.class)
class Crawler {

    private final Logger logger = LoggerFactory.getLogger(Crawler.class);
    private final Boolean chromeEnabled;
    private final RateLimiter rateLimiter;

    // Need to add the crawlera key to cacerts
    // https://support.scrapinghub.com/support/solutions/articles/22000203571-using-crawlera-with-java

    @Autowired
    Crawler(@Value("${proxy.enabled}") Boolean proxyEnabled,
            @Value("${chrome.enabled}") Boolean chromeEnabled,
            @Value("${scrapinghub.api.key}") String scrapinghubApiKey,
            @Value("${rate.limit.permits.per.second}") double permitsPerSecond) {

        this.chromeEnabled = chromeEnabled;
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
        if (proxyEnabled){
            Unirest.config().proxy("proxy.crawlera.com", 8010, scrapinghubApiKey, "");
        }

    }

    @StreamListener("input")
    @SendTo("output")
    public KStream<?, com.fasterxml.jackson.databind.JsonNode> process(KStream<?, com.fasterxml.jackson.databind.JsonNode> input) {
        return input.mapValues(value -> {
                    String url = value.get("url").textValue();
                    String html = crawl(url);
                    logger.info("fetched url: " + url);
                    long timestamp = System.currentTimeMillis();
                    return ((ObjectNode) value)
                            .put("html", html)
                            .put("timestamp", timestamp);
                }
        );
    }

    @VisibleForTesting
    public String crawl(String url) {

        rateLimiter.acquire();

        String html = null;
        if (chromeEnabled) {

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1200");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--no-sandbox");
            WebDriver driver = new ChromeDriver(options);

            driver.get(url);
            html = driver.getPageSource();
            driver.quit();

        } else {
            HttpResponse<String> response = Unirest
                    .get(url)
                    .asString();
            html = response.getBody();
        }

        return html;

    }

}
