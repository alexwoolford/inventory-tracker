package io.woolford;

import io.woolford.serde.PartRecordSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class CalculateDeltas {

    private static Logger logger = LoggerFactory.getLogger(CalculateDeltas.class);

    public static void main(String[] args) {

        // set props for Kafka Steams app (see KafkaConstants)
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, KafkaConstants.APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConstants.KAFKA_BROKERS);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, PartRecordSerde.class);

        final StreamsBuilder builder = new StreamsBuilder();

        // PartRecord serializer/deserializer
        final PartRecordSerde partRecordSerde = new PartRecordSerde();

        // create key/value store for part records
        final StoreBuilder<KeyValueStore<String, PartRecord>> partRecordStore = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("partRecordStore"),
                Serdes.String(),
                Serdes.serdeFrom(partRecordSerde.serializer(), partRecordSerde.deserializer()));

        builder.addStateStore(partRecordStore);

        // calculates changes in inventory on-hand quantity
        KStream<String, PartRecord> part = builder.stream("part");

        // calculate the speed using the Haversine transform
        final KStream<String, DeltaRecord> deltas =
                part.transform(new DeltaCalculatorTransformerSupplier("partRecordStore"), "partRecordStore");

        // remove any deltas with missing values and output to the delta topic
        deltas.filterNot((key, deltaRecord) -> deltaRecord.getDelta() == 0).to("delta");

        // run it
        final Topology topology = builder.build();
        logger.info(topology.describe().toString());

        final KafkaStreams streams = new KafkaStreams(topology, props);
        streams.start();

        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }
}
