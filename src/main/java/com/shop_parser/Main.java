package com.shop_parser;

import com.shop_parser.jsoap.JsoupScrapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
	public static void main(String[] args) {
		String filePath = "links/2-упаковки.txt"; // путь к файлу с ссылками

		try {
			JsoupScrapper scrapper = new JsoupScrapper();
			List<String> productUrls = loadLinksFromFile(filePath, scrapper.getConnection());

			// Используем ExecutorService для параллельной обработки
			ExecutorService executor = Executors.newFixedThreadPool(10); // задайте нужное количество потоков

			for (String productUrl : productUrls) {
				executor.submit(() -> {
					try {
						scrapper.parseAndSaveProduct(productUrl);
						scrapper.markLinkAsProcessed(productUrl); // отмечаем ссылку как обработанную
					} catch (Exception e) {
						System.err.println("Ошибка при обработке URL: " + productUrl + " - " + e.getMessage());
					}
				});
			}

			executor.shutdown(); // Ожидаем завершения всех задач
			while (!executor.isTerminated()) {
				// Ждем завершения всех задач
			}

			scrapper.close(); // Закрытие соединения после выполнения
		} catch (SQLException | IOException e) {
			e.printStackTrace();
		}
	}

	private static List<String> loadLinksFromFile(String filePath, Connection connection) {
		List<String> links = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = br.readLine()) != null) {
				String link = line.trim();
				if (!isLinkProcessed(connection, link)) { // проверка, обработана ли ссылка
					links.add(link);
				}
			}
		} catch (IOException e) {
			System.err.println("Ошибка при чтении файла с ссылками: " + e.getMessage());
		}
		return links;
	}

	private static boolean isLinkProcessed(Connection connection, String link) {
		String query = "SELECT COUNT(*) FROM processed_links WHERE url = ?";
		try (var pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, link);
			try (var rs = pstmt.executeQuery()) {
				return rs.next() && rs.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
