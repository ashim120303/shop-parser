package com.shop_parser;

import com.shop_parser.jsoap.JsoupScrapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class Main {

	public static void main(String[] args) {
//		SeleniumScrapper seleniumScrapper = new SeleniumScrapper();
		String productUrl = "https://gifts.ru/id/135551"; // URL страницы для парсинга

		try {
			JsoupScrapper scrapper = new JsoupScrapper();
			scrapper.parseAndSaveProduct(productUrl);
			scrapper.close(); // Закрытие соединения после выполнения
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
