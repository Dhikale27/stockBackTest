package com.trading.backtest.model;

import lombok.Data;

@Data
public class TradeRequestBody {
    private double percentProfit;
    private double percentLoss;
    private String timeFrame;
    private int durationDays;
    private String strategyName;
    private boolean isTrailSL;
}
