package com.shop_parser.selenium;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class SeleniumScrapper {
    private static final String ROOT_URL = "https://gifts.ru/";
    private static final String LINKS_FILE = "links.txt";
    private WebDriver driver;
    private WebDriverWait wait;

    public SeleniumScrapper() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LINKS_FILE))) {
            driver.get(ROOT_URL);
            List<WebElement> mainCategories = driver.findElements(By.className("m-ctlg-root"));

            for (WebElement mainCategory : mainCategories) {
                processMainCategory(writer, mainCategory);
            }
        } catch (IOException e) {
            System.out.println("Ошибка записи в файл: " + e.getMessage());
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    private void processMainCategory(BufferedWriter writer, WebElement mainCategory) {
        try {
            // Записываем категорию и переходим по ссылке
            String mainCategoryText = mainCategory.getText();
            writer.write("Категория: " + mainCategoryText + "\n");
            System.out.println("Категория: " + mainCategoryText + " сохранена!");
            clickElement(mainCategory);
            parseCategoryLinks(writer); // Парсим ссылки внутри категории
        } catch (IOException e) {
            System.out.println("Ошибка записи в файл: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void parseCategoryLinks(BufferedWriter writer) {
        List<WebElement> categoryLinks = driver.findElements(By.className("ctlg-link"));

        for (WebElement categoryLink : categoryLinks) {
            try {
                clickElement(categoryLink); // Переход к ссылке
                String link = driver.getCurrentUrl(); // Получаем URL
                writer.write(link + "\n");
                System.out.println("Сохранено: " + link);
                driver.navigate().back(); // Возврат к списку ссылок
            } catch (IOException e) {
                System.out.println("Ошибка записи в файл: " + e.getMessage());
                e.printStackTrace();
            } catch (StaleElementReferenceException e) {
                System.out.println("Ссылок не осталось");
            } catch (NoSuchElementException e) {
                System.out.println("Элемент не найден: " + e.getMessage());
            }
        }
    }

    private void clickElement(WebElement element) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(element)).click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }
}
