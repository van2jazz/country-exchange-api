package com.countryexchange.country_exchange_api.service;

import com.countryexchange.country_exchange_api.entity.Country;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private static final String CACHE_DIR = "cache";
    private static final String SUMMARY_FILE = "cache/summary.png";

    public void generateSummaryImage(Instant timestamp, List<Country> countries) {
        try {
            File dir = new File(CACHE_DIR);
            if (!dir.exists()) dir.mkdirs();

            // top 5 by estimatedGdp (nulls treated as -inf)
            List<Country> top5 = countries.stream()
                    .filter(c -> c.getEstimatedGdp() != null)
                    .sorted((a,b) -> Double.compare(b.getEstimatedGdp(), a.getEstimatedGdp()))
                    .limit(5)
                    .collect(Collectors.toList());

            int width = 900, height = 600;
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // white background
            g.setColor(Color.WHITE);
            g.fillRect(0,0,width,height);

            g.setColor(Color.BLACK);
            Font title = new Font("SansSerif", Font.BOLD, 28);
            g.setFont(title);
            g.drawString("Country Currency Summary", 30, 50);

            Font small = new Font("SansSerif", Font.PLAIN, 16);
            g.setFont(small);
            g.drawString("Total countries: " + countries.size(), 30, 90);

            DateTimeFormatter f = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
            g.drawString("Last refresh: " + f.format(timestamp), 30, 115);

            g.drawString("Top 5 by estimated GDP (USD-equivalent-ish):", 30, 155);
            int y = 185;
            int idx = 1;
            for (Country c : top5) {
                String line = String.format("%d. %s — currency=%s — est_gdp=%.2f", idx++, c.getName(),
                        c.getCurrencyCode(), c.getEstimatedGdp());
                g.drawString(line, 40, y);
                y += 28;
            }

            g.dispose();
            ImageIO.write(img, "png", new File(SUMMARY_FILE));
        } catch (Exception e) {
            e.printStackTrace();
            // don't fail the whole refresh if image generation fails, but log
        }
    }

    public File getSummaryImageFile() {
        File f = new File(SUMMARY_FILE);
        return f.exists() ? f : null;
    }
}
