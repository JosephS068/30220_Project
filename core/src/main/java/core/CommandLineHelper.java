package core;

import java.io.Console;

public class CommandLineHelper {
    public static Console console = System.console();

    // Prompts user for yes/no input returns true for yes, false for no
    public static boolean responseYesNo() {
        String response = "";
        do {
            response = console.readLine();
            if (!response.equals("y") && !response.equals("n")) {
                System.out.println("please enter in \'y\' or \'n\'");
            }
        } while (!response.equals("y") && !response.equals("n"));

        return response.equals("y");
    }

    public static String noNullInput() {
        String response = console.readLine();
        if (response == null) {
            System.out.println("You must enter in something");
            response = noNullInput();
        }
        return response;
    }

    public static String getUsername() {
        System.out.println("Please enter in a username");
        String username = CommandLineHelper.noNullInput();
        if (username.contains("[") || username.contains("]")) {
            System.out.println("You cannot have \'[\' or \']\' in your username, these characters are reserved for bots");
            username = getUsername();
        } else if (username.length() < 3) {
            System.out.println("Your username must have at least 3 letters");
            username = getUsername();
        }
        return username;
    }

    // Currently has the same funcationality as getUsername, but is seperate incase server names have different constraints
    public static String getServerName() {
        System.out.println("What is the name of this server?");
        String username = CommandLineHelper.noNullInput();
        if (username.contains("[") || username.contains("]")) {
            System.out
                    .println("You cannot have \'[\' or \']\' in the server name, these characters are reserved for bots");
            username = getUsername();
        } else if (username.length() < 3) {
            System.out.println("Your server name must have at least 3 letters");
            username = getUsername();
        }
        return username;
    }
}