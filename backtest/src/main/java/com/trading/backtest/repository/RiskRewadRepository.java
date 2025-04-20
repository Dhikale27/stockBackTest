package com.trading.backtest.repository;

import com.trading.backtest.model.RiskReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRewadRepository extends JpaRepository<RiskReward, Long> {

    RiskReward findByStopLossPercentAndRewardToRiskAndDurationAndTimeFrameAndStrategyName(double lossPercent, double rewadToRisk, Integer durationDays, String timeFrame, String strategyName);
}
