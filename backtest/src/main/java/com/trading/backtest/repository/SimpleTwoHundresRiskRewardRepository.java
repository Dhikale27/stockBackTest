package com.trading.backtest.repository;

import com.trading.backtest.model.SimpleTwoHundredEmaRiskReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleTwoHundresRiskRewardRepository extends JpaRepository<SimpleTwoHundredEmaRiskReward, Long> {

    SimpleTwoHundredEmaRiskReward findByStopLossPercentAndRewardToRiskAndDurationAndTimeFrame(double stopLoss, double riskToReward, Integer durationDays, String timeFrame);
}
