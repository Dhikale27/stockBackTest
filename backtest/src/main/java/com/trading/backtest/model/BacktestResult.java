package com.trading.backtest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "backtest_results")
public class BacktestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String strategyName;
    private LocalDateTime createdAt;
    private double totalProfit;
    private int numberOfProfitableTrade;
    private int numberOfLossTrade;
    private int totalNumberOfTrade;
    private double maxProfit;
    private LocalDateTime maxProfitDay;
    private double maxLoss;
    private LocalDateTime maxLossDay;
    private int maxTradeProfitInRow;
    private LocalDateTime maxProfitDayInRow;
    private int maxTradeLossInRow;
    private LocalDateTime maxLossDayInRow;
}