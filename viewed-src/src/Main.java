public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            PlayerUI playerUI = new PlayerUI();
            playerUI.createAndShowGUI();
        });
    }
}
