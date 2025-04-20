package com.trading.backtest.repository;

import com.trading.backtest.model.SimpleCrossOverTradeResult;
import com.trading.backtest.model.TradeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SimpleCrossOverTradeResultRepository extends JpaRepository<SimpleCrossOverTradeResult, Long> {
    TradeResult findByEntryTimeAndTimeFrameAndDuration(LocalDateTime entryTime, String timeFrame, Integer durationDays);
}
