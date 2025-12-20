package ru.yandex.practicum.kafka.telemetry.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GeneralAvroSerializer implements Serializer<SpecificRecordBase> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();

    @Override
    public byte[] serialize(String topic, SpecificRecordBase specificRecordBase) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (specificRecordBase == null) {
                return null;
            }
            BinaryEncoder encoder = encoderFactory.binaryEncoder(byteArrayOutputStream, null);
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(specificRecordBase.getSchema());
            writer.setSchema(specificRecordBase.getSchema());
            writer.write(specificRecordBase, encoder);
            encoder.flush();
            return (byteArrayOutputStream.toByteArray());
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации данных для темы [" + topic + "]", e);        }
    }
}
