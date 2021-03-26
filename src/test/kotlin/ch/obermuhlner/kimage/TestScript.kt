package ch.obermuhlner.kimage

import javax.script.Compilable
import javax.script.ScriptEngineManager

object TestScript {
    @JvmStatic
    fun main(args: Array<String>) {
        exampleScript()
    }

    fun exampleScript() {
        val manager = ScriptEngineManager()
        val engine = manager.getEngineByName("kotlin")
        val compiler = engine as Compilable

        val script = """
            println("Hello")
            println("input = " + input)
        """.trimIndent()

        val bindings = engine.createBindings()
        bindings["input"] = "Example Input String"

        println("Evaluating script ...")
        engine.eval(script, bindings)

        val compiledScript = compiler.compile(script)

        println("Evaluating compiled script ...")
        compiledScript.eval(bindings)
    }
}