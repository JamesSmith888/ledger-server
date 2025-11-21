package org.jim.ledgerserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LedgerServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerServerApplication.class, args);
    }

}
