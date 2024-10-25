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
    private BufferedWriter writer;

    public SeleniumScrapper() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            writer = new BufferedWriter(new FileWriter(LINKS_FILE));
            driver.get(ROOT_URL);
            List<WebElement> mainCategories = driver.findElements(By.className("m-ctlg-root"));

            for (WebElement mainCategory : mainCategories) {
                processMainCategory(mainCategory);
            }
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        } finally {
            closeWriter();
            driver.quit();
        }
    }

    private void processMainCategory(WebElement mainCategory) {
        try {
            String mainCategoryText = mainCategory.getText();
            System.out.println("Категория: " + mainCategoryText + " сохранена!");
            clickElement(mainCategory);
            parseCategoryLinks();
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        }
    }

    private void parseCategoryLinks() throws IOException {
        List<WebElement> categoryLinks = driver.findElements(By.className("ctlg-link"));

        for (int i = 0; i < categoryLinks.size(); i++) {
            try {
                WebElement categoryLink = categoryLinks.get(i);
                clickElement(categoryLink);
                String link = driver.getCurrentUrl();

                if (driver.findElements(By.id("j_itemsList")).isEmpty()) {
                    writer.write(link + "\n");
                    writer.flush(); // Сразу записать в файл
                    System.out.println("Сохранено: " + link);
                } else {
                    System.out.println("Вложенный переход: " + link);
                    parseNestedLinks();
                }

                driver.navigate().back(); // Возврат к предыдущей категории
                waitForElements(By.className("ctlg-link")); // Ожидание загрузки элементов
                categoryLinks = driver.findElements(By.className("ctlg-link")); // Обновление списка ссылок
            } catch (StaleElementReferenceException e) {
                System.out.println("Элемент устарел, пробую снова.");
                i--; // Уменьшение счетчика для повторной обработки того же элемента
            } catch (IOException e) {
                System.err.println("Ошибка записи в файл: " + e.getMessage());
            } catch (NoSuchElementException e) {
                System.out.println("Элемент не найден: " + e.getMessage());
            }
        }
    }

    private void parseNestedLinks() throws IOException {
        List<WebElement> nestedLinks = driver.findElements(By.className("ctlg-link"));

        for (int i = 0; i < nestedLinks.size(); i++) {
            try {
                WebElement nestedLink = nestedLinks.get(i);
                clickElement(nestedLink);
                String link = driver.getCurrentUrl();

                writer.write(link + "\n");
                writer.flush(); // Сразу записать в файл
                System.out.println("Сохранено: " + link);

                driver.navigate().back(); // Возврат к предыдущей категории
                waitForElements(By.className("ctlg-link")); // Ожидание загрузки элементов
                nestedLinks = driver.findElements(By.className("ctlg-link")); // Обновление списка ссылок
            } catch (StaleElementReferenceException e) {
                System.out.println("Ссылка устарела, пробую снова.");
                i--; // Уменьшение счетчика для повторной обработки того же элемента
            } catch (IOException e) {
                System.err.println("Ошибка записи в файл: " + e.getMessage());
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

    private void waitForElements(By by) {
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(by));
    }

    private void closeWriter() {
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Ошибка закрытия файла: " + e.getMessage());
        }
    }
}
