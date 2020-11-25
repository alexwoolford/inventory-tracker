package io.woolford;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;

import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class DeltaCalculatorTransformerSupplier implements TransformerSupplier<String, PartRecord, KeyValue<String, DeltaRecord>> {

    private static final Logger LOG = LoggerFactory.getLogger(DeltaCalculatorTransformerSupplier.class);

    public String partRecordStoreName;

    DeltaCalculatorTransformerSupplier(String partRecordStoreName) {
        this.partRecordStoreName = partRecordStoreName;
    }

    @Override
    public Transformer<String, PartRecord, KeyValue<String, DeltaRecord>> get() {
        return new Transformer<String, PartRecord, KeyValue<String, DeltaRecord>>() {

            private KeyValueStore<String, PartRecord> partRecordStore;

            @SuppressWarnings("unchecked")
            @Override
            public void init(final ProcessorContext context) {
                partRecordStore = (KeyValueStore<String, PartRecord>) context.getStateStore(partRecordStoreName);
            }

            @Override
            public KeyValue<String, DeltaRecord> transform(final String dummy, final PartRecord partRecord) {

                PartRecord previousPartRecord = partRecordStore.get(partRecord.getDpn());

                // if there is a previous part record for that DPN, calculate the change in inventory
                DeltaRecord deltaRecord = new DeltaRecord();
                deltaRecord.setTimestamp(partRecord.getTimestamp());
                deltaRecord.setDpn(partRecord.getDpn());
                deltaRecord.setMpn(partRecord.getMpn());
                deltaRecord.setMfg(partRecord.getMfg());

                if (previousPartRecord != null){
                    deltaRecord.setDelta(previousPartRecord.getQoh() - partRecord.getQoh());
                } else {
                    deltaRecord.setDelta(0);
                }

                partRecordStore.put(partRecord.getDpn(), partRecord);

                return new KeyValue<>(partRecord.getDpn(), deltaRecord);

            }

            @Override
            public void close() {
            }

        };
    }
}
