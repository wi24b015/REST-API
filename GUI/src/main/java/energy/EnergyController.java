package energy;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;


import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class EnergyController {

    private static final String API_BASE_URL = "http://localhost:8080";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();


    // Verbindung zu den Labels (fx:id)
    @FXML
    private Label lblPoolUsed;
    @FXML
    private Label lblGridPortion;
    @FXML
    private Label lblProduced;
    @FXML
    private Label lblUsed;
    @FXML
    private Label lblGrid;

    // Verbindung zu den Kalendern
    @FXML
    private DatePicker dpStart;
    @FXML
    private DatePicker dpEnd;

    @FXML
    public void onRefreshClick() {
        try {
            CurrentEnergy current = sendGet("/energy/current", CurrentEnergy.class);

            lblPoolUsed.setText(formatPercent(current.getCommunityDepleted()) + " used");
            lblGridPortion.setText(formatPercent(current.getGridPortion()));
        } catch (Exception e) {
            showCurrentError();
            e.printStackTrace();
        }
    }

    @FXML
    public void onShowDataClick() {
        if (dpStart.getValue() == null || dpEnd.getValue() == null) {
            lblProduced.setText("Start/End fehlt");
            lblUsed.setText("Start/End fehlt");
            lblGrid.setText("Start/End fehlt");
            return;
        }

        try {
            String start = encode(dpStart.getValue().toString());
            String end = encode(dpEnd.getValue().toString());

            String path = "/energy/historical?start=" + start + "&end=" + end;

            HistoricalEnergy[] values = sendGet(path, HistoricalEnergy[].class);

            double produced = Arrays.stream(values)
                    .mapToDouble(HistoricalEnergy::getCommunityProduced)
                    .sum();

            double used = Arrays.stream(values)
                    .mapToDouble(HistoricalEnergy::getCommunityUsed)
                    .sum();

            double grid = Arrays.stream(values)
                    .mapToDouble(HistoricalEnergy::getGridUsed)
                    .sum();

            lblProduced.setText(formatKwh(produced));
            lblUsed.setText(formatKwh(used));
            lblGrid.setText(formatKwh(grid));

        } catch (Exception e) {
            showHistoricalError();
            e.printStackTrace();
        }
    }


    private <T> T sendGet(String path, Class<T> responseType) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + path))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("REST API returned HTTP " + response.statusCode());
        }

        return objectMapper.readValue(response.body(), responseType);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formatPercent(double value) {
        return String.format("%.2f%%", value);
    }

    private String formatKwh(double value) {
        return String.format("%.3f kWh", value);
    }

    private void showCurrentError() {
        lblPoolUsed.setText("API Fehler");
        lblGridPortion.setText("API Fehler");
    }

    private void showHistoricalError() {
        lblProduced.setText("API Fehler");
        lblUsed.setText("API Fehler");
        lblGrid.setText("API Fehler");
    }



}






