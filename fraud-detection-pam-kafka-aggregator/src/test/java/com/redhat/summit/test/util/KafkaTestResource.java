package com.redhat.summit.test.util;

import io.debezium.kafka.KafkaCluster;
import io.debezium.util.Testing;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class KafkaTestResource implements QuarkusTestResourceLifecycleManager {

    private KafkaCluster kafka;
    private File dataDir;

    @Override
    public Map<String, String> start() {
        try {
            Properties props = new Properties();
            props.setProperty("zookeeper.connection.timeout.ms", "45000");
            dataDir = Testing.Files.createTestingDirectory("kafka-data", true);
            kafka = new KafkaCluster().withPorts(2182, 19092)
                    .addBrokers(1)
                    .usingDirectory(dataDir)
                    .deleteDataPriorToStartup(true)
                    .deleteDataUponShutdown(true)
                    .withKafkaConfiguration(props)
                    .startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (kafka != null) {
            try {
                kafka.deleteDataUponShutdown(true);
                Testing.Files.delete(dataDir);
                kafka.shutdown();
            } catch (Exception e) {

            }
        }
    }
}
