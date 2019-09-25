package de.derrop.labymod.addons.cores.display;
/*
 * Created by derrop on 24.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.statistics.PlayerStatistics;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StatisticsDisplay extends JFrame {

    private Map<String, BufferedImage> cachedHeads = new HashMap<>();

    private CoresAddon coresAddon;

    private JPanel container = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            StatisticsDisplay.this.paintComponent(g);
        }
    };

    public StatisticsDisplay(CoresAddon coresAddon) {
        super("Cores");

        this.coresAddon = coresAddon;


        super.add(this.container);
        super.setSize(700, 300);
    }


    public void handleStatsUpdate() { //todo not tested if this works when a player disconnects in a round
        super.repaint();
    }

    public void paintComponent(Graphics graphics) {
        new DrawAction(graphics, this.getWidth(), this.getHeight())
                .draw(this.coresAddon.getStatsParser().getCachedStats().values());
        this.coresAddon.getConfig().add("externalDisplay", this.coresAddon.getGson().toJsonTree(this.getBounds()));
        this.coresAddon.saveConfig();
    }

    private void loadHead(String player) {
        String url = "https://minotar.net/avatar/" + player + "/64";
        try {
            URLConnection connection = new URL(url).openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                this.cachedHeads.put(player, ImageIO.read(inputStream));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class DrawAction {
        private final int distanceX = 10, distanceY = 15;

        private Graphics graphics;

        private int x;
        private int y;

        private int width, height;

        public DrawAction(Graphics graphics, int width, int height) {
            this.graphics = graphics;
            this.x = this.distanceX;
            this.y = this.distanceY;
            this.width = width;
            this.height = height;
        }

        public void draw(Collection<PlayerStatistics> statistics) {
            this.graphics.setFont(new Font("Arial", Font.PLAIN, 15));

            this.drawString("Spieler online: " + statistics.size());
            this.nextLine();
            if (statistics.isEmpty()) {
                return;
            }
            for (PlayerStatistics stats : statistics) {
                if (!cachedHeads.containsKey(stats.getName())) {
                    loadHead(stats.getName());
                }

                if (cachedHeads.containsKey(stats.getName())) {
                    Image image = cachedHeads.get(stats.getName());
                    int width = Math.max(image.getWidth(null), this.graphics.getFontMetrics().stringWidth(stats.getName()));
                    int height = image.getHeight(null) + this.graphics.getFont().getSize();
                    if (width + this.x + this.distanceX >= this.width) {
                        this.nextLine(height);
                    }

                    this.graphics.drawString(stats.getName(), this.x, this.y);
                    this.graphics.drawImage(image, this.x, this.y + this.distanceY, null);

                    this.x += width + this.distanceX;
                }
            }
        }

        private void drawString(String draw) {
            int length = this.graphics.getFontMetrics().stringWidth(draw);
            if (length + this.x >= this.width) {
                this.nextLine();
            }

            this.graphics.drawString(draw, this.x, this.y);

            this.x += length + this.distanceX;
        }

        private void nextLine() {
            this.y += this.distanceY + this.graphics.getFont().getSize();
            this.x = this.distanceX;
        }

        private void nextLine(int yDiff) {
            this.y += this.distanceY + yDiff;
            this.x = this.distanceX;
        }
    }
}
