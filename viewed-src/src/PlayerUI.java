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

import java.util.prefs.Preferences;
import java.util.*;

public class PlayerUI {


    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private JFrame frame;

    private boolean isSeeking = false;
    private static final int MAX_RECENT_FILES = 5;
    private final Preferences prefs = Preferences.userNodeForPackage(PlayerUI.class);
    private final ArrayList<Object> recentFiles = new ArrayList<>();
    private final JMenu recentFilesMenu = new JMenu("Recent Files");
    private boolean isFullscreen = false;
    private Rectangle previousBounds;



    private void openMediaFile(File file) {
        try {
            // Run this on the JavaFX thread
            javafx.application.Platform.runLater(() -> {
                try {
                    // Create new media and player
                    Media media = new Media(file.toURI().toString());
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                    }

                    mediaPlayer = new MediaPlayer(media);
                    mediaView.setMediaPlayer(mediaPlayer);

                    // Start playback
                    mediaPlayer.play();

                    // Update the recent files list
                    addRecentFile(file.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Could not open file:\n" + file.getName() + "\n" + ex.getMessage());
                }
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Error while trying to open media file:\n" + ex.getMessage());
        }
    }


    private void loadRecentFiles() {
        recentFiles.clear();
        for (int i = 0; i <= MAX_RECENT_FILES; i++) {
            String path = prefs.get("recentFile" + i, null);
            if (path != null) {
                recentFiles.add(path); // still storing strings
            }
        }
        refreshRecentFilesMenu();
    }

    private void addRecentFile(String path) {
        // Remove duplicate entries (compare as strings)
        recentFiles.removeIf(obj -> obj instanceof String && obj.equals(path));
        recentFiles.add(0, path);

        while (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        for (int i = 0; i < recentFiles.size(); i++) {
            Object obj = recentFiles.get(i);
            if (obj instanceof String s) {
                prefs.put("recentFile" + i, s);
            }
        refreshRecentFilesMenu();
    }}

    private void refreshRecentFilesMenu() {
        recentFilesMenu.removeAll();
        boolean hasFiles = false; // <-- declare outside the loop
        for (Object obj : recentFiles) {
            if (obj instanceof String path) {
                JMenuItem item = new JMenuItem(path);
                item.addActionListener(e -> openMediaFile(new File(path)));
                recentFilesMenu.add(item);
                hasFiles = true; // <-- update inside loop
            }
        }


        if (!hasFiles) {
            JMenuItem empty = new JMenuItem("No recent files :(");
            empty.setEnabled(false);
            recentFilesMenu.add(empty);
        }
    }

    private void logMediaInfo(Media media) {
        System.out.println("=== JavaFX Backend Dump (GStreamer) ===");
        System.out.println("Source: " + media.getSource());
        media.getMetadata().forEach((key, value) ->
                System.out.println(key + ": " + value)
        );
        System.out.println("=========================");
    }



    public void createAndShowGUI() {
        System.out.println("PlayerUI.class has startedH");
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
        fileMenu.add(recentFilesMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem reloadItem = new JMenuItem("Reload Player");
        JMenuItem fullscreenItem = new JMenuItem("Toggle Fullscreen");
        viewMenu.add(fullscreenItem);
        menuBar.add(viewMenu);
        fullscreenItem.addActionListener(e -> {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

            if (!isFullscreen) {
                // Save current window size/location
                previousBounds = frame.getBounds();

                // Hide decorations (title bar, etc.)
                frame.dispose();
                frame.setUndecorated(true);
                frame.setVisible(true);

                // Maximize to full screen
                gd.setFullScreenWindow(frame);
                System.out.println("Entered Fullscreen");

                isFullscreen = true;
            } else {
                // Exit fullscreen
                gd.setFullScreenWindow(null);
                frame.dispose();
                frame.setUndecorated(false);
                frame.setBounds(previousBounds);
                frame.setVisible(true);

                isFullscreen = false;
                System.out.println("Exited Fullscreen");
            }
        });


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

        JMenuItem moduleB = new JMenuItem("Nothing.");
        toolsMenu.add(moduleA);
        toolsMenu.add(moduleB);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        frame.setJMenuBar(menuBar);
        System.out.println("Init Menubar Completed");

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
        System.out.println("buttonPanel init completed");

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
        System.out.println("Init sequence completed!");

        // --- Action handlers ---
        openItem.addActionListener(e -> openVideo(frame, jfxPanel, seekBar, volumeSlider, timeLabel));
        exitItem.addActionListener(e -> System.exit(0));
        reloadItem.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            JOptionPane.showMessageDialog(frame, "Player reloaded!");
            System.out.println("Player reloaded");
        }));

        // Misc. menu
        moduleA.addActionListener(e -> new About().run());

        moduleB.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "TODO");
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
