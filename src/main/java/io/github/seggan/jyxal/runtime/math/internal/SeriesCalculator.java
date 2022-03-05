package io.github.seggan.jyxal.runtime.math.internal;

/*
MIT License

Copyright (c) 2017 Eric Obermühlner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Utility class to calculate taylor series efficiently until the maximum error (as defined by the precision in the {@link MathContext} is reached.
 *
 * <p>Stores the factors of the taylor series terms so that future calculations will be faster.</p>
 */
public abstract class SeriesCalculator {

    private final boolean calculateInPairs;

    private final List<BigRational> factors = new ArrayList<>();

    /**
     * Constructs a {@link SeriesCalculator} that calculates single terms.
     */
    protected SeriesCalculator() {
        this(false);
    }

    /**
     * Constructs a {@link SeriesCalculator} with control over whether the sum terms are calculated in pairs.
     *
     * <p>Calculation of pairs is useful for taylor series where the terms alternate the sign.
     * In these cases it is more efficient to calculate two terms at once check then whether the acceptable error has been reached.</p>
     *
     * @param calculateInPairs <code>true</code> to calculate the terms in pairs, <code>false</code> to calculate single terms
     */
    protected SeriesCalculator(boolean calculateInPairs) {
        this.calculateInPairs = calculateInPairs;
    }

    /**
     * Calculates the series for the specified value x and the precision defined in the {@link MathContext}.
     *
     * @param x           the value x
     * @param mathContext the {@link MathContext}
     * @return the calculated result
     */
    public BigDecimal calculate(BigDecimal x, MathContext mathContext) {
        BigDecimal acceptableError = BigDecimal.ONE.movePointLeft(mathContext.getPrecision() + 1);

        PowerIterator powerIterator = createPowerIterator(x, mathContext);

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal step;
        int i = 0;
        do {
            BigRational factor;
            BigDecimal xToThePower;

            factor = getFactor(i);
            xToThePower = powerIterator.getCurrentPower();
            powerIterator.calculateNextPower();
            step = factor.getNumerator().multiply(xToThePower).divide(factor.getDenominator(), mathContext);
            i++;

            if (calculateInPairs) {
                factor = getFactor(i);
                xToThePower = powerIterator.getCurrentPower();
                powerIterator.calculateNextPower();
                BigDecimal step2 = factor.getNumerator().multiply(xToThePower).divide(factor.getDenominator(), mathContext);
                step = step.add(step2);
                i++;
            }

            sum = sum.add(step);
            //System.out.println(sum + " " + step);
        } while (step.abs().compareTo(acceptableError) > 0);

        return sum.round(mathContext);
    }

    /**
     * Creates the {@link PowerIterator} used for this series.
     *
     * @param x           the value x
     * @param mathContext the {@link MathContext}
     * @return the {@link PowerIterator}
     */
    protected abstract PowerIterator createPowerIterator(BigDecimal x, MathContext mathContext);

    /**
     * Returns the factor of the term with specified index.
     * <p>
     * All mutable state of this class (and all its subclasses) must be modified in this method.
     * This method is synchronized to allow thread-safe usage of this class.
     *
     * @param index the index (starting with 0)
     * @return the factor of the specified term
     */
    protected synchronized BigRational getFactor(int index) {
        while (factors.size() <= index) {
            BigRational factor = getCurrentFactor();
            addFactor(factor);
            calculateNextFactor();
        }
        return factors.get(index);
    }

    private void addFactor(BigRational factor) {
        factors.add(requireNonNull(factor, "Factor cannot be null"));
    }

    /**
     * Returns the factor of the highest term already calculated.
     * <p>When called for the first time will return the factor of the first term (index 0).</p>
     * <p>After this call the method {@link #calculateNextFactor()} will be called to prepare for the next term.</p>
     *
     * @return the factor of the highest term
     */
    protected abstract BigRational getCurrentFactor();

    /**
     * Calculates the factor of the next term.
     */
    protected abstract void calculateNextFactor();
}
