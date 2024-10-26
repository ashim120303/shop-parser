package com.shop_parser;

import com.shop_parser.selenium.SeleniumScrapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Main {

	public static void main(String[] args) {
		SeleniumScrapper scrapper = new SeleniumScrapper("https://gifts.ru/catalog/upakovka");



//		String productUrl = "https://gifts.ru/id/227884"; // URL страницы для парсинга
//		try {
//			JsoupScrapper scrapper = new JsoupScrapper();
//			scrapper.parseAndSaveProduct(productUrl);
//			scrapper.close(); // Закрытие соединения после выполнения
//		} catch (SQLException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }
}
