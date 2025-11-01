import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class AlbumArtPanel extends JPanel {

    private BufferedImage albumArt;

    public AlbumArtPanel(File mp3File) {
        setPreferredSize(new Dimension(400, 400));
        loadAlbumArt(mp3File);
    }

    private void loadAlbumArt(File mp3File) {
        try {
            // Use Jaudiotagger or a placeholder image if none
            // For now, we'll just try to load a JPEG/PNG with same name
            File imgFile = new File(mp3File.getParentFile(), mp3File.getName().replaceAll("\\.mp3$", ".jpg"));
            if (!imgFile.exists()) imgFile = new File(mp3File.getParentFile(), mp3File.getName().replaceAll("\\.mp3$", ".png"));
            if (imgFile.exists()) albumArt = ImageIO.read(imgFile);
        } catch (Exception e) {
            System.out.println("No album art found for " + mp3File.getName());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (albumArt != null) {
            int w = getWidth();
            int h = getHeight();
            g.drawImage(albumArt, 0, 0, w, h, this);
        } else {
            // Placeholder gray background
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
