import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Scanner;

public class Biometrics {
    // Statics that are used in a few places
    public static MessageDigest HASHER;
    public static Map<String, UserCredential> USER_CREDENTIALS = new java.util.HashMap<>();
    public static Scanner lineReader = new Scanner(System.in);

    public static void main(String[] args) throws NoSuchAlgorithmException {
        // Initializing hasher, current credential, and hasher
        HASHER = MessageDigest.getInstance("MD5");
        UserCredential currentCredential = null;
        int command;

        while (true) {
            // Command prompt and input
            System.out.print("""
                    ------------------------------------------------------------
                     Available Commands:
                     1  - Add New User
                     2  - Print All Users
                     3  - Sign-In
                     4  - View Current Biometric
                     5  - Sign-Out
                     0  - Exit Program
                    ------------------------------------------------------------
                    Enter your choice: """);

            // Filling the prompt
            command = getInt(lineReader);

            // Switch for different actions
            switch (command) {
                case 1: // Adding a new user to the list (does NOT sign in to them)
                    System.out.println("Add New User");
                    createUser();
                    break;
                case 2: // Print all users in the list
                    System.out.println("Print All Users");
                    // Check if the user list is empty
                    if (USER_CREDENTIALS.isEmpty()) {
                        System.out.println("No users found. Please create a user first.");
                        break;
                    }
                    USER_CREDENTIALS.values().forEach(System.out::println);
                    break;
                case 3: // Sign in with an already added user
                    System.out.println("Sign-In");
                    // Check if a user is signed in
                    if (currentCredential != null) {
                        System.out.println("You are already signed in as " + currentCredential.getUsername());
                        break;
                    }
                    // Check if the user list is empty
                    if (USER_CREDENTIALS.isEmpty()) {
                        System.out.println("No users found. Please create a user first.");
                        break;
                    }
                    currentCredential = getCredential();
                    System.out.println("Now signed in as " + currentCredential.getUsername());
                    break;
                case 4: // View the current biometric of the signed-in user
                    System.out.println("View Current Biometric");
                    if (currentCredential != null) {
                        System.out.println("Current Biometric: " + bytesToHex(currentCredential.getBiometricBytes()));
                    } else {
                        System.out.println(
                                "No user is currently signed in.\nPlease sign in first to view your biometric");
                    }
                    break;
                case 5: // Sign out the current user
                    System.out.println("Sign-Out");
                    if (currentCredential != null) {
                        System.out.println("You have been signed out.");
                        currentCredential = null;
                    } else {
                        System.out.println("No user is currently signed in.");
                    }
                    break;

                case 0: // Exit the program
                    System.out.println("Exiting...");
                    lineReader.close();
                    return;
                default: // Catch all for invalid commands
                    System.out.println("Invalid command. Please try again.");
                    break;
            }

            System.out.println("\n\n\n"); // Spacing on the console
        }

    }

    // Get a user credential from the list of users
    // This is done by checking the username and biometric of the user against what
    // the user provides
    private static UserCredential getCredential() {
        String username; // Storage for username
        while (true) {
            // Get username
            username = confirmString(lineReader, "username");
            // Make sure the username is valid
            if (USER_CREDENTIALS.containsKey(username)) {
                System.out.println("Username found.");
                break;
            }
            System.out.println("Username not found. Please try again.");
        }

        // A username exists at this point, so we can check its biometric
        String biometric; // Storage for biometric
        // Get the biometric from the user
        String storedBiometric = bytesToHex(USER_CREDENTIALS.get(username).getBiometricBytes());
        while (true) {
            // Get biometric
            biometric = stepwiseHasher(confirmString(lineReader, "biometric"));
            // Check if the biometric is valid
            // We do this by checking the Levenshtein distance between the two strings
            int dist = levenshteinDistance(biometric, storedBiometric);
            if (dist <= 80) {
                System.out.println("Biometric found.");
                break;
            }
            System.out.println("Biometric invalid found. Please try again.");
        }

        // Return the credential
        return USER_CREDENTIALS.get(username);
    }

    // Create a new user
    private static void createUser() {
        // Getting username and biometric
        String username = confirmString(lineReader, "username");
        String biometric = confirmString(lineReader, "biometric");
        String hexedHashedBiometric = stepwiseHasher(biometric);
        byte[] byteHashedBiometric = hexToByte(hexedHashedBiometric);
        // These could be condensed by putting methods in method calls,
        // but this is more readable

        // Check if the username already exists
        if (USER_CREDENTIALS.containsKey(username)) {
            System.out.println("Username already exists. Please try again.");
            return;
        }

        // Create the user credential and push it to the master list
        USER_CREDENTIALS.put(username, new UserCredential(username, byteHashedBiometric));
    }

    // Compute the Levenshtein between two strings
    public static int levenshteinDistance(String string1, String string2) {
        // Create a 2D array (dynamic programming table) to store intermediate results.
        // dpTable[i][j] represents the minimum edit distance between the first i
        // characters of string1 and the first j characters of string2.
        int[][] dpTable = new int[string1.length() + 1][string2.length() + 1];

        // Initialize the dpTable with base cases.

        // Fill the table row by row and column by column
        for (int i = 0; i <= string1.length(); i++) {
            for (int j = 0; j <= string2.length(); j++) {
                if (i == 0) {
                    // If the first string is empty, the distance is the length of the second string

                    // Meaning there are no matching values, and everything is either insertions or
                    // deletions/modifications
                    dpTable[i][j] = j;
                } else if (j == 0) {
                    // If the second string is empty, the distance is the length of the first string
                    // see previous comment
                    dpTable[i][j] = i;
                } else {
                    // Calculate the cost of substitution.
                    // We do this by checking if the char at the current position are the same.
                    // If they are the same, the cost is 0 (no substitution needed);
                    // If they are different, the cost is 1 (substitution needed).
                    int substitutionCost = string1.charAt(i - 1) == string2.charAt(j - 1) ? 0 : 1;

                    // Compute the minimum cost among:
                    // 1. Deletion (dpTable[i - 1][j] + 1)
                    // 2. Insertion (dpTable[i][j - 1] + 1)
                    // 3. Substitution (dpTable[i - 1][j - 1] + substitutionCost)
                    dpTable[i][j] = Math.min(
                            Math.min(dpTable[i - 1][j] + 1, dpTable[i][j - 1] + 1),
                            dpTable[i - 1][j - 1] + substitutionCost);
                }
            }
        }

        // The final edit distance is stored in the bottom-right cell of the table.
        int distance = dpTable[string1.length()][string2.length()];

        return distance;
    }

    // Compute the stepwise hash of a string
    // This is a really goofy way to do it, but it KINDA works
    public static String stepwiseHasher(String input) {
        String finalString = ""; // Compose a final string to be returned
        for (int i = 0; i < input.length(); i++) {
            // For every character in input, iterate over them
            // Append to final string, the hex equivalent of the hash of the character
            finalString += bytesToHex(doHash(String.valueOf(input.charAt(i))));
            // This isn't a great way to do it, since, technically speaking, it's
            // reversible...
            // But it's a relatively simple way to emulate a biometric hash data
        }
        return finalString;
    }

    // Convert an array of bytes into their hex equivalent, returned as a string
    public static String bytesToHex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    // Convert a string of hex characters into an equivalent byte array
    // Only used once but it's here as the inverse of bytesToHex
    public static byte[] hexToByte(String hex) {
        return HexFormat.of().parseHex(hex);
    }

    // Return the hashed byte array of a string input
    public static byte[] doHash(String input) {
        return HASHER.digest(input.getBytes());
    }

    // Custom object to handle users being created
    // Taken from the Passwords projects for the most part, with some changes
    public static class UserCredential {
        // Storing the username and hashed biometric bytes
        private String username;
        private byte[] biometricBytes;

        // Generic constructor
        public UserCredential(String username, byte[] biometricBytes) {
            this.username = username;
            this.biometricBytes = biometricBytes;
        }

        // Getters for the fields
        public String getUsername() {
            return username;
        }

        public byte[] getBiometricBytes() {
            return biometricBytes;
        }

        // Simple implementation of a toString method
        @Override
        public String toString() {
            return "Username: " + username + "\nBiometric Hash:" + bytesToHex(biometricBytes) + "\n\n";
        }

        // Didn't end up using this method

        // public void toMap() {
        // USER_CREDENTIALS.put(username, this);
        // }

    }

    // Safely get an integer from the user
    public static int getInt(Scanner lineReader) {
        int result = 0;
        boolean valid = false;
        while (!valid) {
            try {
                result = Integer.parseInt(lineReader.nextLine());
                valid = true;
            } catch (NumberFormatException e) {
                System.out.println("\n\n\nPlease enter a valid integer.");
            }
        }
        return result;
    }

    // Get a string from the user, and confirm it
    // Used to confirm usernames and biometrics to ensure they are input correctly
    public static String confirmString(Scanner reader, String prompt) {
        String returnString;

        while (true) {

            System.out.print("Please enter a " + prompt + ": ");
            returnString = reader.nextLine();
            System.out.println("\n\n\n");

            System.out.println("You entered: \"" + returnString + "\" as your " + prompt
                    + ".");
            System.out.println("Are you satisfied with this string?");
            System.out.print("(Y to confirm, any key to retry): ");

            if (reader.nextLine().equalsIgnoreCase("Y")) {
                System.out.println("\n\n\n");
                return returnString;
            }
            System.out.println("\n\n\n");
        }
    }
}