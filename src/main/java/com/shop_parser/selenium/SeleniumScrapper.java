package com.shop_parser.selenium;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SeleniumScrapper {
    private static final String ROOT_URL = "https://gifts.ru/";
    private static final String LINKS_FILE = "links.txt";
    private WebDriver driver;
    private WebDriverWait wait;
    private BufferedWriter writer;
    private Set<String> visitedLinks; // Множество для хранения посещенных ссылок

    public SeleniumScrapper() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        visitedLinks = new HashSet<>(); // Инициализация множества

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
            System.out.println("Категория: " + mainCategoryText);
            clickElement(mainCategory);
            parseCategoryLinks();
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        }
    }

    private void parseCategoryLinks() throws IOException {
        while (true) {
            boolean allLinksParsed = false;

            while (!allLinksParsed) {
                List<WebElement> categoryLinks = driver.findElements(By.className("ctlg-link"));
                allLinksParsed = true;

                for (int i = 0; i < categoryLinks.size(); i++) {
                    try {
                        WebElement categoryLink = categoryLinks.get(i);
                        String link = categoryLink.getAttribute("href");

                        if (visitedLinks.contains(link)) {
                            continue;
                        }

                        clickElement(categoryLink);
                        visitedLinks.add(link);

                        if (driver.findElements(By.id("j_itemsList")).isEmpty()) {
                            writer.write(link + "\n");
                            writer.flush();
                            System.out.println("Сохранено: " + link);
                        } else {
                            System.out.println("Вложенный переход: " + link);
                            parseNestedLinks();
                        }

                        driver.navigate().back();
                        Thread.sleep(1000);  // добавлено для стабилизации
                        waitForElements(By.className("ctlg-link"));
                        categoryLinks = driver.findElements(By.className("ctlg-link"));
                        allLinksParsed = false;
                    } catch (StaleElementReferenceException e) {
                        System.out.println("Элемент устарел, пробую снова.");
                        i--;
                    } catch (IOException e) {
                        System.err.println("Ошибка записи в файл: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("Ожидание прервано: " + e.getMessage());
                    }
                }
            }

            // Проверяем, есть ли кнопка для подгрузки следующих страниц
            List<WebElement> nextPageButton = driver.findElements(By.id("j_showmore"));
            if (nextPageButton.isEmpty()) {
                System.out.println("Больше страниц не найдено.");
                break;
            } else {
                int linksBefore = driver.findElements(By.className("ctlg-link")).size();
                clickElement(nextPageButton.get(0));
                System.out.println("Переходим на следующую страницу" + nextPageButton.size());

                // Ждем, пока количество ссылок изменится
                wait.until(driver -> driver.findElements(By.className("ctlg-link")).size() > linksBefore);

                System.out.println("Загружены новые ссылки.");
                waitForElements(By.className("ctlg-link"));
            }
        }
    }


    private void parseNestedLinks() throws IOException {
        List<WebElement> nestedLinks = driver.findElements(By.className("ctlg-link"));

        for (int i = 0; i < nestedLinks.size(); i++) {
            try {
                WebElement nestedLink = nestedLinks.get(i);
                String link = nestedLink.getAttribute("href"); // Получаем URL из элемента

                // Проверяем, посещали ли мы эту ссылку
                if (visitedLinks.contains(link)) {
                    continue; // Пропускаем обработку этой ссылки
                }

                clickElement(nestedLink);
                visitedLinks.add(link); // Добавляем ссылку в множество

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
