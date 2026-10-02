import java.io.File
import kotlin.system.exitProcess
import kotlin.test.Test

/** Menjalankan semua method @Test di kelas-kelas bernama `*Test` di direktori hasil kompilasi. */
fun main(args: Array<String>) {
    val root = File(args[0])
    val classNames = root.walkTopDown().filter { it.name.endsWith("Test.class") }
        .map { it.relativeTo(root).path.removeSuffix(".class").replace(File.separatorChar, '.') }.sorted().toList()
    var pass = 0
    var fail = 0
    var error = 0
    for (name in classNames) {
        val cls = Class.forName(name)
        val methods = cls.declaredMethods.filter { it.isAnnotationPresent(Test::class.java) }.sortedBy { it.name }
        for (m in methods) {
            val instance = cls.getDeclaredConstructor().also { it.isAccessible = true }.newInstance()
            val label = "${cls.simpleName}.${m.name}"
            try {
                m.isAccessible = true
                m.invoke(instance)
                println("  PASS   $label"); pass++
            } catch (e: java.lang.reflect.InvocationTargetException) {
                val c = e.targetException
                if (c is AssertionError) { println("  FAIL   $label\n           ${c.message}"); fail++ }
                else { println("  ERROR  $label\n           ${c::class.simpleName}: ${c.message}"); error++ }
            }
        }
    }
    println("\n$pass lulus, $fail gagal, $error error (dari ${pass + fail + error})")
    exitProcess(if (fail + error == 0) 0 else 1)
}
