package search

import java.io.File
import java.util.*

object IndexBuilder {
    fun fromFile(fileName: String): Index {
        val scanner = Scanner(File(fileName))

        return Index().apply {
            while (scanner.hasNextLine()) {
                add(scanner.nextLine())
            }
        }
    }
}

enum class SearchStrategy {
    ALL, ANY, NONE
}

sealed class SearchResult {
    object NotFound : SearchResult()

    data class Matched(val elements: Collection<String>) : SearchResult()
}

class Index {
    private val lines = mutableListOf<String>()
    private val index = mutableMapOf<String, MutableSet<Int>>()
    private val size get() = lines.size

    private fun Set<Int>.lines() = lines.filterIndexed { index, _ -> index in this }.toSet()

    fun add(line: String) {
        // Update inverted index.
        line.toLowerCase().split(" ").forEach { word ->
            val positions = index.getOrPut(word) { mutableSetOf() }
            positions += size
            index[word] = positions
        }

        lines.add(line)
    }

    fun search(search: String, strategy: SearchStrategy): SearchResult {
        val lookup = search.toLowerCase().split(" ")

        // Merge of all sets.
        val any: () -> Set<Int> = {
            lookup.flatMap { word -> index[word] ?: emptySet<Int>() }.toSet()
        }

        // Intersection of all sets.
        val all: () -> Set<Int> = {
            lookup.map { word -> index[word] ?: emptySet<Int>() }
                .zipWithNext { a, b -> a intersect b }
                .flatten()
                .toSet()
        }

        // All possible lines as set.
        val range: () -> Set<Int> = { (0 until size).toSet() }

        val result = when (strategy) {
            SearchStrategy.ANY -> { any() }
            SearchStrategy.ALL -> { if (lookup.size > 1) all() else any() }
            SearchStrategy.NONE -> { range() - any() }
        }

        return when {
            result.isNotEmpty() -> SearchResult.Matched(result.lines())
            else -> SearchResult.NotFound
        }
    }

    override fun toString() = lines.joinToString("\n")
}

fun main(args: Array<String>) {
    val index = IndexBuilder.fromFile(args[1])

    fun Int.isRunning() = this > 0

    do {
        println("""
            === Menu ===
            1. Find a person
            2. Print all people
            0. Exit
        """.trimIndent())

        val action = readLine()!!.toInt()
        println()

        when (action) {
            1 -> {
                println("Select a matching strategy: ALL, ANY, NONE")
                val strategy = SearchStrategy.valueOf(readLine()!!)

                println("\nEnter a name or email to search all suitable people.")
                val search = readLine()!!

                when (val result = index.search(search, strategy)) {
                    is SearchResult.NotFound -> println("No matching people found.")
                    is SearchResult.Matched -> println(result.elements.joinToString("\n"))
                }
            }
            2 -> {
                println("=== List of people ===")
                println(index)
            }
            0 -> println("Bye!")
            else -> println("Incorrect option! Try again.")
        }

        if (action.isRunning()) {
            println()
        }
    } while (action.isRunning())
}
