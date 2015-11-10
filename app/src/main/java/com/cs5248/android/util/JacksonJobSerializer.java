package com.cs5248.android.util;

import com.cs5248.android.model.cache.IgnoreAAModelIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.persistentQueue.sqlite.SqliteJobQueue;

import java.io.IOException;

/**
 * This does not work yet.
 * Use the default Java serialization instead (may not work for Video class).
 *
 * @author lpthanh
 */
public class JacksonJobSerializer implements SqliteJobQueue.JobSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonJobSerializer() {
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy());
        mapper.setAnnotationIntrospector(new IgnoreAAModelIntrospector());
        mapper.enableDefaultTyping();
    }

    @Override
    public byte[] serialize(Object object) throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    @Override
    public <T extends Job> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        return (T) mapper.readValue(bytes, Job.class);
    }

}
