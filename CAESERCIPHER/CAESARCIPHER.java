import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class CaesarCipher {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");

    private static String transform(String input, int key, boolean encrypt) {
        int shift = Math.floorMod(encrypt ? key : -key, 26);
        StringBuilder result = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                result.append((char) ('A' + (c - 'A' + shift) % 26));
            } else if (c >= 'a' && c <= 'z') {
                result.append((char) ('a' + (c - 'a' + shift) % 26));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static int readChoice(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.println("Invalid choice. Try again.");
        }
    }

    private static int readKey(Scanner scanner) {
        while (true) {
            System.out.print("KEY : ");
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid integer key.");
            }
        }
    }

    private static Path readTextFile(Scanner scanner) {
        while (true) {
            System.out.print("INPUT .txt FILE PATH : ");
            String entered = scanner.nextLine().trim();
            if (entered.length() >= 2 && entered.startsWith("\"") && entered.endsWith("\"")) {
                entered = entered.substring(1, entered.length() - 1);
            }
            try {
                Path path = Paths.get(entered);
                if (entered.toLowerCase(java.util.Locale.ROOT).endsWith(".txt")
                        && Files.isRegularFile(path) && Files.isReadable(path)) return path;
            } catch (InvalidPathException ignored) { }
            System.out.println("Only existing, readable .txt files are accepted.");
        }
    }

    private static Path outputPath(Path input, boolean encrypt) {
        String filename = input.getFileName().toString();
        String base = filename.substring(0, filename.length() - 4);
        return input.resolveSibling(base + (encrypt ? "_ENC.txt" : "_DEC.txt"));
    }

    private static void createGraph(long nanos, Path path) throws IOException {
        int width = 800, height = 360;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString("Caesar Cipher Execution Time", 35, 40);
            double ms = nanos / 1_000_000.0;
            double scaleMax = Math.max(ms * 1.15, 0.001);
            int barWidth = (int) Math.round(600 * ms / scaleMax);
            g.setColor(new Color(40, 115, 190));
            g.fillRoundRect(100, 140, Math.max(1, barWidth), 70, 10, 10);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            g.drawString(String.format(java.util.Locale.US, "Execution: %.6f ms (%d ns)", ms, nanos), 100, 245);
            g.drawString("Elapsed processing time (milliseconds)", 100, 290);
            ImageIO.write(image, "png", path.toFile());
        } finally {
            g.dispose();
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("===== CAESAR CIPHER =====");
            System.out.println("1 : ENCRYPTION");
            System.out.println("2 : DECRYPTION");
            int operation = readChoice(scanner, "CHOOSE : ", 1, 2);
            boolean encrypt = operation == 1;

            System.out.println("\n1 : TEXT");
            System.out.println("2 : FILE");
            int mode = readChoice(scanner, "CHOOSE : ", 1, 2);
            String input;
            Path source = null;
            if (mode == 1) {
                System.out.print("INPUT TEXT : ");
                input = scanner.nextLine();
            } else {
                source = readTextFile(scanner);
                input = Files.readString(source, StandardCharsets.UTF_8);
            }
            int key = readKey(scanner);

            LocalDateTime start = LocalDateTime.now();
            long startNanos = System.nanoTime();
            String result = transform(input, key, encrypt);
            Path output = null;
            if (mode == 2) {
                output = outputPath(source, encrypt);
                Files.writeString(output, result, StandardCharsets.UTF_8);
            }
            long elapsedNanos = System.nanoTime() - startNanos;
            LocalDateTime end = LocalDateTime.now();

            System.out.println("\n===== " + (encrypt ? "ENCRYPTION" : "DECRYPTION") + " RESULT =====");
            if (mode == 1) {
                System.out.println("INPUT TEXT  : " + input);
                System.out.println("OUTPUT TEXT : " + result);
            } else {
                System.out.println("INPUT FILE  : " + source.toAbsolutePath());
                System.out.println("OUTPUT FILE : " + output.toAbsolutePath());
            }
            System.out.println("KEY         : " + key);
            System.out.println("START TIME  : " + start.format(FORMAT));
            System.out.println("END TIME    : " + end.format(FORMAT));
            System.out.printf(java.util.Locale.US, "DIFFERENCE  : %.6f ms (%d ns)%n",
                    elapsedNanos / 1_000_000.0, elapsedNanos);

            Path graph = Paths.get(encrypt ? "encryption_time.png" : "decryption_time.png");
            try {
                createGraph(elapsedNanos, graph);
                System.out.println("GRAPH FILE  : " + graph.toAbsolutePath());
            } catch (IOException e) {
                System.out.println("Could not save graph: " + e.getMessage());
            }
        } catch (IOException e) {
            System.err.println("File error: " + e.getMessage());
        }
    }
}
