package io.woolford;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableKafka
@EnableScheduling
public class UrlGenerator {

    private final Logger logger = LoggerFactory.getLogger(UrlGenerator.class);

    @Value("${digikey.seed.url}")
    URI digikeySeedUrl;

    @Value("${user.agent}")
    private String userAgent;

    @Value("${digikey.items.per.page}")
    private int digikeyItemsPerPage;

    @Autowired
    KafkaTemplate kafkaTemplate;


    @Scheduled(cron = "0 0 0 * * ?")
    private void generateUrls() throws IOException, URISyntaxException {

        String html = null;

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1200");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        WebDriver driver = new ChromeDriver(options);

        driver.get(digikeySeedUrl.toString());
        html = driver.getPageSource();
        driver.quit();

        Document doc = Jsoup.parse(html);

        Elements catfilterlinks = doc.getElementsByAttributeValue("data-testid", "subcategories-items");

        for (Element element : catfilterlinks) {

            URI catPath = URI.create(element.attr("href").split("\\?")[0]);

            String categoryText = element.parent().text();

            String pattern = "-?\\d+";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(categoryText);
            m.find();

            Integer partCount = Integer.valueOf(m.group(0));

            int pageCount = (int) Math.ceil((double) partCount / digikeyItemsPerPage);

            for (int page = 1; page <= pageCount; page++) {

                String scheme = digikeySeedUrl.getScheme();
                String host = digikeySeedUrl.getHost();
                int port = 443;
                String path = catPath.toString() + "/page/" + page;
                String query = "stock=1&pageSize=" + digikeyItemsPerPage;

                URI uri = new URI(scheme, null, host, port, path, query, null);
                URL url = uri.toURL();

                UrlRecord urlRecord = new UrlRecord();
                urlRecord.setTimestamp(new Date().getTime());
                urlRecord.setUrl(url);

                kafkaTemplate.send("url", urlRecord);

                logger.info(urlRecord.toString());

            }
        }
    }
}

