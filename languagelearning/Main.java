package com.languagelearning;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Abstract base class for tests, allowing for future extensions.
 */
abstract class Test {
    protected String testName;
    protected List<Word> vocabList;
    protected Scanner input;

    // Constructor to initialize test details
    protected Test(String name, List<Word> words, Scanner sc) {
        this.testName = name;
        this.vocabList = words;
        this.input = sc;
    }

    /**
     * Executes the test and returns the result.
     */
    public abstract TestResult run();

    public String getName() {
        return testName;
    }
}

/**
 * Class to store test results.
 */
class TestResult {
    public String testName;
    public int correctAnswers;
    public int totalQuestions;
    public LocalDateTime whenTaken;

    public TestResult(String name, int correct, int total) {
        this.testName = name;
        this.correctAnswers = correct;
        this.totalQuestions = total;
        this.whenTaken = LocalDateTime.now();
    }
}

/**
 * Manages loading and saving vocabulary files in a simple pipe-separated format.
 */
class VocabularyManager {
    private static final String DATA_FOLDER = "data";

    // Loads vocabulary from a specified file.
    public static List<Word> loadVocabulary(String sourceLang, String targetLang) {
        List<Word> wordList = new ArrayList<>();
        String filename = getFileName(sourceLang, targetLang);
        Path filePath = Paths.get(DATA_FOLDER, filename);

        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    String[] parts = currentLine.split("\\|", 3);
                    if (parts.length == 3) {
                        wordList.add(new Word(parts[0], parts[1], parts[2]));
                    }
                }
            } catch (IOException ex) {
                System.err.println("Failed to load vocabulary: " + ex.getMessage());
            }
        }
        return wordList;
    }

    // Saves vocabulary to a specified file.
    public static void saveVocabulary(String sourceLang, String targetLang, List<Word> words) {
        try {
            Files.createDirectories(Paths.get(DATA_FOLDER));
            String filename = getFileName(sourceLang, targetLang);
            Path filePath = Paths.get(DATA_FOLDER, filename);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                for (Word w : words) {
                    writer.write(w.word + "|" + w.translation + "|" + w.partOfSpeech);
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            System.err.println("Error saving vocabulary: " + ex.getMessage());
        }
    }

    private static String getFileName(String src, String tgt) {
        return "vocab_" + src + "_" + tgt + ".txt";
    }
}

/**
 * Manages quiz scores by saving them to text files.
 */
class ScoreManager {
    private static final String DATA_FOLDER = "data";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static void saveScore(String sourceLang, String targetLang, TestResult result) {
        try {
            Files.createDirectories(Paths.get(DATA_FOLDER));
            String filename = "scores_" + sourceLang + "_" + targetLang + ".txt";
            Path filePath = Paths.get(DATA_FOLDER, filename);
            try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                String scoreRecord = result.testName + "|" +
                        result.correctAnswers + "|" +
                        result.totalQuestions + "|" +
                        result.whenTaken.format(DATE_FORMAT);
                writer.write(scoreRecord);
                writer.newLine();
            }
        } catch (IOException ex) {
            System.err.println("Error saving score: " + ex.getMessage());
        }
    }
}

/**
 * Main application class that handles menu and app coordination.
 */
class LanguageLearningApp {
    private String knownLanguage;
    private String learningLanguage;
    private List<Word> myVocabulary;
    private Scanner userInput = new Scanner(System.in);


    // Supported parts of speech
    private static final Set<String> ALLOWED_POS = Set.of(
            "noun", "verb", "adjective", "adverb", "pronoun"
    );

    public static void main(String[] args) {
        LanguageLearningApp app = new LanguageLearningApp();
        app.startApp();
    }

    /** Main entry point. */
    private void startApp() {
        System.out.println("Welcome to the language learning app!");
        setupLanguages();

        // Main menu loop
        boolean keepRunning = true;
        while (keepRunning) {
            showMainMenu();
            String choice = userInput.nextLine().trim();
            switch (choice) {
                case "1": setupLanguages(); break;
                case "2": addNewWord(); break;
                case "3": showVocabCount(); break;
                case "4": displayVocabulary(); break;
                case "5": runFlipDrill(); break;
                case "6": runGuessQuiz(); break;
                case "7": runFillBlankQuiz(); break;
                case "8":
                    System.out.println("Thanks for using the app! Goodbye!");
                    keepRunning = false;
                    break;
                default: System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private void showMainMenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Change language pair");
        System.out.println("2. Add new word");
        System.out.println("3. Show vocabulary count");
        System.out.println("4. Browse vocabulary");
        System.out.println("5. Flip drill mode");
        System.out.println("6. Guess the word quiz");
        System.out.println("7. Fill in the blanks quiz");
        System.out.println("8. Exit");
        System.out.print("Your choice: ");
    }

    /** Set up or change the language pair. */
    private void setupLanguages() {
        System.out.print("Enter your known language: ");
        knownLanguage = userInput.nextLine().trim();
        System.out.print("Enter the language you're learning: ");
        learningLanguage = userInput.nextLine().trim();

        // FR2: clear any previous vocabulary when languages change
        myVocabulary = VocabularyManager.loadVocabulary(knownLanguage, learningLanguage);
        System.out.printf("Loaded %d words from previous sessions.%n", myVocabulary.size());

        System.out.printf("Language pair: %s - %s\n", knownLanguage, learningLanguage);
        System.out.printf("Loaded %d words from previous sessions.\n", myVocabulary.size());
    }

    /** Add a new word to vocabulary. */
    private void addNewWord() {
        System.out.print("Enter word in " + learningLanguage + ": ");
        String foreignWord = userInput.nextLine().trim().toLowerCase();

        System.out.print("Enter translation in " + knownLanguage + ": ");
        String translation = userInput.nextLine().trim().toLowerCase();

        System.out.print("Part of speech (noun/verb/adjective/adverb/pronoun): ");
        String pos = userInput.nextLine().trim().toLowerCase();

        // Validate part of speech
        if (!ALLOWED_POS.contains(pos)) {
            System.out.println("Invalid part of speech. Must be one of: " + ALLOWED_POS);
            return;
        }

        Word newWord = new Word(foreignWord, translation, pos);

        // Check for duplicates
        if (myVocabulary.contains(newWord)) {
            System.out.println("This word already exists in your vocabulary!");
        } else {
            myVocabulary.add(newWord);
            VocabularyManager.saveVocabulary(knownLanguage, learningLanguage, myVocabulary);
            System.out.println("Word added successfully!");
        }
    }

    /** Show current vocabulary count. */
    private void showVocabCount() {
        System.out.printf("You have %d words in your %s - %s vocabulary.\n",
                myVocabulary.size(), knownLanguage, learningLanguage);
    }

    /**
     * Display vocabulary with pagination.
     */
    private void displayVocabulary() {
        if (myVocabulary.isEmpty()) {
            System.out.println("Your vocabulary is empty. Add some words first!");
            return;
        }

        System.out.print("How many words per page? ");
        int wordsPerPage;
        try {
            wordsPerPage = Integer.parseInt(userInput.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number!");
            return;
        }

        // Sort alphabetically
        List<Word> sortedWords = new ArrayList<>(myVocabulary);
        sortedWords.sort(Comparator.comparing(w -> w.word));

        int totalPages = (int) Math.ceil((double) sortedWords.size() / wordsPerPage);

        for (int currentPage = 0; currentPage < totalPages; currentPage++) {
            System.out.printf("\n--- Page %d of %d ---\n", currentPage + 1, totalPages);

            int startIndex = currentPage * wordsPerPage;
            int endIndex = Math.min(startIndex + wordsPerPage, sortedWords.size());

            for (int i = startIndex; i < endIndex; i++) {
                System.out.println("  " + sortedWords.get(i));
            }

            if (currentPage < totalPages - 1) {
                System.out.print("Press Enter for next page, or 'q' to quit: ");
                String input = userInput.nextLine().trim();
                if ("q".equalsIgnoreCase(input)) {
                    break;
                }
            }
        }
    }

    private void runFlipDrill() {
        FlipDrillTest drill = new FlipDrillTest(myVocabulary, userInput);
        TestResult result = drill.run();
        ScoreManager.saveScore(knownLanguage, learningLanguage, result);
    }

    private void runGuessQuiz() {
        GuessTheWordTest quiz = new GuessTheWordTest(myVocabulary, userInput);
        TestResult result = quiz.run();
        ScoreManager.saveScore(knownLanguage, learningLanguage, result);
    }

    private void runFillBlankQuiz() {
        FillInBlanksTest quiz = new FillInBlanksTest(myVocabulary, userInput);
        TestResult result = quiz.run();
        ScoreManager.saveScore(knownLanguage, learningLanguage, result);
    }
}

/**
 * Class for flip drill test, showing one side of the word.
 */
class FlipDrillTest extends Test {
    public FlipDrillTest(List<Word> words, Scanner input) {
        super("Flip Drill", words, input);
    }

    @Override
    public TestResult run() {
        if (vocabList.isEmpty()) {
            System.out.println("No words available for drill!");
            return new TestResult(testName, 0, 0);
        }

        System.out.print("Show (1) foreign words or (2) known language first? ");
        boolean showForeign = input.nextLine().trim().equals("1");

        System.out.print("Order: (1) alphabetical or (2) random? ");
        String orderChoice = input.nextLine().trim();

        List<Word> drillWords = new ArrayList<>(vocabList);
        if (orderChoice.equals("2")) {
            Collections.shuffle(drillWords);
        } else {
            drillWords.sort(Comparator.comparing(showForeign ? w -> w.word : w -> w.translation));
        }

        int wordsShown = 0;
        for (Word currentWord : drillWords) {
            String frontSide = showForeign ? currentWord.word : currentWord.translation;
            String backSide = showForeign ? currentWord.translation : currentWord.word;

            System.out.printf("\n%s [%s]\n", frontSide, currentWord.partOfSpeech);
            System.out.print("Press Enter to flip, or 'q' to quit: ");

            if ("q".equalsIgnoreCase(input.nextLine().trim())) {
                break;
            }

            System.out.println("Answer: " + backSide);
            System.out.print("Press Enter to continue, or 'q' to quit: ");

            if ("q".equalsIgnoreCase(input.nextLine().trim())) {
                break;
            }

            wordsShown++;
        }

        return new TestResult(testName, 0, wordsShown);
    }
}

/**
 * Class for guess the word quiz.
 */
class GuessTheWordTest extends Test {
    public GuessTheWordTest(List<Word> words, Scanner input) {
        super("Guess Quiz", words, input);
    }

    @Override
    public TestResult run() {
        if (vocabList.isEmpty()) {
            System.out.println("No words available for quiz!");
            return new TestResult(testName, 0, 0);
        }

        System.out.print("How many questions? ");
        int numQuestions;
        try {
            numQuestions = Integer.parseInt(input.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number!");
            return new TestResult(testName, 0, 0);
        }

        numQuestions = Math.min(numQuestions, vocabList.size());

        System.out.print("Ask in (1) foreign language or (2) known language? ");
        boolean askInForeign = input.nextLine().trim().equals("1");

        // Shuffle and take first N words
        List<Word> quizWords = new ArrayList<>(vocabList);
        Collections.shuffle(quizWords);

        int correctAnswers = 0;

        for (int i = 0; i < numQuestions; i++) {
            Word currentWord = quizWords.get(i);
            String question = askInForeign ? currentWord.word : currentWord.translation;
            String correctAnswer = askInForeign ? currentWord.translation : currentWord.word;

            System.out.printf("\nQuestion %d/%d: Translate '%s' [%s]: ",
                    i + 1, numQuestions, question, currentWord.partOfSpeech);

            String userAnswer = input.nextLine().trim().toLowerCase();

            if ("q".equals(userAnswer)) {
                System.out.println("Quiz stopped early. Final score: 0");
                return new TestResult(testName, 0, numQuestions);
            }

            if (userAnswer.equals(correctAnswer)) {
                correctAnswers++;
                System.out.println("Correct! ");
            } else {
                System.out.println("Wrong. The answer was: " + correctAnswer);
            }
        }

        System.out.printf("\nQuiz complete! You got %d out of %d correct.\n",
                correctAnswers, numQuestions);
        return new TestResult(testName, correctAnswers, numQuestions);
    }
}

/**
 * Class for fill-in-the-blanks quiz.
 */
class FillInBlanksTest extends Test {
    public FillInBlanksTest(List<Word> words, Scanner input) {
        super("Fill Blanks", words, input);
    }

    @Override
    public TestResult run() {
        if (vocabList.isEmpty()) {
            System.out.println("No words available for quiz!");
            return new TestResult(testName, 0, 0);
        }

        System.out.print("What percentage of letters should be blanked? (0-100): ");
        int blankPercent;
        try {
            blankPercent = Integer.parseInt(input.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid percentage!");
            return new TestResult(testName, 0, 0);
        }

        blankPercent = Math.max(0, Math.min(100, blankPercent));

        System.out.print("How many questions? ");
        int numQuestions;
        try {
            numQuestions = Integer.parseInt(input.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number!");
            return new TestResult(testName, 0, 0);
        }

        numQuestions = Math.min(numQuestions, vocabList.size());
        Collections.shuffle(vocabList);

        Random random = new Random();
        int correctAnswers = 0;

        for (int i = 0; i < numQuestions; i++) {
            Word currentWord = vocabList.get(i);
            String originalWord = currentWord.word;
            String blankedWord = createBlankedWord(originalWord, blankPercent, random);

            System.out.printf("\nQuestion %d/%d: Fill in the blanks: %s\n",
                    i + 1, numQuestions, blankedWord);
            System.out.print("Your answer: ");

            String userAnswer = input.nextLine().trim().toLowerCase();

            if ("q".equals(userAnswer)) {
                System.out.println("Quiz stopped early. Final score: 0");
                return new TestResult(testName, 0, numQuestions);
            }

            if (userAnswer.equals(originalWord)) {
                correctAnswers++;
                System.out.println("Correct! ");
            } else {
                System.out.println("Wrong. The answer was: " + originalWord);
            }
        }

        System.out.printf("\nQuiz complete! You got %d out of %d correct.\n",
                correctAnswers, numQuestions);
        return new TestResult(testName, correctAnswers, numQuestions);
    }

    /**
     * Replace letters with underscores based on percentage.
     */
    private String createBlankedWord(String word, int percentage, Random rng) {
        char[] letters = word.toCharArray();
        int lettersToBlank = Math.round(letters.length * percentage / 100.0f);

        Set<Integer> blankPositions = new HashSet<>();
        while (blankPositions.size() < lettersToBlank && blankPositions.size() < letters.length) {
            blankPositions.add(rng.nextInt(letters.length));
        }

        for (Integer pos : blankPositions) {
            letters[pos] = '_';
        }

        return new String(letters);
    }
}


/**
 * Represents a vocabulary word with its translation and part of speech.
 */
class Word {
    public String word;           // The foreign word
    public String translation;    // Translation in known language
    public String partOfSpeech;   // Part of speech

    public Word(String foreignWord, String meaning, String pos) {
        this.word = foreignWord;
        this.translation = meaning;
        this.partOfSpeech = pos;
    }

    // Two words are equal if they share the same word and part of speech.
    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof Word)) {
            return false;
        }

        Word otherWord = (Word) other;
        return this.word.equals(otherWord.word) &&
                this.partOfSpeech.equals(otherWord.partOfSpeech);
    }

    @Override
    public int hashCode() {
        return word.hashCode() + partOfSpeech.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s [%s] -> %s",
                word, partOfSpeech, translation);
    }
}