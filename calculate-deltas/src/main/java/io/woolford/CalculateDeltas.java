package io.woolford;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class CalculateDeltas {

    private static Logger logger = LoggerFactory.getLogger(CalculateDeltas.class);

    static RocksDB db = getRocksDB();

    public static void main(String[] args) {

        // set props for Kafka Steams app (see KafkaConstants)
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, KafkaConstants.APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.KAFKA_BROKERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        // calculates speed based on distance/time of last measurement
        KStream<String, String> part = builder.stream("part");
        part.mapValues(value -> {
            value = CalculateDeltas.calculateDeltas(value);
            return value;
        }).filter((key, value) -> value != null).to("delta");

        // run it
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }

    private static RocksDB getRocksDB() {
        RocksDB.loadLibrary();

        final Options options = new Options().setCreateIfMissing(true);

        try {
            final RocksDB db = RocksDB.open(options, "/tmp/" + KafkaConstants.APPLICATION_ID);
            return db;
        } catch (RocksDBException e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    private static String calculateDeltas(String partRecordString){

        ObjectMapper mapper = new ObjectMapper();
        PartRecord partRecord = new PartRecord();
        try {
            partRecord = mapper.readValue(partRecordString, PartRecord.class);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        String partKey = partRecord.getDpn() + ":" + partRecord.getMfg() + ":" + partRecord.getMpn();
        PartRecord previousPartRecord = new PartRecord();
        String previousPartRecordString;
        try {
            byte[] previousPartRecordBytes = db.get(partKey.getBytes());
            if (previousPartRecordBytes != null){
                previousPartRecordString = new String(previousPartRecordBytes);
                previousPartRecord = mapper.readValue(previousPartRecordString, PartRecord.class);
            }
        } catch (RocksDBException | IOException e) {
            logger.error(e.getMessage());
        }

        try {
            db.put(partKey.getBytes(), partRecordString.getBytes());
        } catch (RocksDBException e) {
            logger.error(e.getMessage());
        }

        Integer delta = 0;
        if (previousPartRecord.getQoh() != null && partRecord.getQoh() != null){
            delta = previousPartRecord.getQoh() - partRecord.getQoh();
        }

        DeltaRecord deltaRecord = new DeltaRecord();
        deltaRecord.setTimestamp(partRecord.getTimestamp());
        deltaRecord.setDpn(partRecord.getDpn());
        deltaRecord.setMpn(partRecord.getMpn());
        deltaRecord.setMfg(partRecord.getMfg());
        deltaRecord.setDelta(delta);

        if (delta != 0) {
            try {
                return mapper.writeValueAsString(deltaRecord);
            } catch (JsonProcessingException e) {
                logger.error(e.getMessage());
            }
        }

        return null;
    }

}
