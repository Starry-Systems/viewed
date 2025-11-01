import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class PlayerUI {

    private JFrame frame;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private VisualizerPanel visualizerPanel;
    private JLabel albumArtLabel;

    private boolean isFullscreen = false;
    private Rectangle previousBounds;

    private static final int MAX_RECENT_FILES = 5;
    private final Preferences prefs = Preferences.userNodeForPackage(PlayerUI.class);
    private final ArrayList<Object> recentFiles = new ArrayList<>();
    private final JMenu recentFilesMenu = new JMenu("Recent Files");

    private boolean isSeeking = false;

    public void createAndShowGUI() {
        frame = new JFrame("Viewed Player");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // Menu Bar
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open File...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        fileMenu.add(recentFilesMenu);
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem fullscreenItem = new JMenuItem("Toggle Fullscreen");
        viewMenu.add(fullscreenItem);
        menuBar.add(viewMenu);

        JMenu miscMenu = new JMenu("Misc");
        JMenuItem aboutItem = new JMenuItem("About");
        miscMenu.add(aboutItem);
        menuBar.add(miscMenu);

        frame.setJMenuBar(menuBar);

        // Center Panel for media
        JPanel centerPanel = new JPanel(new CardLayout());

        // --- Video Panel ---
        JFXPanel videoPanel = new JFXPanel();
        mediaView = new MediaView();
        visualizerPanel = new VisualizerPanel();
        albumArtLabel = new JLabel();
        albumArtLabel.setHorizontalAlignment(SwingConstants.CENTER);

        centerPanel.add(videoPanel, "VIDEO");
        centerPanel.add(visualizerPanel, "VISUALIZER");
        centerPanel.add(albumArtLabel, "ALBUM_ART");

        frame.add(centerPanel, BorderLayout.CENTER);

        // Control Panel
        JPanel controls = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel();
        JButton playBtn = new JButton("▶ Play");
        JButton pauseBtn = new JButton("⏸ Pause");
        buttons.add(playBtn);
        buttons.add(pauseBtn);

        JPanel volumeTimePanel = new JPanel();
        JSlider volumeSlider = new JSlider(0, 100, 100);
        JSlider seekBar = new JSlider(0, 100, 0);
        JLabel timeLabel = new JLabel("00:00 / 00:00");

        volumeTimePanel.add(new JLabel("🔊"));
        volumeTimePanel.add(volumeSlider);
        volumeTimePanel.add(timeLabel);

        controls.add(buttons, BorderLayout.WEST);
        controls.add(volumeTimePanel, BorderLayout.EAST);

        frame.add(controls, BorderLayout.SOUTH);
        frame.add(seekBar, BorderLayout.NORTH);

        frame.setVisible(true);

        // --- Action Listeners ---
        openItem.addActionListener(e -> chooseFile(centerPanel, videoPanel, seekBar, volumeSlider, timeLabel));
        exitItem.addActionListener(e -> System.exit(0));
        fullscreenItem.addActionListener(e -> toggleFullscreen());

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
                    if (mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                        mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(percent));
                    }
                });
            } else {
                isSeeking = false;
            }
        });

        aboutItem.addActionListener(e -> new About().run());

        loadRecentFiles();
    }

    private void chooseFile(JPanel centerPanel, JFXPanel videoPanel, JSlider seekBar, JSlider volumeSlider, JLabel timeLabel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Media File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Media Files", "mp4", "mp3"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String ext = getFileExtension(file);

            if (ext.equalsIgnoreCase("mp4")) {
                openVideo(file, centerPanel, videoPanel, seekBar, volumeSlider, timeLabel);
            } else if (ext.equalsIgnoreCase("mp3")) {
                openAudio(file, centerPanel);
            }
        }
    }

    private void openVideo(File file, JPanel centerPanel, JFXPanel videoPanel, JSlider seekBar, JSlider volumeSlider, JLabel timeLabel) {
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            StackPane root = new StackPane();
            root.getChildren().add(mediaView);

            Scene scene = new Scene(root, 800, 600, Color.BLACK);
            videoPanel.setScene(scene);

            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "VIDEO");

            mediaPlayer.play();
        });
    }

    private void openAudio(File file, JPanel centerPanel) {
        Platform.runLater(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Media media = new Media(file.toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            visualizerPanel.attachMediaPlayer(mediaPlayer);
            albumArtLabel.setIcon(AlbumArtLoader.loadAlbumArt(file));

            ((CardLayout) centerPanel.getLayout()).show(centerPanel, "VISUALIZER");

            mediaPlayer.play();
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

    private String getFileExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return (dot == -1) ? "" : name.substring(dot + 1);
    }

    // --- Recent Files ---
    private void loadRecentFiles() {
        recentFiles.clear();
        for (int i = 0; i < MAX_RECENT_FILES; i++) {
            String path = prefs.get("recentFile" + i, null);
            if (path != null) recentFiles.add(path);
        }
        refreshRecentFilesMenu();
    }

    private void addRecentFile(String path) {
        recentFiles.removeIf(obj -> obj instanceof String && obj.equals(path));
        recentFiles.add(0, path);

        while (recentFiles.size() > MAX_RECENT_FILES) recentFiles.remove(recentFiles.size() - 1);

        for (int i = 0; i < recentFiles.size(); i++) {
            Object obj = recentFiles.get(i);
            if (obj instanceof String s) prefs.put("recentFile" + i, s);
        }

        refreshRecentFilesMenu();
    }

    private void refreshRecentFilesMenu() {
        recentFilesMenu.removeAll();
        boolean hasFiles = false;
        for (Object obj : recentFiles) {
            if (obj instanceof String path) {
                JMenuItem item = new JMenuItem(path);
                item.addActionListener(e -> chooseFileFromPath(path));
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

    private void chooseFileFromPath(String path) {
        File f = new File(path);
        if (f.exists()) {
            String ext = getFileExtension(f);
            if (ext.equalsIgnoreCase("mp4")) {
                // For simplicity, assuming videoPanel etc. are accessible here
            } else if (ext.equalsIgnoreCase("mp3")) {
                // Same for audio
            }
        }
    }
}
