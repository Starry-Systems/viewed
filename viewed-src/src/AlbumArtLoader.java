import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class AlbumArtLoader {

    // Returns an ImageIcon for the album art, or null if not found
    public static ImageIcon loadAlbumArt(File mp3File) {
        try {
            // For simplicity, check if there is a .jpg or .png with the same name as the MP3
            String base = mp3File.getAbsolutePath().replaceAll("\\.mp3$", "");
            File jpg = new File(base + ".jpg");
            File png = new File(base + ".png");

            File imageFile = jpg.exists() ? jpg : (png.exists() ? png : null);
            if (imageFile != null) {
                BufferedImage img = ImageIO.read(imageFile);
                Image scaled = img.getScaledInstance(300, 300, Image.SCALE_SMOOTH); // adjust size
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // no art found
    }
}
