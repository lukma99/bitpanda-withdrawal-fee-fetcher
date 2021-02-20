import okhttp3.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;


public class main implements Job {

    static double currentPrice; // Current price of bitcoin in EUR
    static String currentCoin = "BTC";
    static OkHttpClient client = new OkHttpClient();
    static String BITCOIN_PRICE_ENDPOINT = "https://api.bitpanda.com/v1/ticker";
    final static String BITPANDA_FEES_ENDPOINT = "https://www.bitpanda.com/de/limits";


    static JTextField tfBtcPriceInEur = new JTextField();
    static JTextField tfWithdrawalFeesInBtc = new JTextField();
    static JTextField tfWithdrawalFeesInEur = new JTextField();


    /**
     * Main method. Initializes Swing objects and runs the initial method
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {
        // Initialize Gui
        initializeGui();

        try {

            // specify the job' s details..
            JobDetail job = JobBuilder.newJob(main.class)
                    .withIdentity("fetchData")
                    .build();

            // specify the running period of the job
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(30)
                            .repeatForever())
                    .build();

            //schedule the job
            SchedulerFactory schFactory = new StdSchedulerFactory();
            Scheduler sch = schFactory.getScheduler();
            sch.start();
            sch.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            e.printStackTrace();
        }


        // Initial run
        run();
    }


    /**
     * Creates all elements for the GUI and arranges them
     */
    public static void initializeGui() throws IOException {
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
        jPanelTop.setLayout(new java.awt.GridLayout(4, 2));
        jPanelMain.add(jPanelTop);

        JPanel jPanelBottom = new JPanel();
        jPanelBottom.setLayout(new java.awt.GridLayout(1, 1));
        jPanelMain.add(jPanelBottom);


        JLabel labelCurrency = new JLabel("Coin");
        jPanelTop.add(labelCurrency);

        
        ArrayList<String> comboBoxListe = getSupportedCurrencies();
        Collections.sort(comboBoxListe);


        JComboBox comboboxCoins = new JComboBox(comboBoxListe.toArray());
        comboboxCoins.setSelectedItem("BTC");
        comboboxCoins.addActionListener(e -> {
            currentCoin = comboboxCoins.getSelectedItem().toString();
            System.out.println(currentCoin);
            run();
        });

        jPanelTop.add(comboboxCoins);


        JLabel labelBtcPriceInEur = new JLabel("Price in EUR");
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

/*
        JButton buttonReload = new JButton("Reload Values");
        buttonReload.addActionListener(e -> run()); // Add Click Event
        jPanelBottom.add(buttonReload);
*/
        // Disclaimer
        JLabel labelDisclaimer = new JLabel("<html>This tool is NOT an official tool by Bitpanda.<br>" +
                "New data is fetched every 30 seconds.<br>" +
                "No guarantee for correct data.<br></html>");
        jPanelBottom.add(labelDisclaimer);

        frame.pack();
    }


    /**
     * Method, which manages the calls to the other methods
     */
    public static void run() {
        //BITCOIN_PRICE_ENDPOINT = "https://api.coingecko.com/api/v3/simple/price?ids="+ currentCoin +"&vs_currencies=eur";
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
        currentPrice = jsonObject.getJSONObject(currentCoin).getDouble("EUR");
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

            currentCurrency = currentWithdrawalFee.replaceAll("[^A-Za-z]+", "");

            if (currentCurrency.equals(currentCoin)) {
                String bitcoinWithdrawalFee = currentWithdrawalFee;
                // Cut the " BTC" at the end of the String
                bitcoinWithdrawalFee = bitcoinWithdrawalFee.replaceAll("[^\\d.]", "");

                System.out.println(bitcoinWithdrawalFee);
                double bitcoinWithdrawalFeeDouble = Double.parseDouble(bitcoinWithdrawalFee);

                // set Text to current fees
                tfBtcPriceInEur.setText(currentPrice + " EUR");
                tfWithdrawalFeesInBtc.setText(String.format("%.8f", bitcoinWithdrawalFeeDouble) + " " + currentCoin);
                tfWithdrawalFeesInEur.setText(String.valueOf(String.format("%.2f", (bitcoinWithdrawalFeeDouble * currentPrice))) + " EUR");
            }
        }
    }


    public static ArrayList<String> getSupportedCurrencies() throws IOException {

        ArrayList<String> list = new ArrayList<>();

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

            currentCurrency = currentWithdrawalFee.replaceAll("[^A-Za-z]+", "");

            list.add(currentCurrency);
        }
        return list;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        run();
    }
}
