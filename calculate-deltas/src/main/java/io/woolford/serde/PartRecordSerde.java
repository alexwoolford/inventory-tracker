package io.woolford.serde;

import io.woolford.PartRecord;
import org.apache.kafka.common.serialization.Serdes;

public class PartRecordSerde extends Serdes.WrapperSerde<PartRecord> {

    public PartRecordSerde(){
        super(new JsonSerializer(), new JsonDeserializer(PartRecord.class));
    }

}
