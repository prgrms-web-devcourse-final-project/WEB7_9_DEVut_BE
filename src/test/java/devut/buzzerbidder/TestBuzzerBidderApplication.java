package devut.buzzerbidder;

import org.springframework.boot.SpringApplication;

public class TestBuzzerBidderApplication {

    public static void main(String[] args) {
        SpringApplication.from(BuzzerBidderApplication::main)
            .with(TestcontainersConfig.class).run(args);
    }

}
