package com.trading.backtest.repository;

import com.trading.backtest.model.MacdRiskReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MacdRiskRewardRepository extends JpaRepository<MacdRiskReward, Long> {
    MacdRiskReward findByStopLossPercentAndRewardToRiskAndDurationAndTimeFrame(double stopLoss, double riskToReward, Integer durationDays, String timeFrame);
}
