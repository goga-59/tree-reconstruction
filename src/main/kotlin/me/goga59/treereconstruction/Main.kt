package me.goga59.treereconstruction

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

// Ребро дерева от родительской вершины к дочерней
data class Edge(val parent: Int, val child: Int)

// Восстановленное дерево с количеством вершин и списком ребер
data class Tree(val vertexCount: Int, val edges: List<Edge>)

fun decodeTree(reader: Reader): Tree {
    // ArrayDeque используется как стек пути от корня до текущей вершины
    val path = ArrayDeque<Int>().apply { addLast(1) }
    val edges = mutableListOf<Edge>()
    val buffer = CharArray(DEFAULT_BUFFER_SIZE)

    // Корень имеет номер 1, поэтому следующей вершине назначается номер 2
    var nextVertex = 2
    var symbolIndex = 0

    while (true) {
        val count = reader.read(buffer)
        if (count < 0) break

        for (index in 0 until count) {
            val symbol = buffer[index]
            if (symbol.isWhitespace()) continue
            symbolIndex++

            // 0 - перейти к новой дочерней вершине, 1 - вернуться к родителю
            when (symbol) {
                '0' -> {
                    val child = nextVertex++
                    edges += Edge(path.last, child)
                    path.addLast(child)
                }

                '1' -> {
                    require(path.size > 1) { "'1' at position $symbolIndex goes above the root" }
                    path.removeLast()
                }

                else -> throw IllegalArgumentException("Invalid symbol '$symbol' at position $symbolIndex")
            }
        }
    }

    require(path.size == 1) { "Traversal did not return to the root" }
    return Tree(nextVertex - 1, edges)
}

fun writeDot(tree: Tree, writer: Writer) {
    with(writer) {
        appendLine("graph Tree {")
        appendLine("\tnode [shape=circle];")
        appendLine("\t1 [style=filled, fillcolor=lightblue];")
        tree.edges.forEach { appendLine("\t${it.parent} -- ${it.child};") }
        appendLine("}")
    }
}

fun renderPng(dot: Path, png: Path) {
    val process = ProcessBuilder("dot", "-Tpng", dot.toString(), "-o", png.toString()).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    check(process.waitFor() == 0) { "Graphviz failed: $output" }
}

fun main(args: Array<String>) {
    try {
        require(args.size <= 1) { "Usage: tree-reconstruction [path-to-file.txt]" }

        val input = args.singleOrNull()?.let { Path(it) } ?: Path("examples/small.txt")
        val results = Path("results")
        val dot = results / "dot" / "${input.nameWithoutExtension}.dot"
        val png = results / "png" / "${input.nameWithoutExtension}.png"

        val tree = input.bufferedReader().use(::decodeTree)

        dot.parent.createDirectories()
        png.parent.createDirectories()
        dot.bufferedWriter().use { writeDot(tree, it) }

        renderPng(dot, png)

        println("Vertices: ${tree.vertexCount}, edges: ${tree.edges.size}")
        println("DOT: ${dot.absolutePathString()}")
        println("PNG: ${png.absolutePathString()}")
    } catch (ex: Exception) {
        System.err.println("Error: ${ex.message}")
        exitProcess(1)
    }
}
