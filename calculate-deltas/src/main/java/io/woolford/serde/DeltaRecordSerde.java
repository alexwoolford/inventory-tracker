package io.woolford.serde;

import io.woolford.DeltaRecord;
import org.apache.kafka.common.serialization.Serdes;

public class DeltaRecordSerde extends Serdes.WrapperSerde<DeltaRecord> {

    public DeltaRecordSerde(){
        super(new JsonSerializer(), new JsonDeserializer(DeltaRecord.class));
    }

}
