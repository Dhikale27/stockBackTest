package com.trading.backtest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "trade_details")
public class TradeDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String strategyName;
    private LocalDateTime entryTime;
    private double entryPrice;
    private LocalDateTime exitTime;
    private double exitPrice;
    private double profitOrLoss;
    private String positionType; // LONG or SHORT
    private Long backtestResultId;
}