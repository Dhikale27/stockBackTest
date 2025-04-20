package com.trading.backtest.repository;

import com.trading.backtest.model.SimpleTwoHundredEmaTradeResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleTwoHundresTradeResultRepository extends JpaRepository<SimpleTwoHundredEmaTradeResult, Long> {

    SimpleTwoHundredEmaTradeResult findByDurationAndTimeFrame(Integer durationDays, String timeFrame);
}
