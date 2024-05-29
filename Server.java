package aufgabe4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serves Hangman for telnet (the terminal version of the Hangman game). All
 * methods and fields are static.
 */
public class Server {
	private static final int portnumber = 2345;
	private static ServerSocket serverSocket;

	/** Active connections to players */
	private static Socket[] connections;

	/** A BufferedWriter for each player. */
	private static BufferedWriter[] writers;

	/** A BufferedReader for each player; */
	private static BufferedReader[] readers;

	/** Number of players. */
	private static final int NUM_PLAYERS = 2;

	/** Currently active player (0 - n-1). */
	private static int curPlayer;


	/**
	 * Initializes the game. Loops until solution is found or hangman is dead.
	 * 
	 * @param argv Optional command line arguments.
	 * @throws Exception
	 */
	public static void main(String[] argv) throws Exception {
		// Initialise game and Hangman
		initGame();
		Hangman hangman = new Hangman();

		// Loop until solution is found or hangman is dead.
		while (!hangman.win() && !hangman.dead()) {
			// Inform players of their turn
			writers[curPlayer].write("\nIts your turn to guess\n");
			writers[curPlayer].flush();
			//TODO: Skip values typed while the other players were guessing

			// Process input
			String output;
			String input;
			while (true) {
				writers[curPlayer].write("Your guess: ");
				writers[curPlayer].flush();
				input = readers[curPlayer].readLine();
				if (input.length() > 0 && input.charAt(0) == '!') {
					output = hangman.checkWord(input.substring(1));
					break;
				} else if (input.length() == 1) {
					output = hangman.checkChar(input.charAt(0));
					break;
				} else {
					output = "No character/word entered";
					writers[curPlayer].write("Please enter a character, or !<word> to guess the full word\n");
					writers[curPlayer].flush();
				}
			}
			writeToAllButCur("Player " + (curPlayer + 1) + " guessed " + input + "\n");
			writeToAll(output);
			writeToAll(hangman.getHangman() + "\n\n");

			// Set curPlayer to next player.
			curPlayer = (curPlayer + 1) % NUM_PLAYERS;
		}

		// Inform players about the game result.
		if (hangman.dead()) {
			writeToAll("You lost :(\n");
		} else if (hangman.win()) {
			writeToAll("You won!!!\n");
		}

		// Close connections
		for (Socket socket : connections) {
			socket.close();
		}
		serverSocket.close();
	}

	/**
	 * Initializes sockets until number of players {@link #NUM_PLAYERS
	 * NUM_PLAYERS} is reached.
	 * 
	 * @throws Exception
	 */
	private static void initGame() throws Exception {
		serverSocket = new ServerSocket(portnumber);
		connections = new Socket[NUM_PLAYERS];

		writers = new BufferedWriter[NUM_PLAYERS];
		readers = new BufferedReader[NUM_PLAYERS];

		System.out.println("Server is running on Port " + portnumber + ", waiting for " + NUM_PLAYERS + " players...");

		for (int i = 0; i < NUM_PLAYERS; i++) {
			Socket newConnection = serverSocket.accept();
			connections[i] = newConnection;

			System.out.println("New player connected from " + newConnection.getInetAddress().getHostAddress() + " (" + (i+1) + "/" + NUM_PLAYERS + ")");
			writeToAll("Player connected (" + (i+1) + "/" + NUM_PLAYERS + ")\n");

			writers[i] = new BufferedWriter(new OutputStreamWriter(newConnection.getOutputStream()));
			readers[i] = new BufferedReader(new InputStreamReader(newConnection.getInputStream()));

			writers[i].write("Connection successful, welcome to the Game\n");
			writers[i].write("" + (i+1) + "/" + NUM_PLAYERS + " players found\n");
			writers[i].flush();
		}
		
		// Prevent more connections to be established. Inform players about start of the
		// game.
		writeToAll("Game starting\n");
	}

	/**
	 * Writes the String s to all players.
	 * 
	 * @param s The String to be sent.
	 * @throws Exception
	 */
	private static void writeToAll(String s) throws Exception {
		for (int i = 0; i < NUM_PLAYERS; i++) {
			BufferedWriter writer = writers[i];
			if (writer == null) continue; // Safe access during init
			writer.write(s);
			writer.flush();
		}
	}

	/**
	 * Writes the String s to all players but to the current player.
	 * 
	 * @param s The String to be sent.
	 * @throws Exception
	 */
	private static void writeToAllButCur(String s) throws Exception {
		for (int i = 0; i < NUM_PLAYERS; i++) {
			BufferedWriter writer = writers[i];
			if (i == curPlayer || writer == null) { // Safe access during init (unused)
				continue;
			}
			writer.write(s);
			writer.flush();
		}
	}
}
