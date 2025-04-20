package com.trading.backtest.service;

import com.trading.backtest.model.*;
import com.trading.backtest.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class BacktestService {

    @Autowired
    BinanceClient binanceClient;
    @Autowired
    CsvToTimeSeriesConverter converter;
    @Autowired
    TradeResultRepository tradeRepo;

    @Autowired
    RiskRewadRepository riskRewadRepository;
    @Autowired
    EmaRiskRewadOneHourRepository emaRiskRewadOneHourRepository;
    @Autowired
    EmaCrossOverOneHourReposiotry emaCrossOverOneHourReposiotry;
    @Autowired
    SimpleCrossOverRiskRewardRepository simpleCrossOverRiskRewardRepository;
    @Autowired
    SimpleCrossOverTradeResultRepository simpleCrossOverTradeResultRepository;

    @Autowired
    SimpleTwoHundresRiskRewardRepository simpleTwoHundresRiskRewardRepository;

    @Autowired
    SimpleTwoHundresTradeResultRepository simpleTwoHundresTradeResultRepository;

    @Autowired
    MacdTradeResultRepository macdTradeResultRepository;

    @Autowired
    MacdRiskRewardRepository macdRiskRewardRepository;

    private int maxLossCount = 0;
    private int maxProfitCount = 0;
//    private String strategyName = null;
    private LocalDateTime maxLossDay = null;
    private LocalDateTime maxProfitDay = null;
    private LocalDateTime maxLossDayInRow = null;
    private LocalDateTime maxProfitDayInRow = null;
    private double tempMaxLoss = 0;
    private double tempMaxProf = 0;


    private Integer numberOfProfitableTrade = 0;
    private Integer numberOfLossTrade = 0;
    private double totalProfit = 0;




    public TradeResultResponse emaCrossOverStrategy(TradeRequestBody requestBody) throws IOException {
//        if(requestBody.getStrategyName() == null || !requestBody.getStrategyName().contains("ema")){
//            throw new InvalidParameterException("Strategy Name not valid");
//        }
//        String json = "";
//        try {
//            json = binanceClient.fetchFullData("BTCUSDT", requestBody.getTimeFrame(), 1000, requestBody.getDurationDays());
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        BarSeries series = converter.convertFromJson(json, requestBody.getTimeFrame());

        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(120, "1h");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema9 = new EMAIndicator(close, 9);
        EMAIndicator ema21 = new EMAIndicator(close, 21);

        boolean inPosition = false;
        boolean isBuy = false;
        int entryIndex = -1;
        double entryPrice = 0.0;

        tempMaxLoss = 100000000;
        tempMaxProf = -100000000;
        maxLossDay = null;
        maxProfitDay = null;
        maxLossDayInRow = null;
        maxProfitDayInRow = null;
        boolean maxLossFlag = false;
        boolean maxProfitFlag = false;
        maxLossCount = 0;
        maxProfitCount = 0;
        int tempMaxLossCount = 1;
        int tempMaxProfitCount = 1;

        for (int i = 1; i < series.getBarCount(); i++) {
            Num prevEma9 = ema9.getValue(i - 1);
            Num prevEma21 = ema21.getValue(i - 1);
            Num currEma9 = ema9.getValue(i);
            Num currEma21 = ema21.getValue(i);
            Num currentPrice = close.getValue(i);

            if (!inPosition) {
                // Check for buy crossover
                if (prevEma9.isLessThan(prevEma21) && currEma9.isGreaterThan(currEma21)) {
                    // Buy entry
                    inPosition = true;
                    isBuy = true;
                    entryIndex = i;
                    entryPrice = currentPrice.doubleValue();
                }
                // Check for sell crossover
                else if (prevEma9.isGreaterThan(prevEma21) && currEma9.isLessThan(currEma21)) {
                    // Sell entry
                    inPosition = true;
                    isBuy = false;
                    entryIndex = i;
                    entryPrice = currentPrice.doubleValue();
                }
            } else {
                double currentPriceVal = currentPrice.doubleValue();
                double priceChange = (currentPriceVal - entryPrice) / entryPrice;

                boolean exit = false;

                if (isBuy) {
                    if (priceChange <= -requestBody.getPercentLoss() || priceChange >= requestBody.getPercentProfit()) {
                        exit = true;
                    }
                } else {
                    if (priceChange >= requestBody.getPercentLoss() || priceChange <= -requestBody.getPercentProfit()) {
                        exit = true;
                    }
                }

                if (exit) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);
                    double profit = isBuy
                            ? currentPriceVal - entryPrice
                            : entryPrice - currentPriceVal;
//                    double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                    //save result is trade result table
                    saveTradeResult(entryBar, exitBar, entryPrice, currentPriceVal, isBuy, requestBody);
                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }

                    inPosition = false;
                    isBuy = false;
                    entryIndex = -1;
                }
            }
        }

        TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
        numberOfLossTrade = 0;
        numberOfProfitableTrade = 0;
        totalProfit = 0;
        return tradeResultResponse;
    }

    public void emaCrossOverTimeFrame(TradeRequestBody requestBody) {
        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(120, "1h");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema9 = new EMAIndicator(close, 9);
        EMAIndicator ema21 = new EMAIndicator(close, 21);

        double tempLossPercent = 0.01;
        while(tempLossPercent<=0.05){
            requestBody.setPercentLoss(tempLossPercent);
            int riskReward = 1;
            while(riskReward <= 20){
                requestBody.setPercentProfit(tempLossPercent*riskReward);

                boolean inPosition = false;
                boolean isBuy = false;
                int entryIndex = -1;
                double entryPrice = 0.0;

                tempMaxLoss = 100000000;
                tempMaxProf = -100000000;
                maxLossDay = null;
                maxProfitDay = null;
                maxLossDayInRow = null;
                maxProfitDayInRow = null;
                boolean maxLossFlag = false;
                boolean maxProfitFlag = false;
                maxLossCount = 0;
                maxProfitCount = 0;
                int tempMaxLossCount = 1;
                int tempMaxProfitCount = 1;

                for (int i = 1; i < series.getBarCount(); i++) {
                    Num prevEma9 = ema9.getValue(i - 1);
                    Num prevEma21 = ema21.getValue(i - 1);
                    Num currEma9 = ema9.getValue(i);
                    Num currEma21 = ema21.getValue(i);
                    Num currentPrice = close.getValue(i);

                    if (!inPosition) {
                        // Check for buy crossover
                        if (prevEma9.isLessThan(prevEma21) && currEma9.isGreaterThan(currEma21)) {
                            // Buy entry
                            inPosition = true;
                            isBuy = true;
                            entryIndex = i;
                            entryPrice = currentPrice.doubleValue();
                        }
                        // Check for sell crossover
                        else if (prevEma9.isGreaterThan(prevEma21) && currEma9.isLessThan(currEma21)) {
                            // Sell entry
                            inPosition = true;
                            isBuy = false;
                            entryIndex = i;
                            entryPrice = currentPrice.doubleValue();
                        }
                    } else {
                        double currentPriceVal = currentPrice.doubleValue();
                        double priceChange = (currentPriceVal - entryPrice) / entryPrice;

                        boolean exit = false;

                        if (isBuy) {
                            if (priceChange <= -requestBody.getPercentLoss() || priceChange >= requestBody.getPercentProfit()) {
                                exit = true;
                            }
                        } else {
                            if (priceChange >= requestBody.getPercentLoss() || priceChange <= -requestBody.getPercentProfit()) {
                                exit = true;
                            }
                        }

                        if (exit) {
                            Bar entryBar = series.getBar(entryIndex);
                            Bar exitBar = series.getBar(i);
                            double profit = isBuy
                                    ? currentPriceVal - entryPrice
                                    : entryPrice - currentPriceVal;
//                    double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                            //save result is trade result table
                            saveTradeResult(entryBar, exitBar, entryPrice, currentPriceVal, isBuy, requestBody);
                            if (profit < 0) {
                                numberOfLossTrade += 1;
                            } else {
                                numberOfProfitableTrade += 1;
                            }
                            totalProfit = totalProfit + profit;

                            if (totalProfit < tempMaxLoss) {
                                tempMaxLoss = totalProfit;
                                maxLossDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (totalProfit > tempMaxProf) {
                                tempMaxProf = totalProfit;
                                maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (profit < 0) {
                                tempMaxProfitCount = 1;
                                maxProfitFlag = false;
                                if (maxLossFlag) {
                                    tempMaxLossCount += 1;
                                }
                                maxLossFlag = true;
                            } else {
                                tempMaxLossCount = 1;
                                maxLossFlag = false;
                                if (maxProfitFlag) {
                                    tempMaxProfitCount += 1;
                                }
                                maxProfitFlag = true;
                            }
                            if (tempMaxLossCount > maxLossCount) {
                                maxLossCount = tempMaxLossCount;
                                maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (tempMaxProfitCount > maxProfitCount) {
                                maxProfitCount = tempMaxProfitCount;
                                maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }

                            inPosition = false;
                            isBuy = false;
                            entryIndex = -1;
                        }
                    }
                }

                TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
                numberOfLossTrade = 0;
                numberOfProfitableTrade = 0;
                totalProfit = 0;

                riskReward+=1;

            }
            tempLossPercent = tempLossPercent + 0.01;
        }

    }

    public TradeResultResponse emaCrossOverStrategy2(String timeFrame, int durationDays) throws IOException {
        String json = "";
        try {
            json = binanceClient.fetchFullData("BTCUSDT", timeFrame, 1000, durationDays);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        BarSeries series = converter.convertFromJson(json, timeFrame);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema9 = new EMAIndicator(close, 9);
        EMAIndicator ema21 = new EMAIndicator(close, 21);

        boolean inTrade = false;
        boolean isBuy = false;
        int entryIndex = -1;
        double entryPrice = 0;

        double tempMaxLoss = 100000000;
        double tempMaxProf = -100000000;
        LocalDateTime maxLossDay = null;
        LocalDateTime maxProfitDay = null;
        LocalDateTime maxLossDayInRow = null;
        LocalDateTime maxProfitDayInRow = null;
        boolean maxLossFlag = false;
        boolean maxProfitFlag = false;
        int maxLossCount = 0;
        int maxProfitCount = 0;
        int tempMaxLossCount = 1;
        int tempMaxProfitCount = 1;

        for (int i = 1; i < series.getBarCount(); i++) {
            Num ema9Prev = ema9.getValue(i - 1);
            Num ema21Prev = ema21.getValue(i - 1);
            Num ema9Curr = ema9.getValue(i);
            Num ema21Curr = ema21.getValue(i);
            Num currentPrice = close.getValue(i);

            boolean crossoverBuy = ema9Prev.isLessThan(ema21Prev) && ema9Curr.isGreaterThan(ema21Curr);
            boolean crossoverSell = ema9Prev.isGreaterThan(ema21Prev) && ema9Curr.isLessThan(ema21Curr);

            // Enter trade
            if (!inTrade && crossoverBuy) {
                inTrade = true;
                isBuy = true;
                entryIndex = i;
                entryPrice = currentPrice.doubleValue();
            } else if (!inTrade && crossoverSell) {
                inTrade = true;
                isBuy = false;
                entryIndex = i;
                entryPrice = currentPrice.doubleValue();
            }

            // Reverse trade (exit + new entry)
            else if (inTrade) {
                if (isBuy && crossoverSell) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);
                    double profit = entryPrice - currentPrice.doubleValue();
                    SimpleCrossOverTradeResult result = new SimpleCrossOverTradeResult();
                    result.setEntryTime(entryBar.getEndTime().toLocalDateTime());
                    result.setExitTime(exitBar.getEndTime().toLocalDateTime());
                    result.setEntryPrice(entryPrice);
                    result.setExitPrice(currentPrice.doubleValue());
                    result.setProfitOrLoss(entryPrice - currentPrice.doubleValue());
                    result.setDuration(durationDays);
                    result.setTimeFrame(timeFrame);
                    result.setPositionType("BUY");

                    simpleCrossOverTradeResultRepository.save(result);

                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }

                    // Enter new sell
                    inTrade = true;
                    isBuy = false;
                    entryIndex = i;
                    entryPrice = currentPrice.doubleValue();
                } else if (!isBuy && crossoverBuy) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);

                    SimpleCrossOverTradeResult result = new SimpleCrossOverTradeResult();
                    result.setEntryTime(entryBar.getEndTime().toLocalDateTime());
                    result.setExitTime(exitBar.getEndTime().toLocalDateTime());
                    result.setEntryPrice(entryPrice);
                    result.setExitPrice(currentPrice.doubleValue());
                    result.setProfitOrLoss(currentPrice.doubleValue() - entryPrice);
                    result.setPositionType("SELL");
                    result.setDuration(durationDays);
                    result.setTimeFrame(timeFrame);
                    simpleCrossOverTradeResultRepository.save(result);

                    double profit = entryPrice - currentPrice.doubleValue();
                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }


                    // Enter new buy
                    inTrade = true;
                    isBuy = true;
                    entryIndex = i;
                    entryPrice = currentPrice.doubleValue();
                }
            }
        }


        SimpleCrossOverRiskReward riskReward = new SimpleCrossOverRiskReward();
        riskReward.setTotalProfitableTrade(numberOfProfitableTrade);
        riskReward.setTotalLossTrade(numberOfLossTrade);
        riskReward.setTotalTrade(numberOfLossTrade + numberOfProfitableTrade);
        riskReward.setTotalProfit(totalProfit);
        riskReward.setTimeFrame(timeFrame);
        riskReward.setDuration(durationDays);
//        RiskReward byStopLossPercentAndRewardToRisk = simpleCrossOverRiskRewardRepository.findByDurationAndTimeFrame(durationDays, timeFrame);
//        if (null == byStopLossPercentAndRewardToRisk) {
//            simpleCrossOverRiskRewardRepository.save(riskReward);
//        }
        simpleCrossOverRiskRewardRepository.save(riskReward);
        TradeResultResponse tradeResultResponse = new TradeResultResponse();
        tradeResultResponse.setNumberOfLossTrade(numberOfLossTrade);
        tradeResultResponse.setNumberOfProfitableTrade(numberOfProfitableTrade);
        tradeResultResponse.setTotalNumberOfTrade(numberOfLossTrade + numberOfProfitableTrade);
        tradeResultResponse.setTotalProfit(totalProfit);
        tradeResultResponse.setMaxLoss(tempMaxLoss);
        tradeResultResponse.setMaxProfit(tempMaxProf);
        tradeResultResponse.setMaxLossDay(maxLossDay);
        tradeResultResponse.setMaxProfDay(maxProfitDay);
        tradeResultResponse.setMaxTradeLossInRow(maxLossCount);
        tradeResultResponse.setMaxLossDayInRow(maxLossDayInRow);
        tradeResultResponse.setMaxTradeProfitInRow(maxProfitCount);
        tradeResultResponse.setMaxProfitDayInRow(maxProfitDayInRow);
        numberOfLossTrade = 0;
        numberOfProfitableTrade = 0;
        totalProfit = 0;
        return tradeResultResponse;

    }

    public TradeResultResponse simpleTwoHundredEma(double percentProfit, double percentLoss, String timeFrame, int durationDays) {
        String json = "";
        try {
            json = binanceClient.fetchFullData("BTCUSDT", timeFrame, 1000, durationDays);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        BarSeries series = converter.convertFromJson(json, timeFrame);
        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema200 = new EMAIndicator(close, 200);

        boolean inTrade = false;
        boolean isBuy = false;
        boolean trailSLActive = false;
        double entryPrice = 0;
        int entryIndex = -1;

//##############################
        double tempMaxLoss = 100000000;
        double tempMaxProf = -100000000;
        LocalDateTime maxLossDay = null;
        LocalDateTime maxProfitDay = null;
        LocalDateTime maxLossDayInRow = null;
        LocalDateTime maxProfitDayInRow = null;
        boolean maxLossFlag = false;
        boolean maxProfitFlag = false;
        int maxLossCount = 0;
        int maxProfitCount = 0;
        int tempMaxLossCount = 1;
        int tempMaxProfitCount = 1;
//#################################
        percentProfit = percentProfit * 100;
        percentLoss = percentLoss * 100;
        for (int i = 1; i < series.getBarCount(); i++) {
            double prevClose = close.getValue(i - 1).doubleValue();
            double currClose = close.getValue(i).doubleValue();
            double prevEma = ema200.getValue(i - 1).doubleValue();
            double currEma = ema200.getValue(i).doubleValue();

            boolean crossAbove = prevClose < prevEma && currClose > currEma;
            boolean crossBelow = prevClose > prevEma && currClose < currEma;

            if (!inTrade) {
                if (crossAbove) {
                    // BUY ENTRY
                    inTrade = true;
                    isBuy = true;
                    entryPrice = currClose;
                    entryIndex = i;
                    trailSLActive = false;
                } else if (crossBelow) {
                    // SELL ENTRY
                    inTrade = true;
                    isBuy = false;
                    entryPrice = currClose;
                    entryIndex = i;
                    trailSLActive = false;
                }
            } else {
                // Already in trade, check for exit
                double currentPrice = currClose;

                double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                double profitPercent = (profit / entryPrice) * 100;

                double loss = isBuy ? (entryPrice - currentPrice) : (currentPrice - entryPrice);
                double lossPercent = (loss / entryPrice) * 100;

                boolean exit = false;

                // Enable trailing SL if profit >= 2%
                if (profitPercent >= 2 && !trailSLActive) {
                    trailSLActive = true;
                }

                // Exit on profit or loss hit
                if (profitPercent >= 6 || lossPercent >= 2) {
                    exit = true;
                }

                // Exit if trail SL active and price falls to entry price
                if (trailSLActive && (
                        (isBuy && currentPrice <= entryPrice) ||
                                (!isBuy && currentPrice >= entryPrice)
                )) {
                    exit = true;
                }

                if (exit) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);
                    SimpleTwoHundredEmaTradeResult result = new SimpleTwoHundredEmaTradeResult();
                    result.setEntryTime(entryBar.getEndTime().toLocalDateTime());
                    result.setExitTime(exitBar.getEndTime().toLocalDateTime());
                    result.setEntryPrice(entryPrice);
                    result.setExitPrice(currentPrice);
                    result.setProfitOrLoss(isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice));
                    result.setPositionType(isBuy ? "BUY" : "SELL");
                    result.setTimeFrame(timeFrame);
                    result.setDuration(durationDays);

                    simpleTwoHundresTradeResultRepository.save(result);
//                    simpleTwoHundresTradeResultRepository.fin

                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }


                    // Reset state
                    inTrade = false;
                    trailSLActive = false;
                    entryPrice = 0;
                    entryIndex = -1;
                }
            }
        }

        SimpleTwoHundredEmaRiskReward riskReward = new SimpleTwoHundredEmaRiskReward();
        riskReward.setTargetPercent(percentProfit * 100);
        riskReward.setStopLossPercent(percentLoss * 100);
        riskReward.setTotalProfitableTrade(numberOfProfitableTrade);
        riskReward.setTotalLossTrade(numberOfLossTrade);
        riskReward.setTotalTrade(numberOfLossTrade + numberOfProfitableTrade);
        riskReward.setTotalProfit(totalProfit);
        riskReward.setRewardToRisk(percentProfit / percentLoss);
        riskReward.setTimeFrame(timeFrame);
        riskReward.setDuration(durationDays);
        SimpleTwoHundredEmaRiskReward byStopLossPercentAndRewardToRisk = simpleTwoHundresRiskRewardRepository.findByStopLossPercentAndRewardToRiskAndDurationAndTimeFrame(percentLoss, percentProfit / percentLoss, durationDays, timeFrame);
        if (null == byStopLossPercentAndRewardToRisk) {
            simpleTwoHundresRiskRewardRepository.save(riskReward);
        }
        TradeResultResponse tradeResultResponse = new TradeResultResponse();
        tradeResultResponse.setNumberOfLossTrade(numberOfLossTrade);
        tradeResultResponse.setNumberOfProfitableTrade(numberOfProfitableTrade);
        tradeResultResponse.setTotalNumberOfTrade(numberOfLossTrade + numberOfProfitableTrade);
        tradeResultResponse.setTotalProfit(totalProfit);
        tradeResultResponse.setMaxLoss(tempMaxLoss);
        tradeResultResponse.setMaxProfit(tempMaxProf);
        tradeResultResponse.setMaxLossDay(maxLossDay);
        tradeResultResponse.setMaxProfDay(maxProfitDay);
        tradeResultResponse.setMaxTradeLossInRow(maxLossCount);
        tradeResultResponse.setMaxLossDayInRow(maxLossDayInRow);
        tradeResultResponse.setMaxTradeProfitInRow(maxProfitCount);
        tradeResultResponse.setMaxProfitDayInRow(maxProfitDayInRow);
        numberOfLossTrade = 0;
        numberOfProfitableTrade = 0;
        totalProfit = 0;
        return tradeResultResponse;
    }

    public TradeResultResponse backtestMacdStrategyTrailSL(TradeRequestBody requestBody) {
        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(requestBody.getDurationDays(), requestBody.getTimeFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        BarSeries series = converter.convertFromJson(json, requestBody.getTimeFrame());

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        EMAIndicator ema3 = new EMAIndicator(close, 3);
        EMAIndicator ema10 = new EMAIndicator(close, 10);
        DifferenceIndicator macdLine = new DifferenceIndicator(ema3, ema10);
        SMAIndicator signalLine = new SMAIndicator(macdLine, 16);

        boolean inTrade = false;
        boolean isBuy = false;
        int entryIndex = -1;
        double entryPrice = 0;
        double stopLoss = 0;

        tempMaxLoss = 100000000;
        tempMaxProf = -100000000;
        maxLossDay = null;
        maxProfitDay = null;
        maxLossDayInRow = null;
        maxProfitDayInRow = null;
        boolean maxLossFlag = false;
        boolean maxProfitFlag = false;
        maxLossCount = 0;
        maxProfitCount = 0;
        int tempMaxLossCount = 1;
        int tempMaxProfitCount = 1;

        for (int i = 1; i < series.getBarCount(); i++) {
            Num macdPrev = macdLine.getValue(i - 1);
            Num signalPrev = signalLine.getValue(i - 1);
            Num macdCurr = macdLine.getValue(i);
            Num signalCurr = signalLine.getValue(i);
            Num price = close.getValue(i);

            if (!inTrade) {
                // Buy Entry
                if (macdPrev.isLessThan(signalPrev) && macdCurr.isGreaterThan(signalCurr)) {
                    entryIndex = i;
                    entryPrice = price.doubleValue();
                    stopLoss = entryPrice * (1-requestBody.getPercentLoss()); // 3% loss SL
                    inTrade = true;
                    isBuy = true;
                }
                // Sell Entry
                else if (macdPrev.isGreaterThan(signalPrev) && macdCurr.isLessThan(signalCurr)) {
                    entryIndex = i;
                    entryPrice = price.doubleValue();
                    stopLoss = entryPrice * (1 + requestBody.getPercentLoss()); // 3% loss SL
                    inTrade = true;
                    isBuy = false;
                }
            } else {
                double currentPrice = price.doubleValue();
                double profitTarget = isBuy ? entryPrice * (1 + requestBody.getPercentProfit()) : entryPrice * (1 - requestBody.getPercentProfit());

                // Trailing Stop Loss logic
                if (requestBody.isTrailSL()) {
                    if (isBuy) {
                        double gainPercent = (currentPrice - entryPrice) / entryPrice;
                        if (gainPercent >= 0.01) {
                            int levels = (int) (gainPercent / 0.01);
//                        stopLoss = Math.max(stopLoss, entryPrice + levels * 0.01 * entryPrice);
//                        stopLoss = Math.max(stopLoss, entryPrice * (1-requestBody.getPercentLoss()) + levels * 0.01 * entryPrice);
                            stopLoss = Math.max(stopLoss, entryPrice * ((1 - requestBody.getPercentLoss()) + (levels * 0.01)));
                        }
                    } else {
                        double gainPercent = (entryPrice - currentPrice) / entryPrice;
                        if (gainPercent >= 0.01) {
                            int levels = (int) (gainPercent / 0.01);
//                        stopLoss = Math.min(stopLoss, entryPrice - levels * 0.01 * entryPrice);
                            stopLoss = Math.min(stopLoss, entryPrice * ((1 + requestBody.getPercentLoss()) - (levels * 0.01)));
                        }
                    }

                }

                boolean exit = false;
                if (isBuy) {
                    if (currentPrice <= stopLoss || currentPrice >= profitTarget) {
                        exit = true;
                    }
                } else {
                    if (currentPrice >= stopLoss || currentPrice <= profitTarget) {
                        exit = true;
                    }
                }

                if (exit) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);

                    double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                    //save result is trade result table
                    saveTradeResult(entryBar, exitBar, entryPrice, currentPrice, isBuy, requestBody);
                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }

                    inTrade = false;
                    entryPrice = 0;
                    entryIndex = -1;
                }
            }
        }
        //save result in risk reward
        TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
        numberOfLossTrade = 0;
        numberOfProfitableTrade = 0;
        totalProfit = 0;
        return tradeResultResponse;
    }

    public void macdBacktestTimeFrame(TradeRequestBody requestBody) {
        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(requestBody.getDurationDays(), requestBody.getTimeFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
//        EMAIndicator shortEma = new EMAIndicator(closePrice, 3);
//        EMAIndicator longEma = new EMAIndicator(closePrice, 10);
//        Indicator<Num> macdLine = new DifferenceIndicator(shortEma, longEma);
//        SMAIndicator signalLine = new SMAIndicator(macdLine, 16);

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 12);
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);
        Indicator<Num> macdLine = new DifferenceIndicator(shortEma, longEma);
        EMAIndicator signalLine = new EMAIndicator(macdLine, 9);

        double tempLossPercent = 0.01;
        while(tempLossPercent<=0.05){
            requestBody.setPercentLoss(tempLossPercent);
            int riskReward = 1;
            while(riskReward <= 20){
                requestBody.setPercentProfit(tempLossPercent*riskReward);

                boolean inTrade = false;
                boolean isBuy = false;
                int entryIndex = -1;
                double entryPrice = 0;


                tempMaxLoss = 100000000;
                tempMaxProf = -100000000;
                maxLossDay = null;
                maxProfitDay = null;
                maxLossDayInRow = null;
                maxProfitDayInRow = null;
                boolean maxLossFlag = false;
                boolean maxProfitFlag = false;
                maxLossCount = 0;
                maxProfitCount = 0;
                int tempMaxLossCount = 1;
                int tempMaxProfitCount = 1;
                double profitPercent = 1 + requestBody.getPercentProfit();
                double lossPercent = 1 - requestBody.getPercentLoss();
                for (int i = 1; i < series.getBarCount(); i++) {
                    Num macdPrev = macdLine.getValue(i - 1);
                    Num signalPrev = signalLine.getValue(i - 1);
                    Num macdCurr = macdLine.getValue(i);
                    Num signalCurr = signalLine.getValue(i);

                    Num price = closePrice.getValue(i);

                    if (!inTrade) {
                        // Buy entry: MACD Line crosses above Signal Line
                        if (macdPrev.isLessThan(signalPrev) && macdCurr.isGreaterThan(signalCurr)) {
                            inTrade = true;
                            isBuy = true;
                            entryIndex = i;
                            entryPrice = price.doubleValue();
                        }
                        // Sell entry: MACD Line crosses below Signal Line
                        else if (macdPrev.isGreaterThan(signalPrev) && macdCurr.isLessThan(signalCurr)) {
                            inTrade = true;
                            isBuy = false;
                            entryIndex = i;
                            entryPrice = price.doubleValue();
                        }
                    } else {
                        double currentPrice = price.doubleValue();
                        double thresholdUp = entryPrice * profitPercent;
                        double thresholdDown = entryPrice * lossPercent;
                        boolean exit = false;

                        if (isBuy) {
                            if (currentPrice <= thresholdDown || currentPrice >= thresholdUp) {
                                exit = true;
                            }
                        } else {
                            if (currentPrice >= entryPrice * (1 + requestBody.getPercentLoss()) || currentPrice <= entryPrice * (1-requestBody.getPercentProfit())) {
                                exit = true;
                            }
                        }
                        if (exit) {
                            Bar entryBar = series.getBar(entryIndex);
                            Bar exitBar = series.getBar(i);
                            double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                            //save result is trade result table
                            saveTradeResult(entryBar, exitBar, entryPrice, currentPrice, isBuy, requestBody);
                            if (profit < 0) {
                                numberOfLossTrade += 1;
                            } else {
                                numberOfProfitableTrade += 1;
                            }
                            totalProfit = totalProfit + profit;

                            if (totalProfit < tempMaxLoss) {
                                tempMaxLoss = totalProfit;
                                maxLossDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (totalProfit > tempMaxProf) {
                                tempMaxProf = totalProfit;
                                maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (profit < 0) {
                                tempMaxProfitCount = 1;
                                maxProfitFlag = false;
                                if (maxLossFlag) {
                                    tempMaxLossCount += 1;
                                }
                                maxLossFlag = true;
                            } else {
                                tempMaxLossCount = 1;
                                maxLossFlag = false;
                                if (maxProfitFlag) {
                                    tempMaxProfitCount += 1;
                                }
                                maxProfitFlag = true;
                            }
                            if (tempMaxLossCount > maxLossCount) {
                                maxLossCount = tempMaxLossCount;
                                maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (tempMaxProfitCount > maxProfitCount) {
                                maxProfitCount = tempMaxProfitCount;
                                maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }

                            inTrade = false;
                            entryIndex = -1;
                            entryPrice = 0;
                        }
                    }
                }
                //save result in risk reward
                TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
                numberOfLossTrade = 0;
                numberOfProfitableTrade = 0;
                totalProfit = 0;

                riskReward+=1;

            }
            tempLossPercent = tempLossPercent + 0.01;
        }

    }

    public void rsiBacktestingTimeFrame(TradeRequestBody requestBody) {

        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(requestBody.getDurationDays(), requestBody.getTimeFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        double tempLossPercent = 0.01;
        while(tempLossPercent<=0.05) {
            requestBody.setPercentLoss(tempLossPercent);
            int riskReward = 1;
            while(riskReward <= 20){
                requestBody.setPercentProfit(tempLossPercent*riskReward);
                boolean inTrade = false;
                boolean isBuy = false;
                int entryIndex = -1;
                double entryPrice = 0;

                tempMaxLoss = 100000000;
                tempMaxProf = -100000000;
                maxLossDay = null;
                maxProfitDay = null;
                maxLossDayInRow = null;
                maxProfitDayInRow = null;
                boolean maxLossFlag = false;
                boolean maxProfitFlag = false;
                maxLossCount = 0;
                maxProfitCount = 0;
                int tempMaxLossCount = 1;
                int tempMaxProfitCount = 1;
                double profitPercent = 1 + requestBody.getPercentProfit();
                double lossPercent = 1 - requestBody.getPercentLoss();

                for (int i = 1; i < series.getBarCount(); i++) {
//            BigDecimal currentClose = new BigDecimal(closePrice.getValue(i).toString());
                    Num price = closePrice.getValue(i);
                    double rsiPrev = rsi.getValue(i - 1).doubleValue();
                    double rsiCurr = rsi.getValue(i).doubleValue();

                    // Entry rules
                    if (!inTrade) {
                        if (rsiPrev < 30 && rsiCurr > 30) {
                            // BUY entry
                            inTrade = true;
                            isBuy = true;
                            entryIndex = i;
                            entryPrice = price.doubleValue();
                        } else if (rsiPrev > 70 && rsiCurr < 70) {
                            // SELL entry
                            inTrade = true;
                            isBuy = false;
                            entryIndex = i;
                            entryPrice = price.doubleValue();
                        }
                    } else {

                        double currentPrice = price.doubleValue();
                        double thresholdUp = entryPrice * profitPercent;
                        double thresholdDown = entryPrice * lossPercent;
                        boolean exit = false;
                        // Exit logic
                        if (isBuy) {
                            if (currentPrice <= thresholdDown || currentPrice >= thresholdUp) {
                                exit = true;
                            }
                        } else {
                            if (currentPrice >= entryPrice * (1 + requestBody.getPercentLoss()) || currentPrice <= entryPrice * (1-requestBody.getPercentProfit())) {
                                exit = true;
                            }
                        }
                        if (exit) {
                            Bar entryBar = series.getBar(entryIndex);
                            Bar exitBar = series.getBar(i);
                            double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                            //save result is trade result table
                            saveTradeResult(entryBar, exitBar, entryPrice, currentPrice, isBuy, requestBody);
                            if (profit < 0) {
                                numberOfLossTrade += 1;
                            } else {
                                numberOfProfitableTrade += 1;
                            }
                            totalProfit = totalProfit + profit;

                            if (totalProfit < tempMaxLoss) {
                                tempMaxLoss = totalProfit;
                                maxLossDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (totalProfit > tempMaxProf) {
                                tempMaxProf = totalProfit;
                                maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (profit < 0) {
                                tempMaxProfitCount = 1;
                                maxProfitFlag = false;
                                if (maxLossFlag) {
                                    tempMaxLossCount += 1;
                                }
                                maxLossFlag = true;
                            } else {
                                tempMaxLossCount = 1;
                                maxLossFlag = false;
                                if (maxProfitFlag) {
                                    tempMaxProfitCount += 1;
                                }
                                maxProfitFlag = true;
                            }
                            if (tempMaxLossCount > maxLossCount) {
                                maxLossCount = tempMaxLossCount;
                                maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }
                            if (tempMaxProfitCount > maxProfitCount) {
                                maxProfitCount = tempMaxProfitCount;
                                maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                            }

                            inTrade = false;
                            entryIndex = -1;
                            entryPrice = 0;
                        }
                    }
                }

                //save result in risk reward
                TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
                numberOfLossTrade = 0;
                numberOfProfitableTrade = 0;
                totalProfit = 0;
                riskReward+=1;
            }
            tempLossPercent = tempLossPercent + 0.01;
        }



    }


    public TradeResultResponse macdBacktesting(TradeRequestBody requestBody) {
//        String json = "";
//        try {
//            json = binanceClient.fetchFullData("BTCUSDT", requestBody.getTimeFrame(), 1000, requestBody.getDurationDays());
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

//        BarSeries series = converter.convertFromJson(json, requestBody.getTimeFrame());
        BarSeries series = null;
        try {
            series = converter.loadSeriesFromCsv(requestBody.getDurationDays(), requestBody.getTimeFrame());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 3);
        EMAIndicator longEma = new EMAIndicator(closePrice, 10);
        Indicator<Num> macdLine = new DifferenceIndicator(shortEma, longEma);
        SMAIndicator signalLine = new SMAIndicator(macdLine, 16);

        boolean inTrade = false;
        boolean isBuy = false;
        int entryIndex = -1;
        double entryPrice = 0;


        tempMaxLoss = 100000000;
        tempMaxProf = -100000000;
        maxLossDay = null;
        maxProfitDay = null;
        maxLossDayInRow = null;
        maxProfitDayInRow = null;
        boolean maxLossFlag = false;
        boolean maxProfitFlag = false;
        maxLossCount = 0;
        maxProfitCount = 0;
        int tempMaxLossCount = 1;
        int tempMaxProfitCount = 1;
        double profitPercent = 1 + requestBody.getPercentProfit();
        double lossPercent = 1 - requestBody.getPercentLoss();
        for (int i = 1; i < series.getBarCount(); i++) {
            Num macdPrev = macdLine.getValue(i - 1);
            Num signalPrev = signalLine.getValue(i - 1);
            Num macdCurr = macdLine.getValue(i);
            Num signalCurr = signalLine.getValue(i);

            Num price = closePrice.getValue(i);

            if (!inTrade) {
                // Buy entry: MACD Line crosses above Signal Line
                if (macdPrev.isLessThan(signalPrev) && macdCurr.isGreaterThan(signalCurr)) {
                    inTrade = true;
                    isBuy = true;
                    entryIndex = i;
                    entryPrice = price.doubleValue();
                }
                // Sell entry: MACD Line crosses below Signal Line
                else if (macdPrev.isGreaterThan(signalPrev) && macdCurr.isLessThan(signalCurr)) {
                    inTrade = true;
                    isBuy = false;
                    entryIndex = i;
                    entryPrice = price.doubleValue();
                }
            } else {
                double currentPrice = price.doubleValue();
                double thresholdUp = entryPrice * profitPercent;
                double thresholdDown = entryPrice * lossPercent;
                boolean exit = false;

                if (isBuy) {
                    if (currentPrice <= thresholdDown || currentPrice >= thresholdUp) {
                        exit = true;
                    }
                } else {
                    if (currentPrice >= entryPrice * (1 + requestBody.getPercentLoss()) || currentPrice <= entryPrice * (1-requestBody.getPercentProfit())) {
                        exit = true;
                    }
                }
                if (exit) {
                    Bar entryBar = series.getBar(entryIndex);
                    Bar exitBar = series.getBar(i);
                    double profit = isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice);
                    //save result is trade result table
                    saveTradeResult(entryBar, exitBar, entryPrice, currentPrice, isBuy, requestBody);
                    if (profit < 0) {
                        numberOfLossTrade += 1;
                    } else {
                        numberOfProfitableTrade += 1;
                    }
                    totalProfit = totalProfit + profit;

                    if (totalProfit < tempMaxLoss) {
                        tempMaxLoss = totalProfit;
                        maxLossDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (totalProfit > tempMaxProf) {
                        tempMaxProf = totalProfit;
                        maxProfitDay = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (profit < 0) {
                        tempMaxProfitCount = 1;
                        maxProfitFlag = false;
                        if (maxLossFlag) {
                            tempMaxLossCount += 1;
                        }
                        maxLossFlag = true;
                    } else {
                        tempMaxLossCount = 1;
                        maxLossFlag = false;
                        if (maxProfitFlag) {
                            tempMaxProfitCount += 1;
                        }
                        maxProfitFlag = true;
                    }
                    if (tempMaxLossCount > maxLossCount) {
                        maxLossCount = tempMaxLossCount;
                        maxLossDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }
                    if (tempMaxProfitCount > maxProfitCount) {
                        maxProfitCount = tempMaxProfitCount;
                        maxProfitDayInRow = exitBar.getEndTime().toLocalDateTime();
                    }

                    inTrade = false;
                    entryIndex = -1;
                    entryPrice = 0;
                }
            }
        }
        //save result in risk reward
        TradeResultResponse tradeResultResponse = saveRiskRiward(requestBody);
        numberOfLossTrade = 0;
        numberOfProfitableTrade = 0;
        totalProfit = 0;
        return tradeResultResponse;
    }

    private void saveTradeResult(Bar entryBar, Bar exitBar, double entryPrice, double currentPrice, boolean isBuy, TradeRequestBody requestBody) {
        TradeResult oldRecord = tradeRepo.findByTimeFrameAndDurationAndStopLossPercentAndRiskRewardAndStrategyName(requestBody.getTimeFrame(), requestBody.getDurationDays(), requestBody.getPercentLoss(), requestBody.getPercentProfit() / requestBody.getPercentLoss(), requestBody.getStrategyName());
        if (null == oldRecord) {
            TradeResult result = new TradeResult();
            result.setEntryTime(entryBar.getEndTime().toLocalDateTime());
            result.setExitTime(exitBar.getEndTime().toLocalDateTime());
            result.setEntryPrice(entryPrice);
            result.setExitPrice(currentPrice);
            result.setProfitOrLoss(isBuy ? (currentPrice - entryPrice) : (entryPrice - currentPrice));
            result.setPositionType(isBuy ? "BUY" : "SELL");
            result.setTimeFrame(requestBody.getTimeFrame());
            result.setDuration(requestBody.getDurationDays());
            result.setStrategyName(requestBody.getStrategyName());
            result.setStopLossPercent(requestBody.getPercentLoss()*100);
            result.setRiskReward(requestBody.getPercentProfit()/requestBody.getPercentLoss());
            tradeRepo.save(result);
        }
    }

    private TradeResultResponse saveRiskRiward(TradeRequestBody requestBody) {
        RiskReward byStopLossPercentAndRewardToRisk = riskRewadRepository.findByStopLossPercentAndRewardToRiskAndDurationAndTimeFrameAndStrategyName(requestBody.getPercentLoss(), requestBody.getPercentProfit() / requestBody.getPercentLoss(), requestBody.getDurationDays(), requestBody.getTimeFrame(), requestBody.getStrategyName());
        if (null == byStopLossPercentAndRewardToRisk) {
            RiskReward riskReward = new RiskReward();
            riskReward.setTargetPercent(requestBody.getPercentProfit() * 100);
            riskReward.setStopLossPercent(requestBody.getPercentLoss() * 100);
            riskReward.setTotalProfitableTrade(numberOfProfitableTrade);
            riskReward.setTotalLossTrade(numberOfLossTrade);
            riskReward.setTotalTrade(numberOfLossTrade + numberOfProfitableTrade);
            riskReward.setTotalProfit(totalProfit);
            riskReward.setRewardToRisk(requestBody.getPercentProfit() / requestBody.getPercentLoss());
            riskReward.setTimeFrame(requestBody.getTimeFrame());
            riskReward.setDuration(requestBody.getDurationDays());
            riskReward.setMaxLossDay(maxLossDay);
            riskReward.setMaxProfitDay(maxProfitDay);
            riskReward.setMaxLoss(tempMaxLoss);
            riskReward.setMaxProfit(tempMaxProf);
            riskReward.setMaxLossInRowDay(maxLossDayInRow);
            riskReward.setMaxProfitInRowDay(maxProfitDayInRow);
            riskReward.setMaxLossInRowCount(maxLossCount);
            riskReward.setMaxProfitInRowCount(maxProfitCount);
            riskReward.setStrategyName(requestBody.getStrategyName());
            riskRewadRepository.save(riskReward);
        }
        TradeResultResponse tradeResultResponse = new TradeResultResponse();
        tradeResultResponse.setNumberOfLossTrade(numberOfLossTrade);
        tradeResultResponse.setNumberOfProfitableTrade(numberOfProfitableTrade);
        tradeResultResponse.setTotalNumberOfTrade(numberOfLossTrade + numberOfProfitableTrade);
        tradeResultResponse.setTotalProfit(totalProfit);
        tradeResultResponse.setMaxLoss(tempMaxLoss);
        tradeResultResponse.setMaxProfit(tempMaxProf);
        tradeResultResponse.setMaxLossDay(maxLossDay);
        tradeResultResponse.setMaxProfDay(maxProfitDay);
        tradeResultResponse.setMaxTradeLossInRow(maxLossCount);
        tradeResultResponse.setMaxLossDayInRow(maxLossDayInRow);
        tradeResultResponse.setMaxTradeProfitInRow(maxProfitCount);
        tradeResultResponse.setMaxProfitDayInRow(maxProfitDayInRow);
        return tradeResultResponse;
    }






}