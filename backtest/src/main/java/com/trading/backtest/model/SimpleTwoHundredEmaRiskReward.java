package com.trading.backtest.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class SimpleTwoHundredEmaRiskReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private double stopLossPercent;
    private double targetPercent;
    private Integer totalProfitableTrade;
    private Integer totalLossTrade;
    private Integer totalTrade;
    private double totalProfit;
    private double rewardToRisk;
    private String timeFrame;
    private Integer duration;
}
