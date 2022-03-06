# Differences

Jyxal has a few differences from [Vyxal](https://github.com/Vyxal/Vyxal). They are listed here.

## Changed Elements

Element | Difference
------- | ----------
`E` | The "eval Python" part of `E` evals Java, not Python. It loads the snippet into [JShell](https://en.wikipedia.org/wiki/JShell) and the pushes the result back onto the stack. The JShell instance is cached, meaning that the following code will push two zeros on the stack: ``` `int i = 0`E`i`E```
`•` | `•` can perform logarithms on complex numbers. It will also not operate on lists and vectorize instead
`¨U` | `¨U` decompresses compressed responses, unlike Vyxal
`∑` | `∑` operating on strings will sum the character codes of the chars in the string instead of returning the string

## Added Elements

### `` øJ `` (JSON Parse)
Parses a JSON string

#### Overloads
- lst a: vectorise
- otherwise: `json.parse(str(a))`
----------------------

### `` Þd `` (Get/Set Dictionary/Map)
Gets a value from a dictionary/map (if not present, 0) or sets a value in the dictionary/map

#### Overloads
- any a, lst b: `dict(b).get(a, 0)`
- any a, lst b, any c: `dict(b)[a] = c`

## Aliases
Aliases are a very powerful new feature in Jyxal. They allow you to reference digraphs in one byte. To use them, simply add a structure of the form `a|b` to the **start** of your program, where `a` is any digraph, and `b` is a single character. Any element previously assigned under `b` will be inaccessible to the program.
