import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;

public class StatsOverlay extends VBox {
    private final Text codecText = new Text("Codec: unknown");
    private final Text resolutionText = new Text("Resolution: unknown");
    private final Text decoderText = new Text("Decoder: unknown");

    public StatsOverlay() {
        setPadding(new Insets(10));
        setSpacing(5);
        setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0, 0.5), new CornerRadii(8), Insets.EMPTY)));

        codecText.setFill(Color.WHITE);
        resolutionText.setFill(Color.WHITE);
        decoderText.setFill(Color.WHITE);
        codecText.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        resolutionText.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        decoderText.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));

        getChildren().addAll(codecText, resolutionText, decoderText);
        setVisible(false);


    }

    public void update(String codec, String resolution, String decoder) {
        codecText.setText("Codect: " + codec);
        resolutionText.setText("Resolution: " + resolution);
        decoderText.setText("Decoder: " + decoder);
    }
}
