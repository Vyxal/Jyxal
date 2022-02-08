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

/**
 * Calculates sinus hyperbolicus using the Taylor series.
 * 
 * <p>See <a href="https://en.wikipedia.org/wiki/Taylor_series">Wikipedia: Taylor series</a></p>
 * 
 * <p>No argument checking or optimizations are done.
 * This implementation is <strong>not</strong> intended to be called directly.</p>
 */
public class SinhCalculator extends SeriesCalculator {

	public static final SinhCalculator INSTANCE = new SinhCalculator();
	
	private int n = 0;

	private BigRational factorial2nPlus1 = BigRational.ONE;
	
	private SinhCalculator() {
		super(true);
	}
	
	@Override
	protected BigRational getCurrentFactor() {
		return factorial2nPlus1.reciprocal();
	}
	
	@Override
	protected void calculateNextFactor() {
		n++;
		factorial2nPlus1 = factorial2nPlus1.multiply(2 * n);
		factorial2nPlus1 = factorial2nPlus1.multiply(2 * n + 1);
	}
	
	@Override
	protected PowerIterator createPowerIterator(BigDecimal x, MathContext mathContext) {
		return new PowerTwoNPlusOneIterator(x, mathContext);
	}
}
