import javafx.scene.media.MediaPlayer;

import javax.swing.*;
import java.awt.*;

public class VisualizerPanel extends JPanel {

    private MediaPlayer mediaPlayer;

    public VisualizerPanel() {
        setPreferredSize(new Dimension(800, 150));
        setBackground(Color.BLACK);
    }

    // Call this when an MP3 is loaded
    public void attachMediaPlayer(MediaPlayer player) {
        this.mediaPlayer = player;

        // You can start a thread to periodically repaint using audio data
        new Thread(() -> {
            while (mediaPlayer != null) {
                repaint();
                try {
                    Thread.sleep(50); // 20 FPS
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    public void reset() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.GREEN);
        // Example: draw some dummy waveform (replace with real audio levels)
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < w; i += 5) {
            int height = (int)(Math.random() * h);
            g.fillRect(i, h - height, 3, height);
        }
    }
}
