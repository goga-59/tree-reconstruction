package me.goga59.graphlabs

import java.io.Reader
import java.io.Writer
import java.nio.file.Path
import java.util.ArrayDeque
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

data class Edge(val parent: Int, val child: Int)

data class Tree(val vertexCount: Int, val edges: List<Edge>)

fun decodeTree(reader: Reader): Tree {
    val path = ArrayDeque<Int>().apply { addLast(1) }
    val edges = mutableListOf<Edge>()
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)

    var nextVertex = 2
    var symbolIndex = 0

    while (true) {
        val count = reader.read(buffer)
        if (count < 0) break

        for (index in 0 until count) {
            val symbol = buffer[index]
            if (symbol.isWhitespace()) continue
            symbolIndex++

            when (symbol) {
                '0' -> {
                    val child = nextVertex++
                    edges += Edge(path.last, child)
                    path.addLast(child)
                }

                '1' -> {
                    require(path.size > 1) { "Символ на позиции $symbolIndex возвращает выше корня" }
                    path.removeLast()
                }

                else -> throw IllegalArgumentException(
                    "Недопустимый символ '$symbol' на позиции $symbolIndex",
                )
            }
        }
    }

    require(path.size == 1) { "Обход не вернулся в корень" }
    return Tree(nextVertex - 1, edges)
}

fun writeDot(tree: Tree, writer: Writer) {
    with(writer) {
        appendLine("digraph Tree {")
        appendLine("\tgraph [ordering=out];")
        appendLine("\tnode [shape=circle];")
        appendLine("\t1 [style=filled, fillcolor=lightblue];")
        tree.edges.forEach { appendLine("\t${it.parent} -> ${it.child};") }
        appendLine("}")
    }
}

fun renderPng(dot: Path, png: Path) {
    val process = ProcessBuilder("dot", "-Tpng", dot.toString(), "-o", png.toString()).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    check(process.waitFor() == 0) { "Graphviz завершился с ошибкой: $output" }
}

fun main(args: Array<String>) {
    try {
        require(args.size <= 1) { "Использование: graph-labs [путь-к-файлу.txt]" }

        val root = Path("src/main/kotlin/me/goga59/graphlabs")
        val input = args.singleOrNull()?.let { Path(it) } ?: (root / "examples" / "small.txt")
        val baseName = input.nameWithoutExtension
        val results = root / "results"
        val dot = results / "dot" / "$baseName.dot"
        val png = results / "png" / "$baseName.png"

        val tree = input.bufferedReader().use(::decodeTree)

        dot.parent.createDirectories()
        png.parent.createDirectories()
        dot.bufferedWriter().use { writeDot(tree, it) }

        renderPng(dot, png)

        println("Восстановлено вершин: ${tree.vertexCount}, ребер: ${tree.edges.size}")
        println("DOT: ${dot.absolutePathString()}")
        println("PNG: ${png.absolutePathString()}")
    } catch (ex: Exception) {
        System.err.println("Ошибка: ${ex.message}")
        exitProcess(1)
    }
}
