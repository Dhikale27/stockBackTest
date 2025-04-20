package com.trading.backtest.service;

import org.ta4j.core.Indicator;
import org.ta4j.core.Position;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AbstractRule;

public class StopGainRule extends AbstractRule {
    private final Indicator<Num> indicator;
    private final Num gainRatio;

    public StopGainRule(Indicator<Num> indicator, Num gainRatio) {
        this.indicator = indicator;
        this.gainRatio = gainRatio;
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
            Num threshold = entryPrice.multipliedBy(gainRatio.plus(entryPrice.numOf(1)));
            return currentPrice.isGreaterThanOrEqual(threshold);
        } else {
            Num threshold = entryPrice.multipliedBy(entryPrice.numOf(1).minus(gainRatio));
            return currentPrice.isLessThanOrEqual(threshold);
        }
    }
}