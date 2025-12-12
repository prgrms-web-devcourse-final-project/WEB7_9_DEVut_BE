package devut.buzzerbidder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BuzzerBidderApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuzzerBidderApplication.class, args);
    }

}
