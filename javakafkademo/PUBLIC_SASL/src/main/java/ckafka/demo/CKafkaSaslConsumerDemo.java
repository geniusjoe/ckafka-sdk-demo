package ckafka.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.JaasUtils;

public class CKafkaSaslConsumerDemo {

    public static void main(String[] args) {
        //加载kafka.properties。
        Properties kafkaProperties = CKafkaConfigure.getCKafkaProperties();
        //设置 jaas 配置信息
        System.setProperty(JaasUtils.JAVA_LOGIN_CONFIG_PARAM, kafkaProperties.getProperty("java.security.auth.login.config"));
        Properties props = new Properties();

        //
        //  SASL_PLAINTEXT 公网接入
        //
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        //  SASL 采用 Plain 方式。
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");

        //
        //  SASL_SSL 公网接入
        //
        //  接入协议。
        // props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        //  SASL 采用 Plain 方式。
        // props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        //  SSL 加密。
        // props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaProperties.getProperty(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
        // props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaProperties.getProperty(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        // props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG,kafkaProperties.getProperty(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG));

        //设置接入点，请通过控制台获取对应Topic的接入点。
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getProperty("bootstrap.servers"));
        //两次Poll之间的最大允许间隔。
        //消费者超过该值没有返回心跳，服务端判断消费者处于非存活状态，服务端将消费者从Consumer Group移除并触发Rebalance，默认30s。
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        //每次Poll的最大数量。
        //注意该值不要改得太大，如果Poll太多数据，而不能在下次Poll之前消费完，则会触发一次负载均衡，产生卡顿。
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 30);
        //消息的反序列化方式。
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        //当前消费实例所属的消费组，请在控制台申请之后填写。
        //属于同一个组的消费实例，会负载消费消息。
        props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getProperty("group.id"));

        //构造消费对象，也即生成一个消费实例。
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        //设置消费组订阅的Topic，可以订阅多个。
        //如果GROUP_ID_CONFIG是一样，则订阅的Topic也建议设置成一样。
        List<String> subscribedTopics = new ArrayList<>();
        //如果需要订阅多个Topic，则在这里添加进去即可。
        //每个Topic需要先在控制台进行创建。
        String topicStr = kafkaProperties.getProperty("topic");
        String[] topics = topicStr.split(",");
        for (String topic : topics) {
            subscribedTopics.add(topic.trim());
        }
        consumer.subscribe(subscribedTopics);

        //循环消费消息。
        while (true) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                //必须在下次Poll之前消费完这些数据, 且总耗时不得超过SESSION_TIMEOUT_MS_CONFIG。
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("[consumer partition: %d][offset: %d][record: \"%s\"]%n", record.partition(), record.offset(), record.value());
                }
            } catch (Exception e) {
                System.out.println("consumer error! error = " + e.getMessage());
            }
        }
    }
}