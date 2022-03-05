# Jyxal

Jyxal is a coffee-flavored version of [Vyxal](https://github.com/Vyxal/Vyxal) compiled for the Java Virtual Machine. A list of differences can be seen [here](https://github.com/Vyxal/Jyxal/blob/master/docs/differences.md).

## Running

Note: Jyxal and all JARs produced by Jyxal require Java 17 or higher to run.

You can grab the latest release from the [releases tab](https://github.com/Vyxal/Jyxal/releases). You can also download the latest auto-built release by going to the [actions tab](https://github.com/Vyxal/Jyxal/actions), clicking on the latest action run on `master`, and scrolling all the way down to show the artifact. A third way is by [building it yourself](https://github.com/Vyxal/Jyxal#building-it-yourself).

To compile some Jyxal code, all you have to do is run the command `java -jar "Jyxal v<version>.jar" <file> [flags]` (for version 0.1.0, the JAR is named `Jyxal-0.1.0.jar`). A list of flags can be found [here](https://github.com/Vyxal/Jyxal#compiler-flags). The compiler will then compile the source file into a JAR with the same name. This JAR contains the entire runtime for Jyxal, so no extra files are needed. There will also be another file called `debug.log` generated, but you may safely ignore/delete it. To run the resultant JAR, run `java -jar <your-program>.jar [flags] [inputs]`.

## Building it Yourself

Jyxal uses Gradle for building, but the wrapper is included in the repository, so you do not have to build it yourself. To build Jyxal, run `./gradlew shadowJar`. Your IDE's "build" command will not work, as `shadowJar` also copied the runtime environment into the resulting JAR, while the command does not do that. The resulting JAR will be located under `/build/libs/`.

## Compiler Flags
Flag | What It Does
-----|-------------
`V`  | This flag makes the compiler read files in the Vyxal encoding
`D`  | This flag makes the compiler print the parse tree, useful for debugging.
`o`  | This flag forces the compiler to not perform any optimizations on the compiled bytecode.
`f`  | This flag pipes implicit output of the program into a file called `test.out`, useful as some terminals cannot display unicode chars correctly. 