package com.shop_parser;

import com.shop_parser.jsoap.JsoupScrapper;
import java.io.IOException;
import java.sql.SQLException;

// TODO Кол-во в мин.упаковке
public class Main {

	public static void main(String[] args) {
//		SeleniumScrapper seleniumScrapper = new SeleniumScrapper();
		String productUrl = "https://gifts.ru/id/227884"; // URL страницы для парсинга

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
