import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/*
This is the exam for DM584 - Concurrent Programming, Spring 2025.

Your task is to implement the following methods of class Exam:
- shortestWord;
- wordWithConsonants;
- findWordsCommonToAllLines;
- wordsEndingWith.

These methods search text files for particular words.
You must use a BreakIterator to identify words in a text file,
which you can obtain by calling BreakIterator.getWordInstance().
For more details on the usage of BreakIterator, please see the corresponding video lecture in the course.

The implementations of these methods must exploit concurrency to achieve improved performance.

The only code that you can change is the implementation of these methods.
In particular, you cannot change the signatures (return type, name, parameters) of any method, and you cannot edit method main.
The current code of (some of) these methods throws an UnsupportedOperationException: remove that line before proceeding on to the implementation.
*/
public class Exam {
	// Do not change this method
	public static void main(String[] args) {
		checkArguments(args.length > 0,
				"You must choose a command: help, shortestWord, consonants, allLines, or suffix.");
		switch (args[0]) {
			case "help":
				System.out.println(
						"Available commands: help, shortestWord, consonants, allLines, or suffix.\nFor example, try:\n\tjava Exam shortestWord data");
				break;
			case "shortestWord":
				checkArguments(args.length == 2, "Usage: java Exam.java shortestWord <directory>");
				String shortestWord = shortestWord(Paths.get(args[1]));
				System.out.println("The shortest word found is " + shortestWord);
				break;
			case "consonants":
				checkArguments(args.length == 3, "Usage: java Exam.java consonants <directory> <consonants>");
				int consonants = Integer.parseInt(args[2]);
				Optional<LocatedWord> word = wordWithConsonants(Paths.get(args[1]), consonants);
				word.ifPresentOrElse(
							locatedWord -> System.out.println("Found " + locatedWord.word + " in " + locatedWord.filepath + ":" + locatedWord.line),
							() -> System.out.println("No word found with " + args[2] + " consonants."));
				break;
			case "allLines":
				checkArguments(args.length == 2, "Usage: java Exam.java allLines <directory>");
				List<LocatedWord> commonWords = findWordsCommonToAllLines(Paths.get(args[1]));
				System.out.println("Found " + commonWords.size() + " words");
				commonWords.forEach(locatedWord ->
							System.out.println(locatedWord.word + ":" + locatedWord.filepath));
				break;
			case "suffix":
				checkArguments(args.length == 4, "Usage: java Exam.java suffix <directory> <suffix> <limit>");
				int limit = Integer.parseInt(args[3]);
				List<LocatedWord> words = wordsEndingWith(Paths.get(args[1]), args[2], limit);
				if (words.size() > limit) {
					System.out.println("WARNING: Implementation of wordsEndingWith computes more than " + args[3] + " words!");
				}
				words.forEach(loc -> System.out.println(loc.word + ":" + loc.filepath + ":" + loc.line));
				break;
			default:
				System.out.println("Unrecognised command: " + args[0] + ". Try java Exam.java help.");
				break;
		}
	}

	// Do not change this method
	private static void checkArguments(Boolean check, String message) {
		if (!check) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * Returns the shortest word present in the text files contained in a directory.
	 *
	 * This method recursively visits a directory to find all the text files
	 * contained in it and its subdirectories (and the subdirectories of these
	 * subdirectories, etc.).
	 *
	 * You must consider only files ending with a ".txt" suffix. You are
	 * guaranteed that they will be text files.
	 *
	 * The method should return the shortest word found among all text files.
	 * If multiple words are identified as shortest, the method should return
	 * the one that precedes the other shortest words lexicographically.
   *
   * Allowed concurrency strategies: executors.
	 *
	 * @param dir the directory to search
	 * @return the shortest word found among all text files inside of dir
	 */

	private static String shortestWord(Path dir) {
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<String>> futures = new ArrayList<>();

		try(Stream<Path> paths = Files.walk(dir)){
			paths.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".txt"))
			.forEach(file -> {
				futures.add(executor.submit(() -> {
					return FindShortestWordInFile(file);
				}));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		String shortest = null;
		for(Future<String> future : futures){
			try {
				String word = future.get();
				if (word != null && (shortest == null || word.length() < shortest.length() || 
				(word.length() == shortest.length() && word.compareTo(shortest) < 0))) {
					shortest = word;
				}
			} catch (InterruptedException | ExecutionException e){
				e.printStackTrace();
			}
		}

		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	return shortest;
	}

	//Extracts words from a Line using BreakIterator
	private static List<String> extractWords(String text){
		List<String> words = new ArrayList<>();
		BreakIterator it = BreakIterator.getWordInstance();
		it.setText(text);

		int start = it.first();
		int end = it.next();
		while (end != BreakIterator.DONE) {
			String word = text.substring(start, end).trim();
			if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0))) {
				words.add(word); 
			}
			start = end;
			end = it.next();
		}
		return words;
	}

	//Helper to find shortest word in a single file
	private static String FindShortestWordInFile(Path file) throws IOException{
		String content = Files.readString(file);
		List<String> words = extractWords(content);
		String shortest = null;
		for(String word : words){
			if (shortest == null || word.length() < shortest.length() ||
			(word.length()) == shortest.length() && word.compareTo(shortest) < 0) {
				shortest = word;
			}
		}
		return shortest;
	}

	/**
	 * Returns an Optional<LocatedWord> about a word found in the files of the given directory
	 * containing the given number of consonants.
	 *
	 * This method should return *as soon as possible*: as soon as a satisfactory
	 * word is found, the method should return a result without waiting for the
	 * processing of remaining files and/or other data.
   *
   * Allowed concurrency strategies: all.
	 *
	 * @param dir the directory to search
	 * @param numberOfConsonants the number of consonants the word must contain
	 * @return an optional LocatedWord about a word containing exactly n consonants
	 */
	private static Optional<LocatedWord> wordWithConsonants(Path dir, int numberOfConsonants) { //lavet nogenlunde, men mangler fix ift parallelstream. At finde konsonanter virker, men stream no
		Optional<LocatedWord> result = java.util.Optional.empty();

		try{
			result = Files
				.walk(dir)
				.parallel()
				.filter( Files::isRegularFile)
				.filter(p -> p.toString().endsWith(".txt"))
				.flatMap(file -> {
					try{
						List<String> lines = Files.readAllLines(file);
						for(int lineNumber = 0; lineNumber < lines.size(); lineNumber++){
							String line = lines.get(lineNumber);
							List<String> words = extractWords(line);
							for(String word : words){
								if (countConsonants(word.toLowerCase()) == numberOfConsonants) {
									return Stream.of(new LocatedWord(word, file, lineNumber +1));
								}
							}
						}
						return Stream.empty();
					} catch ( IOException e){
						return Stream.empty();
					}
					})
					.findAny();
		} catch (IOException e){
			e.printStackTrace();
			result = Optional.empty();}
		return result;
	}

	//Helper to count consonants in a word
	private static int countConsonants(String word){
		int count = 0;
		for(char c: word.toLowerCase().toCharArray()) {
			if (Character.isLetter(c) && !"aeiou".contains(String.valueOf(c))){
				count++;
			}
		}
		return count;
	}
	/**
	 * Returns the words that appear on every line of a text file contained in the given directory.
	 *
	 * This method should return a list of LocatedWord objects, where each LocatedWord object
	 * should consist of:
	 * - a word appearing in every line of a file
	 * - the path to the file containing such word.
	 *
	 * Words must be compared case-insensitively.
   *
   * Allowed concurrency strategies: virtual threads.
	 *
	 * @param dir the directory to search
	 * @return a list of words that, within a file inside dir, appear on every line
	 */
	private static List<LocatedWord> findWordsCommonToAllLines(Path dir) {
		List<LocatedWord> result = new ArrayList<>();
		List<Thread> threads = new ArrayList<>();
		List<List<LocatedWord>> partialResult = java.util.Collections.synchronizedList(new ArrayList<>());

		try (Stream<Path> files = Files.walk(dir)) {
			files
			.filter(Files::isRegularFile)
			.filter(p -> p.toString().endsWith(".txt"))
			.forEach(file -> {
				Thread thread = Thread.ofVirtual().start(() -> {
					try {
						List<String> lines = Files.readAllLines(file);
						if (lines.isEmpty()) return;
						
						List<Set<String>> wordSets = new ArrayList<>();
						//extract words sets per line

						for(String line : lines){
							Set<String> lineWords = new HashSet<>();
							for(String word : extractWords(line)){
								lineWords.add(word.toLowerCase());
							}
							if (!lineWords.isEmpty()) {
								wordSets.add(lineWords);	
							}
						}

						//Find interscetion of all the lines
						Set<String> intersection = new HashSet<>(wordSets.get(0));
						for (int i = 1; i < wordSets.size(); i++) {
							intersection.retainAll(wordSets.get(i));
							if (intersection.isEmpty()) return;
						}

						List<LocatedWord> fileResult = new ArrayList<>();
						for(String word : intersection) {
							fileResult.add(new LocatedWord(word, file, 0));
						}
						partialResult.add(fileResult);
					} catch (IOException e){
						e.printStackTrace();
					}
				});
				threads.add(thread);
		});
		for(Thread t : threads) {
			try {
			t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	} catch (IOException e){
	e.printStackTrace();
	}
	for(List<LocatedWord> list : partialResult) {
		result.addAll(list);
	}

	return result;
}

	/**
	 * Returns a list of words found in the given directory ending with the given suffix.
	 *
	 * The size of the returned list must not exceed the given limit.
	 * Therefore, this method should return *as soon as possible*: if the list
	 * reaches the given limit, no more elements should be added.
   *
   * Allowed concurrency strategies: all.
	 *
	 * @param dir the directory to search
	 * @param suffix the suffix to be searched for
	 * @param limit the size limit for the returned list
	 * @return a list of locations where the given suffix has been found
	 */
	private static List<LocatedWord> wordsEndingWith(Path dir, String suffix, int limit) {
		List<LocatedWord> result = java.util.Collections.synchronizedList(new ArrayList<>());
		AtomicBoolean done = new AtomicBoolean(false);
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try(Stream<Path> files = Files.walk(dir)) {
			files.filter(Files::isRegularFile)
				.filter(p -> p.toString().endsWith(".txt"))
				.forEach(file -> executor.submit(() -> {
					try{
						List<String> lines = Files.readAllLines(file);
						for(int i = 0; i< lines.size(); i++){
							for(String word : extractWords(lines.get(i))){
								if (done.get()) return;{
								if (word.toLowerCase().endsWith(suffix.toLowerCase())) {
									synchronized (result) {
										if (result.size() < limit) { 
										result.add(new LocatedWord(word, file, i+1));
										if (result.size() >= limit) {
											done.set(true);
											return;
										}											
									} else {
										done.set(true);
										return;
									}
								}
							}
						}
					}
				}
				} catch (IOException e){
					e.printStackTrace();
				}
			}));	
	} catch (IOException e){
		e.printStackTrace();
	} 
	
	executor.shutdown();
	try {
		executor.awaitTermination(1, TimeUnit.DAYS);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

	return result;
}

	// Do not change this class
	private static class LocatedWord {
		private final String word;
		private final Path filepath;
		private final int line;

		private LocatedWord(String word, Path filepath, int line) {
			this.word = word;
			this.filepath = filepath;
			this.line = line;
		}
	}

	// Do not change this class
	private static class InternalException extends RuntimeException {
		private InternalException(String message) {
			super(message);
		}
	}
}
