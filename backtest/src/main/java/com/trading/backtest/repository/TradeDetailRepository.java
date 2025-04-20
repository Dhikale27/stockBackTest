package com.trading.backtest.repository;

import com.trading.backtest.model.TradeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeDetailRepository extends JpaRepository<TradeDetail, Long> {
    List<TradeDetail> findByBacktestResultId(Long backtestResultId);
}