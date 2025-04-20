package com.trading.backtest.repository;

import com.trading.backtest.model.MacdTradeResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MacdTradeResultRepository extends JpaRepository<MacdTradeResult, Long> {
    MacdTradeResult findByTimeFrameAndDurationAndStopLossPercentAndRiskReward(String timeFrame, Integer duration, double lossPercent, double riskReward);
}
