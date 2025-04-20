package com.trading.backtest.repository;

import com.trading.backtest.model.BacktestResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BacktestResultRepository extends JpaRepository<BacktestResult, Long> {
}