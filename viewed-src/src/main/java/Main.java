public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            long time = System.nanoTime();
            System.out.println("Starting...");
            System.out.println("Log time: " + time);
            PlayerUI playerUI = new PlayerUI();
            playerUI.createAndShowGUI();
            System.out.println("Called createAndShowGUI(), PlayerUI.class");

        });
    }
}
