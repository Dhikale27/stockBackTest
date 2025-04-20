package com.trading.backtest.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class MacdRiskReward {
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
    private LocalDateTime maxLossDay;
    private LocalDateTime maxProfitDay;
    private LocalDateTime maxLossInRowDay;
    private LocalDateTime maxProfitInRowDay;
    private double maxLoss;
    private double maxProfit;
    private Integer maxLossInRowCount;
    private Integer maxProfitInRowCount;

}
