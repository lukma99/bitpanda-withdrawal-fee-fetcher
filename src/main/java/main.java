import okhttp3.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;


public class main {

    static float currentPrice; // Current price of bitcoin in EUR
    static OkHttpClient client = new OkHttpClient();
    final static String BITCOIN_PRICE_ENDPOINT = "https://api.coindesk.com/v1/bpi/currentprice.json";
    final static String BITPANDA_FEES_ENDPOINT = "https://www.bitpanda.com/de/limits";


    static JTextField tfBtcPriceInEur = new JTextField();
    static JTextField tfWithdrawalFeesInBtc = new JTextField();
    static JTextField tfWithdrawalFeesInEur = new JTextField();


    /**
     * Main method. Initializes Swing objects and runs the initial method
     *
     * @param args
     */
    public static void main(String[] args) {
        // Initialize Gui
        initializeGui();


        // Initial run
        run();
    }


    /**
     * Creates all elements for the GUI and arranges them
     */
    public static void initializeGui() {
        // Create frame
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setTitle("Withdrawal Fees");
        frame.setVisible(true);


        JPanel jPanelMain = new JPanel();
        jPanelMain.setLayout(new java.awt.GridLayout(2, 1));
        frame.add(jPanelMain);

        JPanel jPanelTop = new JPanel();
        jPanelTop.setLayout(new java.awt.GridLayout(3, 3));
        jPanelMain.add(jPanelTop);

        JPanel jPanelBottom = new JPanel();
        jPanelBottom.setLayout(new java.awt.GridLayout(3, 1));
        jPanelMain.add(jPanelBottom);


        JLabel labelBtcPriceInEur = new JLabel("BTC Price in EUR");
        jPanelTop.add(labelBtcPriceInEur);

        tfBtcPriceInEur.setEditable(false);
        jPanelTop.add(tfBtcPriceInEur);

        JLabel labelWithdrawalFees = new JLabel("Bitpanda Withdrawal Fees ");
        jPanelTop.add(labelWithdrawalFees);

        tfWithdrawalFeesInBtc.setEditable(false);
        jPanelTop.add(tfWithdrawalFeesInBtc);

        JLabel labelEmpty = new JLabel("");
        jPanelTop.add(labelEmpty);

        tfWithdrawalFeesInEur.setEditable(false);
        jPanelTop.add(tfWithdrawalFeesInEur);


        JButton buttonReload = new JButton("Reload Values");
        buttonReload.addActionListener(e -> run()); // Add Click Event
        jPanelBottom.add(buttonReload);

        // Disclaimer
        JLabel labelDisclaimer = new JLabel("<html>This tool is NOT an official tool by Bitpanda.<br>" +
                "No guarantee for correct data.<br></html>");
        jPanelBottom.add(labelDisclaimer);


        // Link to CoinDesk, because they demand it here (https://www.coindesk.com/coindesk-api) if you use their API
        JLabel labelCoinDeskLink = new JLabel("<html>API for BTC price is Powered by <a href=\"https://www.coindesk.com/price/bitcoin\">CoinDesk</a></html>");
        labelCoinDeskLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://www.coindesk.com/price/bitcoin"));
                } catch (IOException | URISyntaxException e1) {
                    e1.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        jPanelBottom.add(labelCoinDeskLink);


        frame.pack();
    }


    /**
     * Method, which manages the calls to the other methods
     */
    public static void run() {
        loadBitcoinPrice(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                parseBitcoinPrice(response.body().string());
                getBitpandaWithdrawalFee();
            }

            @Override
            public void onFailure(Call call, IOException e) {
            }
        });
    }


    /**
     * Loads current bitcoin price from API
     *
     * @param callback callback
     */
    private static void loadBitcoinPrice(Callback callback) {
        Request request = new Request.Builder().url(BITCOIN_PRICE_ENDPOINT).build();
        client.newCall(request).enqueue(callback);
    }


    /**
     * Parses body from API response into JSON object and saves the bitcoin price in EUR into variable currentPrice
     *
     * @param str string, which will be converted to JSON
     */
    private static void parseBitcoinPrice(String str) {
        JSONObject jsonObject = new JSONObject(str);
        currentPrice = jsonObject.getJSONObject("bpi").getJSONObject("EUR").getFloat("rate_float");
        tfBtcPriceInEur.setText(currentPrice + " EUR");
    }


    /**
     * Searches Bitpanda withdrawal fee website for current value in BTC, calculates the price in EUR
     * and writes it into the button
     *
     * @throws IOException
     */
    public static void getBitpandaWithdrawalFee() throws IOException {
        // Set up connection to bitpanda limits site
        Document doc = Jsoup.connect(BITPANDA_FEES_ENDPOINT).get();


        // Extract rows of withdrawal table
        Element table = doc.getElementById("transactioncosts").getElementsByClass("table").first();
        Elements rows = table.getElementsByClass("table__row");

        // Check all rows, if they contain the text "Bitcoin" (so other currencies are ignored)
        for (int i = 1; i < rows.size(); i++) {
            Element row = rows.get(i);

            String currentCurrency = row.getElementsByClass("table__row__cell table__row__cell--header ").get(0).text();
            String currentWithdrawalFee = row.getElementsByClass("table__row__cell ").get(3).text();

            if (currentCurrency.equals("Bitcoin")) {
                String bitcoinWithdrawalFee = currentWithdrawalFee;
                // Cut the " BTC" at the end of the String
                bitcoinWithdrawalFee = bitcoinWithdrawalFee.substring(0, bitcoinWithdrawalFee.length() - 4);

                double bitcoinWithdrawalFeeDouble = Double.parseDouble(bitcoinWithdrawalFee);

                // set Text to current fees
                tfWithdrawalFeesInBtc.setText(String.format("%.8f", bitcoinWithdrawalFeeDouble) + " BTC");
                tfWithdrawalFeesInEur.setText(String.valueOf(String.format("%.2f", (bitcoinWithdrawalFeeDouble * currentPrice))) + " EUR");
            }
        }
    }

}
