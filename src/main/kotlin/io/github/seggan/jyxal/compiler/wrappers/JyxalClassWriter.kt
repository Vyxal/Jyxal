package io.github.seggan.jyxal.compiler.wrappers

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.util.regex.Pattern

class JyxalClassWriter(flags: Int) : ClassWriter(flags) {

    override fun getCommonSuperClass(type1: String, type2: String): String {
        return super.getCommonSuperClass(
                RUNTIME.matcher(type1).replaceAll("io/github/seggan/jyxal/runtime/"),
                RUNTIME.matcher(type2).replaceAll("io/github/seggan/jyxal/runtime/")
        )
    }

    fun visitMethod(access: Int, name: String, desc: String): JyxalMethod {

        return if (name == "main" && desc == "([Ljava/lang/String;)V" && access == Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC) {
            MainMethod(this, access, name, desc)
        } else {
            Function(this, access, name, desc)
        }
    }

    companion object {
        private val RUNTIME = Pattern.compile("^runtime/")
    }
}