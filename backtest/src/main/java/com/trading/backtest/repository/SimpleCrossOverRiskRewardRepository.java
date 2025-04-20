package com.trading.backtest.repository;

import com.trading.backtest.model.RiskReward;
import com.trading.backtest.model.SimpleCrossOverRiskReward;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleCrossOverRiskRewardRepository extends JpaRepository<SimpleCrossOverRiskReward, Long> {

    RiskReward findByDurationAndTimeFrame(Integer durationDays, String timeFrame);
}
