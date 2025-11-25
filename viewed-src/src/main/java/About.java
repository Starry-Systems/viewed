import javax.swing.*;

public class About {
    public void run() {
        JFrame frame = new JFrame("About Viewed Player");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(350, 200);

        JLabel label = new JLabel(
                "<html><div style='text-align: center;'>"
                        + "<h2>Viewed Media Player</h2>"
                        + "<p>By Starry Systems</p>"
                        + "<p>Build: 1A01</p>"
                        + "<p>Application Programming: Ben House</p>"
                        + "<p>Web Programming: Lakshin Hemachandran</p>"
                        + "<p>Made with ❤️, Java and IntelliJ IDEA</p>"
                        + "<p><em>Viewed is published under the GNU Affero General Public License v3.0</em></p>"
                        + "</div></html>",
                SwingConstants.CENTER
        );

        frame.add(label);
        frame.setLocationRelativeTo(null); // center on screen
        frame.setVisible(true);
        System.out.println("Called about.class");
    }
}
