package com.trading.backtest.repository;

import com.trading.backtest.model.TradeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TradeResultRepository extends JpaRepository<TradeResult, Long> {
    TradeResult findByEntryTimeAndTimeFrameAndDuration(LocalDateTime entryTime, String timeFrame, Integer durationDays);
    TradeResult findByTimeFrameAndDurationAndStopLossPercentAndRiskRewardAndStrategyName(String timeFrame, Integer duration, double lossPercent, double riskReward, String strategyName);
}
