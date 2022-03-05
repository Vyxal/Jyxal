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

/**
 * Iterator over the powers of a value x.
 *
 * <p>This API allows to efficiently calculate the various powers of x in a taylor series by storing intermediate results.</p>
 * <p>For example x<sup>n</sup> can be calculated using one multiplication by storing the previously calculated x<sup>n-1</sup> and x.</p>
 *
 * <p>{@link #getCurrentPower()} will be called first to retrieve the initial value.</p>
 * <p>
 * For later iterations {@link #calculateNextPower()} will be called before {@link #getCurrentPower()}.
 */
public interface PowerIterator {

    /**
     * Returns the current power.
     *
     * @return the current power.
     */
    BigDecimal getCurrentPower();

    /**
     * Calculates the next power.
     */
    void calculateNextPower();
}