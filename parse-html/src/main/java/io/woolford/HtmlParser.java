package io.woolford;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@EnableKafka
public class HtmlParser {

    final Logger logger = LoggerFactory.getLogger(HtmlParser.class);

    @Autowired
    KafkaTemplate kafkaTemplate;

    @KafkaListener(topics="html", groupId = "parse-html")
    private void parseHtml(String message) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        HtmlRecord htmlRecord = mapper.readValue(message, HtmlRecord.class);

        try {
            Document doc = Jsoup.parse(htmlRecord.getHtml());
            Element productTable = doc.getElementsByAttributeValue("class", "MuiTableBody-root").get(0);
            Elements productTableRows = productTable.getElementsByTag("tr");

            for (Element productTableRow : productTableRows) {

                String dpn = productTableRow.getElementsByAttributeValue("data-atag", "tr-dkProducts").text();
                String mpn = productTableRow.getElementsByAttribute("data-product-id").text();
                String mfg = productTableRow.getElementsByAttributeValue("data-atag", "tr-manufacturer").text();
                String qtyAvailableRaw = productTableRow.getElementsByAttributeValue("data-atag", "tr-qtyAvailable").text();
                Integer qoh = qohParser(qtyAvailableRaw);

                PartRecord partRecord = new PartRecord();
                partRecord.setTimestamp(htmlRecord.getTimestamp());
                partRecord.setDpn(dpn);
                partRecord.setMpn(mpn);
                partRecord.setMfg(mfg);
                partRecord.setQoh(qoh);

                String partRecordJson = mapper.writeValueAsString(partRecord);

                kafkaTemplate.send("part", partRecordJson);

            }
        } catch (Exception e) {
            logger.error("Unable to parse url " + htmlRecord.getUrl() + e.getMessage());
        }
    }

    Integer qohParser(String qtyAvailableRaw) {

        Integer qoh = 0;
        try {
            String pattern = "(\\d+) - Immediate.*";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(qtyAvailableRaw.replace(",", ""));
            m.find();

            qoh = Integer.valueOf(m.group(1));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return qoh;
    }

}