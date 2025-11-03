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
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class PlayerUI {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private JFrame frame;
    private JFXPanel jfxPanel;

    private boolean isSeeking = false;
    private static final int MAX_RECENT_FILES = 5;
    private final Preferences prefs = Preferences.userNodeForPackage(PlayerUI.class);
    private final ArrayList<Object> recentFiles = new ArrayList<>();
    private final JMenu recentFilesMenu = new JMenu("Recent Files");
    private boolean isFullscreen = false;
    private Rectangle previousBounds;

    private JLabel fileLabel;

    public void createAndShowGUI() {
        frame = new JFrame("Viewed - V0.2 (Ready for liftoff)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Audio/Video file...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        fileMenu.add(recentFilesMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem fullscreenItem = new JMenuItem("Toggle Fullscreen");
        viewMenu.add(fullscreenItem);

        JMenu playbackMenu = new JMenu("Playback");
        JMenuItem reloadItem = new JMenuItem("Reload Player");
        playbackMenu.add(reloadItem);

        JMenu toolsMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem nothingItem = new JMenuItem("Help");
        toolsMenu.add(aboutItem);
        toolsMenu.add(nothingItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(playbackMenu);
        menuBar.add(toolsMenu);
        frame.setJMenuBar(menuBar);

        // --- Player area ---
        jfxPanel = new JFXPanel();
        frame.add(jfxPanel, BorderLayout.CENTER);

        // Label for MP3 or no video
        fileLabel = new JLabel("No media loaded :(");
        fileLabel.setHorizontalAlignment(SwingConstants.CENTER);
        fileLabel.setVerticalAlignment(SwingConstants.CENTER);
        fileLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        jfxPanel.setLayout(new BorderLayout());
        jfxPanel.add(fileLabel, BorderLayout.CENTER);

        // --- Control bar ---
        JPanel controls = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JButton playBtn = new JButton("▶ Play");
        JButton pauseBtn = new JButton("⏸ Pause");
        buttonPanel.add(playBtn);
        buttonPanel.add(pauseBtn);

        JPanel volumePanel = new JPanel();
        JLabel timeLabel = new JLabel("00:00 / 00:00");
        JSlider volumeSlider = new JSlider(0, 100, 100);
        volumePanel.add(new JLabel("🔊"));
        volumePanel.add(volumeSlider);
        volumePanel.add(timeLabel);

        controls.add(buttonPanel, BorderLayout.WEST);
        controls.add(volumePanel, BorderLayout.EAST);
        frame.add(controls, BorderLayout.SOUTH);

        // Seek bar
        JSlider seekBar = new JSlider(0, 100, 0);
        frame.add(seekBar, BorderLayout.NORTH);

        frame.setVisible(true);

        // --- Action handlers ---
        openItem.addActionListener(e -> chooseFile());
        exitItem.addActionListener(e ->
                System.exit(0));

        fullscreenItem.addActionListener(e -> toggleFullscreen());

        reloadItem.addActionListener(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }
            JOptionPane.showMessageDialog(frame, "Player reloaded!");
        });

        aboutItem.addActionListener(e -> new About().run());
        nothingItem.addActionListener(e -> JOptionPane.showMessageDialog(frame, "There currently isn't any help available. \n There will be by V1."));

        playBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.play();
        }));
        pauseBtn.addActionListener(e -> Platform.runLater(() -> {
            if (mediaPlayer != null) mediaPlayer.pause();
        }));

        volumeSlider.addChangeListener((ChangeEvent e) -> {
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

        loadRecentFiles();
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a media file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Audio/Video Files", "mp4", "mp3", "wav", "aac", "aiff"));

        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            // User canceled, just return
            return;
        }

        File file = chooser.getSelectedFile();
        if (file == null) {
            // Safety check (shouldn't happen if APPROVE_OPTION)
            return;
        }

        String path = file.getAbsolutePath().toLowerCase();
        if (path.endsWith(".mp4")) {
            openMediaFile(file);
        } else if (path.endsWith(".mp3") || path.endsWith(".wav") || path.endsWith(".aiff") || path.endsWith(".aac")) {
            openAudio(file);
        } else {
            JOptionPane.showMessageDialog(frame, "Unsupported file type!");
        }
    }


    private void openMediaFile(File file) {
        if (file == null) return;

        Platform.runLater(() -> {
            try {
                Media media = new Media(file.toURI().toString());

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }

                mediaPlayer = new MediaPlayer(media);

                // MediaView
                if (mediaView == null) {
                    mediaView = new MediaView(mediaPlayer);
                } else {
                    mediaView.setMediaPlayer(mediaPlayer);
                }

                StackPane root = new StackPane();
                root.getChildren().add(mediaView);

                mediaView.setPreserveRatio(true);
                mediaView.fitWidthProperty().bind(root.widthProperty());
                mediaView.fitHeightProperty().bind(root.heightProperty());

                Scene scene = new Scene(root, 800, 600, javafx.scene.paint.Color.BLACK);
                jfxPanel.setScene(scene);

                fileLabel.setVisible(false);
                mediaPlayer.play();

                addRecentFile(file.getAbsolutePath());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Could not open file:\n" + file.getName() + "\n" + e.getMessage());
            }
        });
    }

    private void openAudio(File file) {
        if (file == null) return;

        Platform.runLater(() -> {
            try {
                Media media = new Media(file.toURI().toString());

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }

                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.play();

                fileLabel.setText(file.getName());
                fileLabel.setVisible(true);

                addRecentFile(file.getAbsolutePath());

            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame,
                        "Could not open audio:\n" + file.getName() + "\n" + e.getMessage());
            }
        });
    }

    private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!isFullscreen) {
            previousBounds = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setVisible(true);
            gd.setFullScreenWindow(frame);
            isFullscreen = true;
        } else {
            gd.setFullScreenWindow(null);
            frame.dispose();
            frame.setUndecorated(false);
            frame.setBounds(previousBounds);
            frame.setVisible(true);
            isFullscreen = false;
        }
    }

    private void loadRecentFiles() {
        recentFiles.clear();
        for (int i = 0; i <= MAX_RECENT_FILES; i++) {
            String path = prefs.get("recentFile" + i, null);
            if (path != null) {
                recentFiles.add(path);
            }
        }
        refreshRecentFilesMenu();
    }

    private void addRecentFile(String path) {
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
        }
        refreshRecentFilesMenu();
    }

    private void refreshRecentFilesMenu() {
        recentFilesMenu.removeAll();
        boolean hasFiles = false;
        for (Object obj : recentFiles) {
            if (obj instanceof String path) {
                JMenuItem item = new JMenuItem(path);
                item.addActionListener(e -> openMediaFile(new File(path)));
                recentFilesMenu.add(item);
                hasFiles = true;
            }
        }
        if (!hasFiles) {
            JMenuItem empty = new JMenuItem("No recent files :(");
            empty.setEnabled(false);
            recentFilesMenu.add(empty);
        }
    }

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return new DecimalFormat("00").format(minutes) + ":" + new DecimalFormat("00").format(seconds);
    }
}
