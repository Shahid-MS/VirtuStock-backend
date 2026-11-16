package com.virtu_stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class VirtuStockApplication {

	public static void main(String[] args) {
		SpringApplication.run(VirtuStockApplication.class, args);
	}

}
