package com.trading.backtest.service;

import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.CachedIndicator;
import org.ta4j.core.num.Num;

public class DifferenceIndicator extends CachedIndicator<Num> {

    private final Indicator<Num> indicator1;
    private final Indicator<Num> indicator2;

    public DifferenceIndicator(Indicator<Num> indicator1, Indicator<Num> indicator2) {
        super(indicator1);
        this.indicator1 = indicator1;
        this.indicator2 = indicator2;
    }

    @Override
    protected Num calculate(int index) {
        return indicator1.getValue(index).minus(indicator2.getValue(index));
    }
}
