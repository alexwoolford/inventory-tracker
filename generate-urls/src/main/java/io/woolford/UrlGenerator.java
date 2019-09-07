package io.woolford;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

        Document doc = Jsoup.connect(digikeySeedUrl.toString()).userAgent(userAgent).get();

        Elements catfilterlinks = doc.getElementsByClass("catfilterlink");

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

                ObjectMapper mapper = new ObjectMapper();
                String urlRecordJson = mapper.writeValueAsString(urlRecord);

                kafkaTemplate.send("url", urlRecordJson);

                logger.info(urlRecordJson);

            }
        }
    }
}

