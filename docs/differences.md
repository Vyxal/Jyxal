# Differences

Jyxal has a few differences from [Vyxal](https://github.com/Vyxal/Vyxal). They are listed here.

## Changed Elements

### `` E `` 
The "eval Python" part of `E` evals Java, not Python. It loads the snippet into [JShell](https://en.wikipedia.org/wiki/JShell) and the pushes the result back onto the stack. The JShell instance is cached, meaning that the following code will push two zeros on the stack: ``` `int i = 0`E`i`E```

### `` • ``
`•` can perform logarithms on complex numbers. It will also not operate on lists and vectorize instead

### `` ¨U `` 
`¨U` decompresses compressed responses, unlike Vyxal

### `` ∑ ``

`∑` operating on strings will sum the character codes of the chars in the string instead of returning the string

### `` \ ``

Single character strings are capable of decompression

### `` " ``

You may use any Java escape codes in strings

### `` ` ``, `` " ``, and `` : ``

`` ` `` is the duplicate command, `"` is a string, and `:` will be pair.

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

## Node Aliases

Node aliases are a very powerful new feature in Jyxal. They allow you to reference any "program nodes" in a single byte.
A program node is any expression that can be captured by the one element lambda. So `[aabbcc|c]` is a program node, and
so is `Y`, and likewise `kA` is a program node. Node aliases are placed at the start of a file, and have the
syntax `program_node+ ';' alias`, where "alias" is a single character.
