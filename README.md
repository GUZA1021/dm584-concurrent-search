# Concurrent Text Search (DM584)

Exam project for a course in concurrent programming. A command-line tool that
searches text files in a directory tree, where each search uses a different
Java concurrency model.

The CLI scaffolding (`main`, argument parsing, the `LocatedWord` class) was
provided as part of the exam. My work is the four search methods and their
helpers, each constrained to a specific concurrency strategy.

Words are identified with `BreakIterator` and compared case-insensitively.

## Commands

    java Exam shortestWord <directory>
    java Exam consonants   <directory> <consonants>
    java Exam allLines     <directory>
    java Exam suffix       <directory> <suffix> <limit>

| Command | What it does | Concurrency model |
|---|---|---|
| `shortestWord` | Shortest word across all `.txt` files, ties broken lexicographically | Fixed thread pool with `Future` |
| `consonants` | First word with exactly *n* consonants, returning as soon as one is found | Parallel stream with `findAny` |
| `allLines` | Words appearing on every line of a file, via set intersection | Virtual threads |
| `suffix` | Words ending with a given suffix, stopping once the limit is reached | Thread pool with `AtomicBoolean` cancellation flag |

The last two searches short-circuit: they stop scanning as soon as the result
is determined rather than processing every file.

## Running

    javac Exam.java
    java Exam shortestWord data

## Technologies
Java, `ExecutorService`, virtual threads, parallel streams, `BreakIterator`

## Disclaimer
Developed for educational purposes as part of a university course.