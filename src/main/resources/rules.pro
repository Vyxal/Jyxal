-keep public class jyxal.Main {
    public static void main(java.lang.String[]);
}

-dontnote

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
}

-assumenosideeffects class io.github.seggan.jyxal.runtime.math.BigComplex {
    public io.github.seggan.jyxal.runtime.math.BigComplex add(io.github.seggan.jyxal.runtime.math.BigComplex);
    public io.github.seggan.jyxal.runtime.math.BigComplex subtract(io.github.seggan.jyxal.runtime.math.BigComplex);
    public io.github.seggan.jyxal.runtime.math.BigComplex multiply(io.github.seggan.jyxal.runtime.math.BigComplex);
    public io.github.seggan.jyxal.runtime.math.BigComplex divide(io.github.seggan.jyxal.runtime.math.BigComplex);
}