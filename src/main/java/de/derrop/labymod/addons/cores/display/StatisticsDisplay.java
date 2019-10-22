package de.derrop.labymod.addons.cores.display;
/*
 * Created by derrop on 24.09.2019
 */

import de.derrop.labymod.addons.cores.CoresAddon;
import de.derrop.labymod.addons.cores.player.OnlinePlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StatisticsDisplay extends JFrame {

    private Map<String, BufferedImage> cachedHeads = new HashMap<>();

    private CoresAddon coresAddon;

    private Collection<OnlinePlayer> lastOnlinePlayers;

    public StatisticsDisplay(CoresAddon coresAddon) {
        super("Stats");

        this.coresAddon = coresAddon;


        JPanel container = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                StatisticsDisplay.this.paintComponent(g);
            }
        };
        super.add(container);
        super.setSize(700, 300);
    }


    public void handleStatsUpdate(Collection<OnlinePlayer> onlinePlayers) { //todo stats in BW not displayed properly, and pretty rare, but in CORES too
        this.lastOnlinePlayers = onlinePlayers;
        super.repaint();
    }

    private void paintComponent(Graphics graphics) {
        if (this.lastOnlinePlayers == null) {
            return;
        }
        new DrawAction(graphics, this.getWidth(), this.getHeight())
                .draw(this.lastOnlinePlayers);
        this.coresAddon.getConfig().add("externalDisplay", this.coresAddon.getGson().toJsonTree(this.getBounds()));
        this.coresAddon.getConfig().addProperty("externalDisplayExtendedState", this.getExtendedState());
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

        private int width;

        DrawAction(Graphics graphics, int width, int height) {
            this.graphics = graphics;
            this.x = this.distanceX;
            this.y = this.distanceY;
            this.width = width;
        }

        void draw(Collection<OnlinePlayer> players) {
            this.graphics.setFont(new Font("Arial", Font.PLAIN, 15));

            this.drawString("Spieler online: " + players.size());
            this.nextLine();
            if (players.isEmpty()) {
                return;
            }
            coresAddon.sortStreamByStats(players.stream()).forEach(stats -> {
                if (!cachedHeads.containsKey(stats.getName())) {
                    loadHead(stats.getName());
                }

                if (cachedHeads.containsKey(stats.getName())) {
                    Collection<String> texts = new ArrayList<>();
                    texts.add(stats.getName());
                    texts.add(" ");
                    texts.addAll(stats.getLastStatistics().getHumanReadableEntries());

                    Image image = cachedHeads.get(stats.getName());
                    int width = image.getWidth(null);
                    int yDiff = this.distanceY;
                    for (String text : texts) {
                        int textWidth = this.graphics.getFontMetrics().stringWidth(text);
                        if (textWidth > width) {
                            width = textWidth;
                        }
                        yDiff += this.graphics.getFont().getSize() + this.distanceY;
                    }
                    int height = image.getHeight(null) + yDiff;
                    if (width + this.x + this.distanceX >= this.width) {
                        this.nextLine(height);
                    }
                    yDiff = this.distanceY;
                    for (String text : texts) {
                        this.graphics.drawString(text, this.x, this.y + yDiff + image.getHeight(null));
                        yDiff += this.graphics.getFont().getSize() + this.distanceY;
                    }

                    this.graphics.drawImage(image, this.x, this.y, null);

                    this.x += width + this.distanceX;
                }
            });
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
