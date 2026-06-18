package com.shopflow.order_inventory;

import org.springframework.boot.SpringApplication;

public class TestOrderInventoryApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderInventoryApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
