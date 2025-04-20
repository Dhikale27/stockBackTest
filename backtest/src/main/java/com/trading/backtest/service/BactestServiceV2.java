//package com.trading.backtest.service;
//
//import com.trading.backtest.model.BacktestResult;
//import com.trading.backtest.model.TradeDetail;
//import com.trading.backtest.repository.BacktestResultRepository;
//import com.trading.backtest.repository.TradeDetailRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.ta4j.core.*;
//import org.ta4j.core.indicators.EMAIndicator;
//import org.ta4j.core.indicators.MACDIndicator;
//import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
//import org.ta4j.core.num.Num;
//import org.ta4j.core.rules.CrossedDownIndicatorRule;
//import org.ta4j.core.rules.CrossedUpIndicatorRule;
//import org.ta4j.core.rules.StopLossRule;
//
//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Service
//public class BactestServiceV2 {
//
//    @Autowired
//    private TradeDetailRepository tradeDetailRepository;
//    @Autowired
//    private BacktestResultRepository backtestResultRepository;
//
//    public BacktestResult runMacdStrategy(BarSeries series) {
//        Strategy strategy = buildMACDStrategy(series);
//        BarSeriesManager seriesManager = new BarSeriesManager(series);
//        TradingRecord tradingRecord = seriesManager.run(strategy);
//        return saveResultsToDatabase(tradingRecord, "MACD(2,3,16) Strategy");
//    }
//
////    private Strategy buildMACDStrategy(BarSeries series) {
////        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
////
////        // MACD parameters
////        MACDIndicator macd = new MACDIndicator(closePrice, 2, 3);
////        EMAIndicator macdSignal = new EMAIndicator(macd, 16);
////
////        // Entry rules
////        Rule entryBuyRule = new CrossedUpIndicatorRule(macd, macdSignal);
////        Rule entrySellRule = new CrossedDownIndicatorRule(macd, macdSignal);
////
////        // Exit rules
////        Num stopLossPercent = series.numOf(0.03);
////        Num takeProfitPercent = series.numOf(0.09);
////        Num trailingTriggerPercent = series.numOf(0.03);
////
////        // Long exit rules
////        Rule exitLongStopLoss = new StopLossRule(closePrice, stopLossPercent);
////        Rule exitLongTakeProfit = new StopGainRule(closePrice, takeProfitPercent);
////        Rule exitLongTrailingStop = new TrailingStopLossRule(closePrice, trailingTriggerPercent);
////        Rule exitLongRule = exitLongStopLoss.or(exitLongTakeProfit).or(exitLongTrailingStop);
////
////        // New code for short positions:
////        Rule exitShortStopLoss = new StopGainRule(closePrice, stopLossPercent); // For shorts, a stop loss is actually a price increase
////        Rule exitShortTakeProfit = new StopLossRule(closePrice, takeProfitPercent); // For shorts, take profit is a price decrease
////        Rule exitShortTrailingStop = new TrailingStopLossRule(closePrice, trailingTriggerPercent); // Works the same for shorts
////
////        // The rest of your strategy building remains the same:
////        Rule exitShortRule = exitShortStopLoss.or(exitShortTakeProfit).or(exitShortTrailingStop);
////
////        // Create strategy (corrected for TA4J 0.17)
////        return new BaseStrategy(
////                "MACD(2,3,16) Strategy",
////                entryBuyRule,
////                entrySellRule,
////                exitLongRule,
////                exitShortRule,
////                2  // Minimum number of bars for trade
////        );
////    }
//
//    private Strategy buildMACDStrategy(BarSeries series) {
//        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
//
//        // MACD parameters
//        MACDIndicator macd = new MACDIndicator(closePrice, 2, 3);
//        EMAIndicator macdSignal = new EMAIndicator(macd, 16);
//
//        // Entry rules
//        Rule entryBuyRule = new CrossedUpIndicatorRule(macd, macdSignal);
//        Rule entrySellRule = new CrossedDownIndicatorRule(macd, macdSignal);
//
//        // Exit rules
//        Num stopLossPercent = series.numOf(0.03);
//        Num takeProfitPercent = series.numOf(0.09);
//        Num trailingTriggerPercent = series.numOf(0.03);
//
//        // Long exit rules
//        Rule exitLongStopLoss = new StopLossRule(closePrice, stopLossPercent);
//        Rule exitLongTakeProfit = new StopGainRule(closePrice, takeProfitPercent);
//        Rule exitLongTrailingStop = new TrailingStopLossRule(closePrice, trailingTriggerPercent);
//        Rule exitLongRule = exitLongStopLoss.or(exitLongTakeProfit).or(exitLongTrailingStop);
//
//        // Short exit rules
//        Rule exitShortStopLoss = new StopGainRule(closePrice, stopLossPercent);
//        Rule exitShortTakeProfit = new StopLossRule(closePrice, takeProfitPercent);
//        Rule exitShortTrailingStop = new TrailingStopLossRule(closePrice, trailingTriggerPercent);
//        Rule exitShortRule = exitShortStopLoss.or(exitShortTakeProfit).or(exitShortTrailingStop);
//
//        // Create separate strategies for long and short
//        Strategy longStrategy = new BaseStrategy(
//                "MACD(2,3,16) Long Strategy",
//                entryBuyRule,
//                exitLongRule,
//                2  // Minimum number of bars
//        );
//
//        Strategy shortStrategy = new BaseStrategy(
//                "MACD(2,3,16) Short Strategy",
//                entrySellRule,
//                exitShortRule,
//                2  // Minimum number of bars
//        );
//
//        // Combine both strategies
//        return longStrategy.or(shortStrategy);
//    }
//
//    private BacktestResult saveResultsToDatabase(TradingRecord tradingRecord, String strategyName) {
//        BacktestResult result = new BacktestResult();
//        result.setStrategyName(strategyName);
//        result.setCreatedAt(LocalDateTime.now());
//        result.setTotalProfit(new TotalProfitCriterion().calculate(tradingRecord.getSeries(), tradingRecord).doubleValue());
//        result.setNumberOfProfitableTrade(new NumberOfProfitableTradesCriterion().calculate(tradingRecord.getSeries(), tradingRecord));
//        result.setNumberOfLossTrade(new NumberOfLosingTradesCriterion().calculate(tradingRecord.getSeries(), tradingRecord));
//        result.setTotalNumberOfTrade(tradingRecord.getTradeCount());
//
//        result = backtestResultRepository.save(result);
//        saveTradeDetails(tradingRecord, strategyName, result.getId());
//        calculateExtremeValues(result, tradingRecord);
//
//        return backtestResultRepository.save(result);
//    }
//
//    private void saveTradeDetails(TradingRecord tradingRecord, String strategyName, Long resultId) {
//        List<TradeDetail> details = new ArrayList<>();
//        for (Trade trade : tradingRecord.getTrades()) {
//            TradeDetail detail = new TradeDetail();
//            detail.setStrategyName(strategyName);
//            detail.setEntryTime(trade.getEntry().getTime().toLocalDateTime());
//            detail.setEntryPrice(trade.getEntry().getNetPrice().doubleValue());
//            detail.setExitTime(trade.getExit().getTime().toLocalDateTime());
//            detail.setExitPrice(trade.getExit().getNetPrice().doubleValue());
//            detail.setProfitOrLoss(trade.getProfit().doubleValue());
//            detail.setPositionType(trade.getEntry().isBuy() ? "LONG" : "SHORT");
//            detail.setBacktestResultId(resultId);
//            details.add(detail);
//        }
//        tradeDetailRepository.saveAll(details);
//    }
//
//    private void calculateExtremeValues(BacktestResult result, TradingRecord tradingRecord) {
//        Trade maxProfitTrade = null;
//        Trade maxLossTrade = null;
//        double maxProfit = Double.MIN_VALUE;
//        double maxLoss = Double.MAX_VALUE;
//
//        for (Trade trade : tradingRecord.getTrades()) {
//            double profit = trade.getProfit().doubleValue();
//            if (profit > maxProfit) {
//                maxProfit = profit;
//                maxProfitTrade = trade;
//            }
//            if (profit < maxLoss) {
//                maxLoss = profit;
//                maxLossTrade = trade;
//            }
//        }
//
//        if (maxProfitTrade != null) {
//            result.setMaxProfit(maxProfit);
//            result.setMaxProfitDay(maxProfitTrade.getExit().getTime().toLocalDateTime());
//        }
//        if (maxLossTrade != null) {
//            result.setMaxLoss(maxLoss);
//            result.setMaxLossDay(maxLossTrade.getExit().getTime().toLocalDateTime());
//        }
//
//        calculateConsecutiveStreaks(result, tradingRecord);
//    }
//
//    private void calculateConsecutiveStreaks(BacktestResult result, TradingRecord tradingRecord) {
//        int currentWinStreak = 0;
//        int maxWinStreak = 0;
//        ZonedDateTime maxWinStreakStart = null;
//
//        int currentLossStreak = 0;
//        int maxLossStreak = 0;
//        ZonedDateTime maxLossStreakStart = null;
//
//        for (Trade trade : tradingRecord.getTrades()) {
//            if (trade.getProfit().isPositive()) {
//                currentWinStreak++;
//                if (currentWinStreak > maxWinStreak) {
//                    maxWinStreak = currentWinStreak;
//                    maxWinStreakStart = trade.getEntry().getTime();
//                }
//                currentLossStreak = 0;
//            } else {
//                currentLossStreak++;
//                if (currentLossStreak > maxLossStreak) {
//                    maxLossStreak = currentLossStreak;
//                    maxLossStreakStart = trade.getEntry().getTime();
//                }
//                currentWinStreak = 0;
//            }
//        }
//
//        result.setMaxTradeProfitInRow(maxWinStreak);
//        result.setMaxProfitDayInRow(maxWinStreakStart != null ? maxWinStreakStart.toLocalDateTime() : null);
//        result.setMaxTradeLossInRow(maxLossStreak);
//        result.setMaxLossDayInRow(maxLossStreakStart != null ? maxLossStreakStart.toLocalDateTime() : null);
//    }
//}
//
//}
