package com.hueharvest.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();

    /**
     * Loads an image from the src/main/resources/assets folder.
     * @param fileName The name of the file (e.g., "player1.png")
     * @return The BufferedImage or null if not found.
     */
    public static BufferedImage getImage(String fileName) {
        if (imageCache.containsKey(fileName)) {
            return imageCache.get(fileName);
        }

        try {
            String path = "/assets/" + fileName;
            InputStream is = AssetManager.class.getResourceAsStream(path);
            if (is == null) {
                // image not found - expected behavior if assets haven't been added yet
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            imageCache.put(fileName, img);
            return img;
        } catch (IOException e) {
            System.err.println("Error loading asset: " + fileName);
            return null;
        }
    }
}
