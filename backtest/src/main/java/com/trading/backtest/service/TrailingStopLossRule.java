package com.trading.backtest.service;

import org.ta4j.core.Indicator;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class TrailingStopLossRule extends AbstractRule {
    private final Indicator<Num> indicator;
    private final Num triggerRatio;
    private Num highestPrice = null;
    private Num lowestPrice = null;

    public TrailingStopLossRule(Indicator<Num> indicator, Num triggerRatio) {
        this.indicator = indicator;
        this.triggerRatio = triggerRatio;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        if (!tradingRecord.getCurrentPosition().isOpened()) {
            return false;
        }
        
        Position position = tradingRecord.getCurrentPosition();
        Num entryPrice = position.getEntry().getNetPrice();
        Num currentPrice = indicator.getValue(index);

        if (position.getEntry().isBuy()) {
            // For long positions
            if (highestPrice == null || currentPrice.isGreaterThan(highestPrice)) {
                highestPrice = currentPrice;
            }
            // Check if we've reached the trigger profit
            if (highestPrice.isGreaterThanOrEqual(entryPrice.multipliedBy(entryPrice.numOf(1).plus(triggerRatio)))) {
                // Trail stop at entry price
                return currentPrice.isLessThanOrEqual(entryPrice);
            }
        } else {
            // For short positions
            if (lowestPrice == null || currentPrice.isLessThan(lowestPrice)) {
                lowestPrice = currentPrice;
            }
            // Check if we've reached the trigger profit
            if (lowestPrice.isLessThanOrEqual(entryPrice.multipliedBy(entryPrice.numOf(1).minus(triggerRatio)))) {
                // Trail stop at entry price
                return currentPrice.isGreaterThanOrEqual(entryPrice);
            }
        }
        return false;
    }
}