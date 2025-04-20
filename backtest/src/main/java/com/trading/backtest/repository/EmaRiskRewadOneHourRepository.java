package com.trading.backtest.repository;

import com.trading.backtest.model.EmaRiskRewardOneHour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmaRiskRewadOneHourRepository extends JpaRepository<EmaRiskRewardOneHour, Long> {

    EmaRiskRewardOneHour findByStopLossPercentAndRewardToRisk(double lossPercent, double rewardToRisk);
}
