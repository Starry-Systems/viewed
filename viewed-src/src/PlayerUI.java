import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioSpectrumListener;
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
import javafx.util.Duration;


public class PlayerUI {

    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private JFrame frame;
    private JFXPanel fxPanel;
    private boolean isSeeking = false;

    private static final int MAX_RECENT_FILES = 5;
    private final Preferences prefs = Preferences.userNodeForPackage(PlayerUI.class);
    private final ArrayList<String> recentFiles = new ArrayList<>();
    private final JMenu recentFilesMenu = new JMenu("Recent Files");
    private boolean isFullscreen = false;
    private Rectangle previousBounds;

    private StatsOverlay statsOverlay;

    // Controls
    private JSlider seekBar;
    private JLabel timeLabel;
    private JSlider volumeSlider;

    // MP3 album art & visualizer
    private JPanel albumArtPanel;
    private JLabel albumArtLabel;
    private VisualizerPanel visualizerPanel;

    public void createAndShowGUI() {
        frame = new JFrame("Viewed - Alpha");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLayout(new BorderLayout());

        // --- Menu Bar ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open Media...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        fileMenu.add(recentFilesMenu);

        JMenu viewMenu = new JMenu("View");
        JMenuItem fullscreenItem = new JMenuItem("Toggle Fullscreen");
        viewMenu.add(fullscreenItem);

        JMenu playbackMenu = new JMenu("Playback");
        JCheckBoxMenuItem toggleStats = new JCheckBoxMenuItem("Show Stats Overlay");
        playbackMenu.add(toggleStats);

        JMenu toolsMenu = new JMenu("Misc.");
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem moduleB = new JMenuItem("Nothing.");
        toolsMenu.add(aboutItem);
        toolsMenu.add(moduleB);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(playbackMenu);
        menuBar.add(toolsMenu);
        frame.setJMenuBar(menuBar);

        // --- Player area ---
        fxPanel = new JFXPanel();

        // MediaView for video
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        // Stats overlay
        statsOverlay = new StatsOverlay();
        statsOverlay.setVisible(false);

        // Album art panel
        albumArtPanel = new JPanel(new BorderLayout());
        albumArtLabel = new JLabel("", SwingConstants.CENTER);
        albumArtPanel.add(albumArtLabel, BorderLayout.CENTER);

        // Visualizer panel
        visualizerPanel = new VisualizerPanel();
        albumArtPanel.add(visualizerPanel, BorderLayout.SOUTH);
        albumArtPanel.setVisible(false);

        // Root pane
        StackPane root = new StackPane(mediaView, statsOverlay);
        Scene scene = new Scene(root, 800, 600, Color.BLACK);
        Platform.runLater(() -> fxPanel.setScene(scene));
        frame.add(fxPanel, BorderLayout.CENTER);
        frame.add(albumArtPanel, BorderLayout.CENTER);

        // --- Controls ---
        JPanel controls = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JButton playBtn = new JButton("▶ Play");
        JButton pauseBtn = new JButton("⏸ Pause");
        buttonPanel.add(playBtn);
        buttonPanel.add(pauseBtn);

        JPanel volumePanel = new JPanel();
        timeLabel = new JLabel("00:00 / 00:00");
        volumeSlider = new JSlider(0, 100, 100);
        volumePanel.add(new JLabel("🔊"));
        volumePanel.add(volumeSlider);
        volumePanel.add(timeLabel);

        controls.add(buttonPanel, BorderLayout.WEST);
        controls.add(volumePanel, BorderLayout.EAST);
        frame.add(controls, BorderLayout.SOUTH);

        // --- Seek bar ---
        seekBar = new JSlider(0, 100, 0);
        frame.add(seekBar, BorderLayout.NORTH);

        frame.setVisible(true);

        // --- Action handlers ---
        openItem.addActionListener(e -> openVideo());
        exitItem.addActionListener(e -> System.exit(0));
        fullscreenItem.addActionListener(e -> toggleFullscreen());
        toggleStats.addActionListener(e -> Platform.runLater(() -> statsOverlay.setVisible(toggleStats.isSelected())));
        aboutItem.addActionListener(e -> SwingUtilities.invokeLater(() -> new About().run()));
        moduleB.addActionListener(e -> JOptionPane.showMessageDialog(frame, "TODO"));

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

    private void openVideo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a media file");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Media files (MP4, MP3)", "mp4", "mp3"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            openMediaFile(file);
        }
    }

    private void openMediaFile(File file) {
        Platform.runLater(() -> {
            try {
                Media media = new Media(file.toURI().toString());

                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer.dispose();
                }

                mediaPlayer = new MediaPlayer(media);

                boolean isVideo = file.getName().toLowerCase().endsWith(".mp4");

                mediaView.setVisible(isVideo);
                fxPanel.setVisible(isVideo);
                albumArtPanel.setVisible(!isVideo);

                if (isVideo) {
                    mediaView.setMediaPlayer(mediaPlayer);
                } else {
                    // For MP3: setup audio visualizer
                    visualizerPanel.reset();
                    mediaPlayer.setAudioSpectrumListener(new AudioSpectrumListener() {
                        @Override
                        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
                            visualizerPanel.updateSpectrum(magnitudes);
                        }
                    });
                    albumArtLabel.setIcon(new ImageIcon("resources/default_album.png")); // fallback image
                }

                mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
                    if (!isSeeking && mediaPlayer.getTotalDuration() != null && !mediaPlayer.getTotalDuration().isUnknown()) {
                        double percent = newTime.toMillis() / mediaPlayer.getTotalDuration().toMillis() * 100;
                        SwingUtilities.invokeLater(() -> {
                            seekBar.setValue((int) percent);
                            timeLabel.setText(formatTime(newTime) + " / " + formatTime(mediaPlayer.getTotalDuration()));
                        });
                    }
                });

                mediaPlayer.setOnReady(() -> SwingUtilities.invokeLater(() ->
                        timeLabel.setText("00:00 / " + formatTime(mediaPlayer.getTotalDuration()))
                ));

                mediaPlayer.play();
                addRecentFile(file.getAbsolutePath());

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame,
                        "Could not open file:\n" + file.getName() + "\n" + ex.getMessage());
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

    private String formatTime(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
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
        recentFiles.remove(path);
        recentFiles.add(0, path);
        while (recentFiles.size() > MAX_RECENT_FILES) recentFiles.remove(recentFiles.size() - 1);
        for (int i = 0; i < recentFiles.size(); i++) prefs.put("recentFile" + i, recentFiles.get(i));
        refreshRecentFilesMenu();
    }

    private void refreshRecentFilesMenu() {
        recentFilesMenu.removeAll();
        if (recentFiles.isEmpty()) {
            JMenuItem empty = new JMenuItem("No recent files :(");
            empty.setEnabled(false);
            recentFilesMenu.add(empty);
        } else {
            for (String path : recentFiles) {
                JMenuItem item = new JMenuItem(path);
                item.addActionListener(e -> openMediaFile(new File(path)));
                recentFilesMenu.add(item);
            }
        }
    }

    // --- Simple visualizer panel ---
    static class VisualizerPanel extends JPanel {
        private float[] magnitudes;

        public void updateSpectrum(float[] mags) {
            this.magnitudes = mags.clone();
            repaint();
        }

        public void reset() {
            this.magnitudes = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (magnitudes == null) return;
            int width = getWidth();
            int height = getHeight();
            int barWidth = Math.max(1, width / magnitudes.length);
            g.setColor(Color.GREEN);
            for (int i = 0; i < magnitudes.length; i++) {
                int barHeight = (int) ((magnitudes[i] + 60) * 2); // simple scale
                g.fillRect(i * barWidth, height - barHeight, barWidth - 1, barHeight);
            }
        }
    }
}
