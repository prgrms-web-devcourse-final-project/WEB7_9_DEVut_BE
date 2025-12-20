package devut.buzzerbidder.global.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${custom.kafka.topic.live-bid.name:live-bid-events}")
    private String topicName;

    @Value("${custom.kafka.topic.live-bid.partitions:1}")
    private int partitions;

    @Value("${custom.kafka.topic.live-bid.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic liveBidTopic() {
        return TopicBuilder.name(topicName)
                // 파티션 개수. 컨슈머의 처리 속도가 입찰 속도를 못따라 갈 때 확장
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}