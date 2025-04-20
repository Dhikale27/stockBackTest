package com.trading.backtest.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TradeResultResponse {
    private double totalProfit;
    private Integer numberOfProfitableTrade;
    private Integer numberOfLossTrade;
    private Integer totalNumberOfTrade;
    private LocalDateTime maxLossDay;
    private LocalDateTime maxProfDay;
    private double maxLoss;
    private double maxProfit;
    private LocalDateTime maxLossDayInRow;
    private LocalDateTime maxProfitDayInRow;
    private double maxTradeLossInRow;
    private double maxTradeProfitInRow;

}
