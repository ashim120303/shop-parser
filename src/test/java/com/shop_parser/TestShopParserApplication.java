package com.shop_parser;

import org.springframework.boot.SpringApplication;

public class TestShopParserApplication {

	public static void main(String[] args) {
		SpringApplication.from(Main::main).with(TestcontainersConfiguration.class).run(args);
	}

}
