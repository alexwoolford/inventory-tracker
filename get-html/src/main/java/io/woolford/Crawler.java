package io.woolford;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.RateLimiter;
import kong.unirest.*;
import org.apache.kafka.streams.kstream.KStream;
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

    private final RateLimiter rateLimiter;

    // Need to add the crawlera key to cacerts
    // https://support.scrapinghub.com/support/solutions/articles/22000203571-using-crawlera-with-java

    @Autowired
    Crawler(@Value("${scrapinghub.api.key}") String scrapinghubApiKey,
            @Value("${rate.limit.permits.per.second}") double permitsPerSecond) {
        rateLimiter = RateLimiter.create(permitsPerSecond);
        Unirest.config().proxy("proxy.crawlera.com", 8010, scrapinghubApiKey, "");
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

    private String crawl(String url) {

        // httpbin allows you to see what the target host might see, e.g.
        //     /headers
        //     /anything shows everything the host can see
        //     /ip shows the source IP address
        rateLimiter.acquire();
        HttpResponse<String> response = Unirest
                .get(url)
                .asString();

        // http://ec2-54-153-68-123.us-west-1.compute.amazonaws.com/ip

        return response.getBody();

    }

}
