import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Project: HW 5
 * Purpose Details: javaBruteForceFreqAnalysis using Arabic
 * Course: IST 242
 * Author: Matthew Sulpizio
 * Date Developed: 4/10/2026
 * Last Date Changed: 4/14/2026
 * Rev:
 */

public class HW5_5_Sulpizio {

    /**
     * The Arabic alphabet used for Caesar cipher shifting.
     */
    private static final String ARABIC_ALPHABET = "ابتثجحخدذرزسشصضطظعغفقكلمنهوي";

    /**
     * The relative frequency percentages for Arabic letters.
     * The order matches the ARABIC_ALPHABET string.
     */
    private static final double[] ARABIC_FREQUENCIES = {
            11.6, // ا
            4.8,  // ب
            3.7,  // ت
            1.1,  // ث
            2.8,  // ج
            2.6,  // ح
            1.1,  // خ
            3.5,  // د
            1.0,  // ذ
            4.7,  // ر
            0.9,  // ز
            6.5,  // س
            3.0,  // ش
            2.9,  // ص
            1.5,  // ض
            1.7,  // ط
            0.7,  // ظ
            3.9,  // ع
            1.0,  // غ
            3.0,  // ف
            2.7,  // ق
            3.6,  // ك
            5.3,  // ل
            3.1,  // م
            7.2,  // ن
            2.5,  // ه
            6.0,  // و
            6.7   // ي
    };

    /**
     * The main method for program execution.
     *
     * @param args The command-line arguments.
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== Arabic Translation + Caesar Cipher + Frequency Analysis ===");
            System.out.println();

            System.out.print("Enter the API Key: ");
            String apiKey = scanner.nextLine().trim();

            System.out.print("Enter the plaintext: ");
            String plaintext = scanner.nextLine();

            System.out.print("Enter the Caesar cipher shift key (0-27): ");
            int shiftKey = Integer.parseInt(scanner.nextLine().trim());

            if (shiftKey < 0 || shiftKey >= ARABIC_ALPHABET.length()) {
                System.out.println("Invalid shift key. Please enter a value from 0 to 27.");
                return;
            }

            System.out.println();

            String arabicText = translateToArabic(plaintext, apiKey);
            System.out.println("Arabic translation:");
            System.out.println(arabicText);
            System.out.println();

            String encryptedText = encryptArabic(arabicText, shiftKey);
            System.out.println("Encrypted Arabic text:");
            System.out.println(encryptedText);
            System.out.println();

            Result result = frequencyAnalysis(encryptedText);
            System.out.println("Best shift detected: " + result.getShift());
            System.out.println("Best decrypted guess:");
            System.out.println(result.getText());

        } catch (NumberFormatException e) {
            System.out.println("Program error: Shift key must be a whole number.");
        } catch (Exception e) {
            System.out.println("Program error: " + e.getMessage());
        }
    }

    /**
     * Translates English plaintext to Arabic using the Google Translate API.
     *
     * @param text The plaintext message entered by the user.
     * @param apiKey The Google API key entered by the user.
     * @return The Arabic translated text.
     * @throws Exception If the API request fails or the response cannot be parsed.
     */
    public static String translateToArabic(String text, String apiKey) throws Exception {
        String urlStr = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        String jsonInput = "{"
                + "\"q\":\"" + escapeJson(text) + "\","
                + "\"target\":\"ar\""
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();

        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );
        } else {
            reader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)
            );
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception("Google Translate API request failed: " + response);
        }

        String translated = extractTranslatedText(response.toString());
        if (translated == null || translated.isEmpty()) {
            throw new Exception("Could not extract translated text from API response.");
        }

        return decodeBasicEntities(translated);
    }

    /**
     * Extracts the translated text from the JSON response.
     *
     * @param json The raw JSON response.
     * @return The extracted translated text, or null if not found.
     */
    public static String extractTranslatedText(String json) {
        Pattern pattern = Pattern.compile("\"translatedText\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Escapes quotation marks and backslashes for JSON safety.
     *
     * @param text The input text.
     * @return The escaped text.
     */
    public static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Decodes a few basic encoded entities that may appear in the API response.
     *
     * @param text The encoded text.
     * @return The decoded text.
     */
    public static String decodeBasicEntities(String text) {
        return text.replace("&#39;", "'")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    /**
     * Encrypts Arabic text using a Caesar cipher.
     *
     * @param text The Arabic text to encrypt.
     * @param shift The shift amount.
     * @return The encrypted Arabic text.
     */
    public static String encryptArabic(String text, int shift) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            int index = ARABIC_ALPHABET.indexOf(c);

            if (index != -1) {
                int newIndex = (index + shift) % ARABIC_ALPHABET.length();
                result.append(ARABIC_ALPHABET.charAt(newIndex));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Decrypts Arabic text using a Caesar cipher.
     *
     * @param text The Arabic text to decrypt.
     * @param shift The shift amount.
     * @return The decrypted Arabic text.
     */
    public static String decryptArabic(String text, int shift) {
        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            int index = ARABIC_ALPHABET.indexOf(c);

            if (index != -1) {
                int newIndex = (index - shift + ARABIC_ALPHABET.length()) % ARABIC_ALPHABET.length();
                result.append(ARABIC_ALPHABET.charAt(newIndex));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Performs brute-force frequency analysis on Arabic ciphertext.
     *
     * @param cipher The encrypted Arabic text.
     * @return The best detected shift and decrypted text guess.
     */
    public static Result frequencyAnalysis(String cipher) {
        double bestScore = Double.MAX_VALUE;
        String bestText = "";
        int bestShift = 0;

        for (int shift = 0; shift < ARABIC_ALPHABET.length(); shift++) {
            String decrypted = decryptArabic(cipher, shift);
            double score = chiSquare(decrypted);

            if (score < bestScore) {
                bestScore = score;
                bestText = decrypted;
                bestShift = shift;
            }
        }

        return new Result(bestShift, bestText);
    }

    /**
     * Calculates the chi-square value for a given Arabic text sample.
     *
     * @param text The Arabic text to analyze.
     * @return The chi-square score.
     */
    public static double chiSquare(String text) {
        int[] counts = new int[ARABIC_ALPHABET.length()];
        int total = 0;

        for (char c : text.toCharArray()) {
            int index = ARABIC_ALPHABET.indexOf(c);
            if (index != -1) {
                counts[index]++;
                total++;
            }
        }

        if (total == 0) {
            return Double.MAX_VALUE;
        }

        double chi = 0.0;

        for (int i = 0; i < counts.length; i++) {
            double expected = total * ARABIC_FREQUENCIES[i] / 100.0;
            if (expected > 0) {
                chi += Math.pow(counts[i] - expected, 2) / expected;
            }
        }

        return chi;
    }

    /**
     * Stores the best frequency analysis result.
     */
    static class Result {

        /**
         * The best detected shift value.
         */
        private final int shift;

        /**
         * The best decrypted text guess.
         */
        private final String text;

        /**
         * Creates a Result object.
         *
         * @param shift The best detected shift.
         * @param text The best decrypted text guess.
         */
        Result(int shift, String text) {
            this.shift = shift;
            this.text = text;
        }

        /**
         * Returns the detected shift.
         *
         * @return The best shift.
         */
        public int getShift() {
            return shift;
        }

        /**
         * Returns the best decrypted text guess.
         *
         * @return The best decrypted text.
         */
        public String getText() {
            return text;
        }
    }
}