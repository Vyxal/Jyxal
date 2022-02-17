# Differences

Jyxal has a few differences from [Vyxal](https://github.com/Vyxal/Vyxal). They are listed here.

## Elements

Element | Difference
------- | ----------
`E` | The "eval Python" part of `E` evals Java, not Python. It loads the snippet into [JShell](https://en.wikipedia.org/wiki/JShell) and the pushes the result back onto the stack. The JShell instance is cached, meaning that the following code will push two zeros on the stack: ``` `int i = 0`E`i`E```
`•` | `•` can perform logarithms on complex numbers. It will also not operate on lists and vectorize instead
``` ` ``` | Normal, uncompressed strings are not decompressed if they do not contain ASCII characters