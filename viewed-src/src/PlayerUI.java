import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.File;

public class PlayerUI {

    private MediaPlayer mediaPlayer;
    private boolean isSeeking = false; // To avoid conflict between slider update & user drag

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Swing + JavaFX MP4 Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);
        frame.setLayout(new BorderLayout());

        JFXPanel jfxPanel = new JFXPanel();
        jfxPanel.setPreferredSize(new Dimension(800, 600));
        frame.add(jfxPanel, BorderLayout.CENTER);

        // Controls panel
        JPanel controls = new JPanel();
        JButton openBtn = new JButton("Open MP4...");
        JButton playBtn = new JButton("Play");
        JButton pauseBtn = new JButton("Pause");

        controls.add(openBtn);
        controls.add(playBtn);
        controls.add(pauseBtn);
        frame.add(controls, BorderLayout.SOUTH);

        // Seek bar
        JSlider seekBar = new JSlider(0, 100, 0);
        frame.add(seekBar, BorderLayout.NORTH);

        frame.pack();
        frame.setVisible(true);

        // --- Button Actions ---
        openBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select an MP4 file");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP4 files", "mp4"));
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                String path = file.toURI().toString();
                Platform.runLater(() -> initFX(jfxPanel, path, seekBar));
            }
        });

        playBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.play();
        }));

        pauseBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        }));

        // Slider listener for seeking
        seekBar.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (mediaPlayer == null) return;
                if (seekBar.getValueIsAdjusting()) {
                    isSeeking = true; // User is dragging
                    double percent = seekBar.getValue() / 100.0;
                    Platform.runLater(() -> {
                        if (mediaPlayer.getTotalDuration() != null) {
                            Duration seekTo = mediaPlayer.getTotalDuration().multiply(percent);
                            mediaPlayer.seek(seekTo);
                        }
                    });
                } else {
                    isSeeking = false;
                }
            }
        });
    }

    private void initFX(JFXPanel jfxPanel, String videoPath, JSlider seekBar) {
        // Stop previous video
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        Media media = new Media(videoPath);
        mediaPlayer = new MediaPlayer(media);

        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(800);
        mediaView.setFitHeight(600);

        StackPane root = new StackPane(mediaView);
        Scene scene = new Scene(root, 800, 600);
        jfxPanel.setScene(scene);

        // Update seek bar as video plays
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSeeking && mediaPlayer.getTotalDuration() != null) {
                double percent = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100;
                SwingUtilities.invokeLater(() -> seekBar.setValue((int) percent));
            }
        });

        mediaPlayer.setOnError(() -> {
            String err = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Unknown error";
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                            "Video playback error: " + err,
                            "Playback Error",
                            JOptionPane.ERROR_MESSAGE));
        });

        // Warn about long videos
        mediaPlayer.setOnReady(() -> {
            if (mediaPlayer.getTotalDuration().greaterThan(Duration.minutes(30))) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null,
                                "Warning: This is a long video and may freeze the player on some systems.",
                                "Video Warning",
                                JOptionPane.WARNING_MESSAGE));
            }
        });

        mediaPlayer.play();
    }
}
