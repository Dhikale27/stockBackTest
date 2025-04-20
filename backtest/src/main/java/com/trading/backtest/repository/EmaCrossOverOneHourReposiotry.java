package com.trading.backtest.repository;

import com.trading.backtest.model.EmaCrossOverOneHour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmaCrossOverOneHourReposiotry extends JpaRepository<EmaCrossOverOneHour, Long> {
}
