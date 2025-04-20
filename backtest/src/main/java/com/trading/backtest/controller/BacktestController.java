package com.trading.backtest.controller;

import com.trading.backtest.model.TradeRequestBody;
import com.trading.backtest.model.TradeResultResponse;
import com.trading.backtest.service.BacktestService;
import com.trading.backtest.service.BinanceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/backtest")
public class BacktestController {

    @Autowired
    BacktestService service;

    @Autowired
    BinanceClient binanceClient;

//    @PostMapping
//    public ResponseEntity<TradeResultResponse> run(@RequestParam(name = "profit") int profitPercent,
//                                                   @RequestParam(name = "loss") int lossPercent,
//                                                   @RequestParam(name = "isSave") boolean isSave) {
//        try {
//            TradeResultResponse tradeResultResponse = service.run2(profitPercent, lossPercent, isSave);
//            return ResponseEntity.ok(tradeResultResponse);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(new TradeResultResponse());
//        }
//    }

    @PostMapping("/macd")
    public ResponseEntity<TradeResultResponse> macdBacktesting(@RequestBody TradeRequestBody requestBody) {
        try {
            TradeResultResponse tradeResultResponse = service.macdBacktesting(requestBody);
            return ResponseEntity.ok(tradeResultResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new TradeResultResponse());
        }
    }

    @PostMapping("/macd/timeFrame")
    public ResponseEntity<String> macdBacktestingTimeFrame(@RequestBody TradeRequestBody requestBody) {
        try {
            service.macdBacktestTimeFrame(requestBody);
            return ResponseEntity.ok("Backtesting Complete");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failure .......!");
        }
    }

    @PostMapping("/macd/trailSL")
    public ResponseEntity<TradeResultResponse> macdBacktestingTrailSL(@RequestBody TradeRequestBody requestBody) {
        try {
            TradeResultResponse tradeResultResponse = service.backtestMacdStrategyTrailSL(requestBody);
//            TradeResultResponse tradeResultResponse = service.macdBacktesting(requestBody);
            return ResponseEntity.ok(tradeResultResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new TradeResultResponse());
        }
    }

    @GetMapping("/ema/twoHundred")
    public ResponseEntity<TradeResultResponse> simpleTwoHundredEma(@RequestParam(name = "profit") double profitPercent,
                                                                    @RequestParam(name = "loss") double lossPercent,
                                                                    @RequestParam(name = "timeFrame", required = true) String timeFrame,
                                                                    @RequestParam(name = "days") int days) {
        try {
            TradeResultResponse tradeResultResponse = service.simpleTwoHundredEma(profitPercent, lossPercent, timeFrame, days);
            return ResponseEntity.ok(tradeResultResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new TradeResultResponse());
        }
    }

    @PostMapping("/ema/crossOver")
    public ResponseEntity<TradeResultResponse> emaCrossOverStrategy(@RequestBody TradeRequestBody requestBody) {
        try {
            TradeResultResponse tradeResultResponse = service.emaCrossOverStrategy(requestBody);
            return ResponseEntity.ok(tradeResultResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new TradeResultResponse());
        }
    }

    @PostMapping("/ema/crossOver/timeFrame")
    public ResponseEntity<String> emaCrossOverStrategyTimeFrame(@RequestBody TradeRequestBody requestBody) {
        try {
            service.emaCrossOverTimeFrame(requestBody);
            return ResponseEntity.ok("Backtesting comeplete .....");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failure ....!");
        }
    }

    @PostMapping("/rsi/timeFrame")
    public ResponseEntity<String> rsiBacktestingTimeFrame(@RequestBody TradeRequestBody requestBody) {
        try {
            service.rsiBacktestingTimeFrame(requestBody);
            return ResponseEntity.ok("Backtesting comeplete .....");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failure ....!");
        }
    }

    @GetMapping("/setAll")
    public ResponseEntity<TradeResultResponse> emaCrossOverStrategy2(@RequestParam(name = "timeFrame", required = true) String timeFrame,
                                                           @RequestParam(name = "days") int days) {
        try {
            TradeResultResponse tradeResultResponse = service.emaCrossOverStrategy2(timeFrame, days);
            return ResponseEntity.ok(tradeResultResponse);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new TradeResultResponse());
        }
    }

    @GetMapping("/fetch/csvData")
    public ResponseEntity<String> fatchCsvData(@RequestParam(name = "timeFrame", required = true) String timeFrame,
                                                                     @RequestParam(name = "days") int days) {
        try {
            binanceClient.fetchHistoricalData("BTCUSDT", timeFrame, days);
            return ResponseEntity.ok("Data fetch successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failure .....!");
        }
    }

//    public void fetchHistoricalData(String symbol, String interval, int days, String outputPath
}