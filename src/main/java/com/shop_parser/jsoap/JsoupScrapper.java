package com.shop_parser.jsoap;

import com.luciad.imageio.webp.WebPWriteParam;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class JsoupScrapper {

    private static final String URL = "jdbc:mysql://localhost:3306/shop_db";
    private static final String USER = "shop_user";
    private static final String PASSWORD = "123";
    private static final String IMAGE_DIRECTORY = "images/ежедневники и блокноты/";

    private Connection connection;

    public JsoupScrapper() throws SQLException, IOException {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        Files.createDirectories(Paths.get(IMAGE_DIRECTORY)); // Создаем директорию для изображений при создании объекта
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void parseAndSaveProduct(String productUrl) {
        try {
            Document doc = Jsoup.connect(productUrl).get();
            String name = doc.select("h1[itemprop=name]").text();

            String price = doc.select("li.j_price").attr("data-price");
            String article = doc.select("meta[itemprop=sku]").attr("content");

            String size = extractFeature(doc, "Размеры");
            String material = extractFeature(doc, "Материал");
            Float weight = parseFloat(extractFeature(doc, "Вес брутто (1 шт.)"));
            String packaging = extractFeature(doc, "Транспортная упаковка");
            Float packagingWeight = parseFloat(extractFeature(doc, "Вес упаковки"));
            Float packagingVolume = parseFloat(extractFeature(doc, "Объем упаковки"));
            int quantityPerPack = parseQuantity(extractFeature(doc, "Количество в упаковке"));
            int minQuantityPerPack = parseQuantity(extractFeature(doc, "Кол-во в мин.упаковке"));

            int productId = saveProductToDatabase(name, price, article, size, material, weight, packaging, packagingWeight, packagingVolume, quantityPerPack, minQuantityPerPack);
            System.out.println("Attempting to save product: " + name + ", Price: " + price);

            saveProductImages(doc, productId);
            System.out.println("Данные успешно сохранены в базу данных.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markLinkAsProcessed(String link) {
        String insertSQL = "INSERT INTO processed_links (url) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, link);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String extractFeature(Document doc, String featureName) {
        Element featureElement = doc.select("li:contains(" + featureName + ")").first();
        return featureElement != null ? featureElement.ownText() : "";
    }

    private Float parseFloat(String valueStr) {
        if (valueStr.isEmpty()) {
            return null;
        }
        valueStr = valueStr.replaceAll("[^\\d,\\.]", "").replace(",", ".").trim();
        try {
            return Float.parseFloat(valueStr);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования значения: " + valueStr);
            return null;
        }
    }

    private int saveProductToDatabase(String name, String price, String article, String size, String material, Float weight,
                                      String packaging, Float packagingWeight, Float packagingVolume, int quantityPerPack, int minQuantityPerPack) {
        String insertProductSQL = "INSERT INTO product (name, price, article, size, material, weight, packaging, packaging_weight, packaging_volume, quantity_per_pack, min_quantity_per_pack, category_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertProductSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, price);
            pstmt.setString(3, article);
            pstmt.setString(4, size);
            pstmt.setString(5, material);
            pstmt.setFloat(6, weight != null ? weight : 0.0f);
            pstmt.setString(7, packaging);
            pstmt.setFloat(8, packagingWeight != null ? packagingWeight : 0.0f);
            pstmt.setFloat(9, packagingVolume != null ? packagingVolume : 0.0f);
            pstmt.setInt(10, quantityPerPack);
            pstmt.setInt(11, minQuantityPerPack);
            pstmt.setInt(12, 1);
            pstmt.executeUpdate();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private int parseQuantity(String quantityStr) {
        if (quantityStr.isEmpty()) {
            return 0;
        }
        quantityStr = quantityStr.replaceAll("[^\\d]", "").trim();
        try {
            return Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            System.err.println("Ошибка преобразования количества: " + quantityStr);
            return 0;
        }
    }

    private void saveProductImages(Document doc, int productId) {
        Set<String> existingImageNames = new HashSet<>();
        for (Element section : doc.select("section")) {
            for (Element image : section.select("img")) {
                String imageUrl = image.attr("data-hd");
                if (imageUrl.isEmpty()) {
                    imageUrl = "https:" + image.attr("src");
                } else {
                    imageUrl = "https:" + imageUrl;
                }

                String originalImageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                String uniqueImageName = checkAndGetUniqueImageName(productId, originalImageName, existingImageNames);

                try (InputStream in = new URL(imageUrl).openStream()) {
                    // Читаем оригинальное изображение
                    BufferedImage originalImage = ImageIO.read(in);

                    // Сохраняем в формате WebP
                    Path filePath = Paths.get(IMAGE_DIRECTORY + uniqueImageName.replaceAll("\\.(jpg|jpeg|png)$", ".webp"));

                    try (ImageOutputStream output = ImageIO.createImageOutputStream(new File(filePath.toString()))) {
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
                        if (writers.hasNext()) {
                            ImageWriter writer = writers.next();
                            writer.setOutput(output);
                            WebPWriteParam param = (WebPWriteParam) writer.getDefaultWriteParam();
                            param.setCompressionQuality(0.75f); // Настройка качества (0.0f - 1.0f)

                            writer.write(null, new javax.imageio.IIOImage(originalImage, null, null), param);
                            writer.dispose();
                            saveImageToDatabase(productId, uniqueImageName.replaceAll("\\.(jpg|jpeg|png)$", ".webp"));
                            existingImageNames.add(uniqueImageName);
                        } else {
                            System.err.println("Нет доступных писателей для формата WebP.");
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Ошибка при скачивании изображения: " + imageUrl + " - " + e.getMessage());
                }
            }
        }
    }

    private String getUniqueFileName(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        int count = 1;

        while (Files.exists(filePath)) {
            fileName = baseName + "_" + count + extension;
            filePath = filePath.getParent().resolve(fileName);
            count++;
        }
        return fileName;
    }

    private String checkAndGetUniqueImageName(int productId, String imageName, Set<String> existingImageNames) {
        String uniqueName = productId + "_" + imageName;
        if (existingImageNames.contains(uniqueName)) {
            int count = 1;
            while (existingImageNames.contains(productId + "_" + count + "_" + imageName)) {
                count++;
            }
            uniqueName = productId + "_" + count + "_" + imageName;
        }
        return uniqueName;
    }

    private void saveImageToDatabase(int productId, String imageName) {
        String insertImageSQL = "INSERT INTO product_image (product_id, image_name) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(insertImageSQL)) {
            pstmt.setInt(1, productId);
            pstmt.setString(2, imageName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
