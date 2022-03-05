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
 * Calculates arc sinus using the Maclaurin series.
 * 
 * <p>See <a href="https://de.wikipedia.org/wiki/Taylorreihe">Wikipedia: Taylorreihe</a></p>
 * 
 * <p>No argument checking or optimizations are done.
 * This implementation is <strong>not</strong> intended to be called directly.</p>
 */
public class AsinCalculator extends SeriesCalculator {

	public static final AsinCalculator INSTANCE = new AsinCalculator();
	
	private int n = 0;
	private BigRational factorial2n = BigRational.ONE;
	private BigRational factorialN = BigRational.ONE;
	private BigRational fourPowerN = BigRational.ONE;
	
	private AsinCalculator() {
	}
	
	@Override
	protected BigRational getCurrentFactor() {
		return factorial2n.divide(fourPowerN.multiply(factorialN).multiply(factorialN).multiply(2 * n + 1));
	}
	
	@Override
	protected void calculateNextFactor() {
		n++;
		factorial2n = factorial2n.multiply(2 * n - 1).multiply(2 * n);
		factorialN = factorialN.multiply(n);
		fourPowerN = fourPowerN.multiply(4);
	}
	
	@Override
	protected PowerIterator createPowerIterator(BigDecimal x, MathContext mathContext) {
		return new PowerTwoNPlusOneIterator(x, mathContext);
	}
}
