import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;

public class PlayerUI {

    private MediaPlayer mediaPlayer;
    private boolean isSeeking = false;

    public void createAndShowGUI() {
        JFrame frame = new JFrame("Viewed - Alpha");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open MP4...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        JMenuItem reloadItem = new JMenuItem("Reload Player");
        viewMenu.add(reloadItem);

        JMenu toolsMenu = new JMenu("Misc.");
        JMenuItem moduleA = new JMenuItem("About");
        moduleA.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                // Prevent duplicate triggers by ensuring we're on the Swing thread
                for (Frame f : JFrame.getFrames()) {
                    if (f.getTitle().equals("About Viewed Player")) {
                        f.toFront();
                        return; // already open
                    }
                }
                new About().run();
            });
        });

        JMenuItem moduleB = new JMenuItem("Launch Module B");
        toolsMenu.add(moduleA);
        toolsMenu.add(moduleB);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        frame.setJMenuBar(menuBar);

        // --- Player area ---
        JFXPanel jfxPanel = new JFXPanel();
        frame.add(jfxPanel, BorderLayout.CENTER);

        // --- Control bar (bottom) ---
        JPanel controls = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();

        JButton playBtn = new JButton("▶ Play");
        JButton pauseBtn = new JButton("⏸ Pause");

        buttonPanel.add(playBtn);
        buttonPanel.add(pauseBtn);

        // --- Volume + time display ---
        JPanel volumePanel = new JPanel();
        JLabel timeLabel = new JLabel("00:00 / 00:00");
        JSlider volumeSlider = new JSlider(0, 100, 100);
        volumePanel.add(new JLabel("🔊"));
        volumePanel.add(volumeSlider);
        volumePanel.add(timeLabel);

        controls.add(buttonPanel, BorderLayout.WEST);
        controls.add(volumePanel, BorderLayout.EAST);
        frame.add(controls, BorderLayout.SOUTH);

        // --- Seek bar ---
        JSlider seekBar = new JSlider(0, 100, 0);
        frame.add(seekBar, BorderLayout.NORTH);

        frame.setVisible(true);

        // --- Action handlers ---
        openItem.addActionListener(e -> openVideo(frame, jfxPanel, seekBar, volumeSlider, timeLabel));
        exitItem.addActionListener(e -> System.exit(0));
        reloadItem.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            JOptionPane.showMessageDialog(frame, "Player reloaded!");
        }));

        // Misc. menu
        moduleA.addActionListener(e -> new About().run());

        moduleB.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "Module B launched!");
        });

        playBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.play();
        }));
        pauseBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        }));

        volumeSlider.addChangeListener(e -> {
            if (mediaPlayer != null && !volumeSlider.getValueIsAdjusting()) {
                Platform.runLater(() -> mediaPlayer.setVolume(volumeSlider.getValue() / 100.0));
            }
        });

        seekBar.addChangeListener((ChangeEvent e) -> {
            if (mediaPlayer == null) return;
            if (seekBar.getValueIsAdjusting()) {
                isSeeking = true;
                double percent = seekBar.getValue() / 100.0;
                Platform.runLater(() -> {
                    Duration total = mediaPlayer.getTotalDuration();
                    if (total != null && !total.isUnknown()) {
                        mediaPlayer.seek(total.multiply(percent));
                    }
                });
            } else {
                isSeeking = false;
            }
        });
    }

    private void openVideo(JFrame frame, JFXPanel jfxPanel, JSlider seekBar, JSlider volumeSlider, JLabel timeLabel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select an MP4 file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP4 files", "mp4"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String path = file.toURI().toString();
            Platform.runLater(() -> initFX(jfxPanel, path, seekBar, volumeSlider, timeLabel));
        }
    }

    private void initFX(JFXPanel jfxPanel, String videoPath, JSlider seekBar, JSlider volumeSlider, JLabel timeLabel) {
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

        // --- Time updates ---
        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!isSeeking && mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                double percent = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100;
                SwingUtilities.invokeLater(() -> {
                    seekBar.setValue((int) percent);
                    timeLabel.setText(formatTime(newTime) + " / " + formatTime(mediaPlayer.getTotalDuration()));
                });
            }
        });

        mediaPlayer.setOnReady(() -> {
            SwingUtilities.invokeLater(() -> {
                timeLabel.setText("00:00 / " + formatTime(mediaPlayer.getTotalDuration()));
            });
        });

        mediaPlayer.setOnError(() -> {
            String err = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Unknown error";
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null, "Playback error: " + err, "Error", JOptionPane.ERROR_MESSAGE));
        });

        // Volume sync
        Platform.runLater(() -> mediaPlayer.setVolume(volumeSlider.getValue() / 100.0));

        mediaPlayer.play();
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return new DecimalFormat("00").format(minutes) + ":" + new DecimalFormat("00").format(seconds);
    }
}
