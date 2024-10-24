package com.shop_parser.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class SeleniumScrapper {
    private WebDriver driver;
    private WebDriverWait wait;
    private Set<String> visitedLinks; // Множество для отслеживания посещенных ссылок

    public SeleniumScrapper() {
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(3));
        visitedLinks = new HashSet<>(); // Инициализация множества

        try {
            // Открытие главной страницы
            driver.get("https://gifts.ru/catalog/ejednevniki-i-bloknoty");
            Stack<String> externalLinksStack = new Stack<>(); // Стек для внешних ссылок
            parseLinks(externalLinksStack);  // Запуск парсинга
            processStack(externalLinksStack); // Обработка стека внешних ссылок
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Закрытие браузера
            driver.quit();
            System.out.println("Браузер закрыт.");
        }
    }

    private void parseLinks(Stack<String> linksStack) {
        // Получение всех ссылок на текущей странице
        List<WebElement> links = driver.findElements(By.className("ctlg-link"));

        // Проверка, есть ли ссылки на текущей странице
        if (links.isEmpty()) {
            System.out.println("На текущей странице нет ссылок.");
            return; // Возвращаемся, если нет ссылок
        }

        // Обход всех ссылок и сохранение в стек
        for (WebElement link : links) {
            String href = link.getAttribute("href");

            // Проверка, была ли уже посещена эта ссылка
            if (!visitedLinks.contains(href)) {
                System.out.println("Сохранение ссылки: " + href);
                linksStack.push(href); // Добавляем ссылку в стек
                visitedLinks.add(href); // Помечаем ссылку как посещённую
            }
        }
    }

    private void processStack(Stack<String> linksStack) {
        // Обработка ссылок из стека
        while (!linksStack.isEmpty()) {
            String currentLink = linksStack.pop();
            System.out.println("Переход по ссылке: " + currentLink);

            // Переход по текущей ссылке
            driver.get(currentLink);

            // Ожидание наличия h1
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));

            // Получение и вывод h1
            String h1Text = driver.findElement(By.tagName("h1")).getText();
            System.out.println("Заголовок h1: " + h1Text);

            // Создание стека для внутренних ссылок
            Stack<String> internalLinksStack = new Stack<>();
            parseLinks(internalLinksStack); // Сбор внутренних ссылок

            // Обработка внутренних ссылок
            while (!internalLinksStack.isEmpty()) {
                String internalLink = internalLinksStack.pop();
                System.out.println("Переход по внутренней ссылке: " + internalLink);
                driver.get(internalLink);

                // Ожидание наличия h1
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1")));

                // Получение и вывод h1
                String internalH1Text = driver.findElement(By.tagName("h1")).getText();
                System.out.println("Заголовок h1: " + internalH1Text);

                // Сбор новых внутренних ссылок для текущей внутренней ссылки
                parseLinks(internalLinksStack); // Рекурсивный вызов для сбора внутренних ссылок
                driver.navigate().back(); // Возврат на предыдущую страницу
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("h1"))); // Ожидание загрузки
            }

            // Возврат на предыдущую страницу
            driver.navigate().back();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("ctlg-link"))); // Ожидание загрузки
        }

        // Если все ссылки были обработаны, выводим сообщение
        System.out.println("Все ссылки на текущей странице обработаны.");
    }
}
