package com.trading.backtest.service;

import com.opencsv.CSVWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Service
public class BinanceClient {

    @Autowired
    RestTemplate restTemplate;

    private static final Map<String, Long> intervalToMillisMap = new HashMap<>();
    private static final String BINANCE_API_URL = "https://api.binance.com/api/v3/klines";


    static {
        intervalToMillisMap.put("15m", 15L * 60 * 1000);
        intervalToMillisMap.put("30m", 30L * 60 * 1000);
        intervalToMillisMap.put("1h", 60L * 60 * 1000);
        intervalToMillisMap.put("4h", 4L * 60 * 60 * 1000);
    }

    public String fetchFullData(String symbol, String interval, int limitPerRequest, int durationInDays) throws InterruptedException {
        if (!intervalToMillisMap.containsKey(interval)) {
            throw new IllegalArgumentException("Unsupported interval: " + interval);
        }

        long intervalMillis = intervalToMillisMap.get(interval);
        long now = System.currentTimeMillis();
        long from = now - (long) durationInDays * 24 * 60 * 60 * 1000;
        long current = from;

        StringBuilder allData = new StringBuilder("[");
        boolean first = true;

        while (current < now) {
            String url = String.format(
                    "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s&limit=%d&startTime=%d",
                    symbol, interval, limitPerRequest, current
            );

            String json = restTemplate.getForObject(url, String.class);
            if (json != null && json.length() > 2) {
                // Remove square brackets []
                String data = json.substring(1, json.length() - 1);
                if (!data.isEmpty()) {
                    if (!first) allData.append(",");
                    allData.append(data);
                    first = false;
                }
            }

            current += intervalMillis * limitPerRequest;
            Thread.sleep(300); // Avoid hitting Binance rate limits
        }

        allData.append("]");
        return allData.toString();
    }

    public void fetchHistoricalData(String symbol, String interval, int days) throws IOException {
        long endTime = System.currentTimeMillis();
        long startTime = endTime - days * 24L * 60 * 60 * 1000;
        String outputPath = "D:\\stockMaket\\csvData\\btc_data_" + interval + "_" + days+ ".csv";
        FileWriter outputfile = new FileWriter(outputPath);
        CSVWriter writer = new CSVWriter(outputfile);
        writer.writeNext(new String[]{"OpenTime", "Open", "High", "Low", "Close", "Volume", "CloseTime"});

        while (startTime < endTime) {
            String url = BINANCE_API_URL + "?symbol=" + symbol + "&interval=" + interval
                    + "&startTime=" + startTime + "&limit=1000";
            List<List<Object>> klines = restTemplate.getForObject(url, List.class);
            if (klines == null || klines.isEmpty()) break;

            for (List<Object> kline : klines) {
                String[] record = {
                        kline.get(0).toString(),
                        kline.get(1).toString(),
                        kline.get(2).toString(),
                        kline.get(3).toString(),
                        kline.get(4).toString(),
                        kline.get(5).toString(),
                        kline.get(6).toString()
                };
                writer.writeNext(record);
                startTime = ((Number) kline.get(6)).longValue();
            }

            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }

        writer.close();
        System.out.println("Data saved to: " + outputPath);
    }

    public String fetchCandles() throws IOException {
        String url = "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=4h&limit=2190";
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");

        Scanner scanner = new Scanner(conn.getInputStream());
        StringBuilder response = new StringBuilder();
        while (scanner.hasNext()) {
            response.append(scanner.nextLine());
        }
        scanner.close();
        return response.toString();
    }

    public String fetchCandlesForLastYear() throws IOException {
        StringBuilder fullResponse = new StringBuilder();
        long now = System.currentTimeMillis();
        long millisPerCandle = 4L * 60 * 60 * 1000; // 4 hours
        int candlesPerRequest = 1500;

        // Total needed: 365 * 6 = ~2190 candles => 2 requests needed
        int totalCandles = 2190;

        for (int i = 0; i < totalCandles; i += candlesPerRequest) {
            long endTime = now - i * millisPerCandle;
            long startTime = endTime - candlesPerRequest * millisPerCandle;

//            while (current < now) {
//                String url = String.format(
//                        "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=%s&limit=%d&startTime=%d",
//                        interval, limit, current
//                );

            String url = String.format(
                    "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=4h&limit=1500&startTime=%d&endTime=%d",
                    startTime, endTime
            );

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            // Remove outer brackets and add comma between chunks
            String chunk = response.toString();
            if (chunk.startsWith("[") && chunk.endsWith("]")) {
                chunk = chunk.substring(1, chunk.length() - 1);
            }
            if (fullResponse.length() > 0) fullResponse.append(",");
            fullResponse.append(chunk);
        }

        return "[" + fullResponse + "]";
    }

    public String fetchFullTwoYearData(String interval) throws IOException {
        long now = System.currentTimeMillis();
        long twoYearsAgo = now - (long) 2 * 365 * 24 * 60 * 60 * 1000;

        long current = twoYearsAgo;
        long limit = 1000;
        long intervalMillis = 60 * 60 * 1000; // 1H = 3600000 ms
        StringBuilder allData = new StringBuilder("[");

        while (current < now) {
            String url = String.format(
                    "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=%s&limit=%d&startTime=%d",
                    interval, limit, current
            );

            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();

            String json = response.toString();
            json = json.substring(1, json.length() - 1); // remove [ and ]
            if (!json.isEmpty()) {
                if (!allData.toString().endsWith("[")) allData.append(",");
                allData.append(json);
            }

            current += intervalMillis * limit;
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }

        allData.append("]");
        return allData.toString();
    }



}
