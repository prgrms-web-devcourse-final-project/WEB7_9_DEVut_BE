package devut.buzzerbidder;

import devut.buzzerbidder.global.config.DopplerContextInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BuzzerBidderApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BuzzerBidderApplication.class);
        application.addInitializers(new DopplerContextInitializer());
        application.run(args);
    }

}
