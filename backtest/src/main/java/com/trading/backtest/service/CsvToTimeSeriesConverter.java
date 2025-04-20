package com.trading.backtest.service;

import org.json.JSONArray;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvToTimeSeriesConverter {
    public BarSeries convertFromJson(String jsonData, String timeFrame) {
        JSONArray candles = new JSONArray(jsonData);
        List<Bar> bars = new ArrayList<>();
        Duration barDuration = mapIntervalToDuration(timeFrame);
        for (int i = 0; i < candles.length(); i++) {
            JSONArray c = candles.getJSONArray(i);
            ZonedDateTime endTime = Instant.ofEpochMilli(c.getLong(0)).atZone(ZoneId.of("Asia/Kolkata"));
            Num open = DecimalNum.valueOf(c.getString(1));
            Num high = DecimalNum.valueOf(c.getString(2));
            Num low = DecimalNum.valueOf(c.getString(3));
            Num close = DecimalNum.valueOf(c.getString(4));
            Num volume = DecimalNum.valueOf(c.getString(5));
            Num amount = DecimalNum.valueOf("0");
            Num tradeCount = DecimalNum.valueOf("0");

            Bar bar = new BaseBar(
                    barDuration,
                    endTime,
                    open,
                    high,
                    low,
                    close,
                    volume,
                    DecimalNum.valueOf("0") // amount (or volume in quote asset)
            );
            bars.add(bar);
        }
        return new BaseBarSeries("BTC/USDT", bars);
    }

    private Duration mapIntervalToDuration(String interval) {
        return switch (interval) {
            case "15m" -> Duration.ofMinutes(15);
            case "30m" -> Duration.ofMinutes(30);
            case "1h"  -> Duration.ofHours(1);
            case "4h"  -> Duration.ofHours(4);
            default -> throw new IllegalArgumentException("Unsupported interval: " + interval);
        };
    }

    /**
     * Load CSV data into a TA4J BarSeries
     *
     * @param days
     * @param timeFrame
     * @return BarSeries
     * @throws Exception if reading fails
     */
    public  BarSeries loadSeriesFromCsv(int days, String timeFrame) throws Exception {
        BarSeries series = new BaseBarSeries("BTC Series");

        Duration barDuration = mapIntervalToDuration(timeFrame);

        String outputPath = "D:\\stockMaket\\csvData\\btc_data_" + timeFrame + "_" + days+ ".csv";

        BufferedReader reader = new BufferedReader(new FileReader(outputPath));
        String line = reader.readLine(); // skip header line

        // Set formatter with IST timezone
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        while ((line = reader.readLine()) != null) {
            String[] fields = line.split(",");

            if (fields.length < 6){
                continue; // ensure minimum columns exist
            }

            long timestamp = Long.parseLong(fields[0].replaceAll("\"", ""));
            ZonedDateTime time = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.of("Asia/Kolkata"));

            BigDecimal open = new BigDecimal(fields[1].replaceAll("\"", "").trim());
            BigDecimal high = new BigDecimal(fields[2].replaceAll("\"", "").trim());
            BigDecimal low = new BigDecimal(fields[3].replaceAll("\"", "").trim());
            BigDecimal close = new BigDecimal(fields[4].replaceAll("\"", "").trim());
            BigDecimal volume = new BigDecimal(fields[5].replaceAll("\"", "").trim());

            Bar bar = new BaseBar(barDuration, time, open, high, low, close, volume);
            series.addBar(bar);
        }

        reader.close();
        return series;
    }
}