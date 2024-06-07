package aufgabe4;

import aufgabe4.Hangman;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Serves Hangman for HTTP (the browser version of the Hangman game). All
 * methods and fields are static.
 */
public final class WebServer {
	private static final int portNumber = 2345;
	private static ServerSocket serverSocket;

	/** HTTP EOL sequence. */
	private static final String EOL = "\r\n";

	/**
	 * Session ID to distinguish between the current and previous game sessions.
	 */
	private static int session;

	/** Session ID cookie of the currently handled request. */
	private static int sessionCookie;

	/** Player number of the currently handled request. */
	private static int playerCookie;

	private static Character letterGuess;
	private static String wordGuess;

	/** Hangman game object. */
	private static Hangman hangman;

	/** Used to save the result message of the previous action. */
	private static String prevMsg;

	/** Number of players. */
	private static final int NUM_PLAYERS = 2;

	/** Currently active player (0 - n-1). */
	private static int curPlayer;

	/**
	 * Indicates whether we are already in-game or still waiting for some
	 * players.
	 */
	private static boolean gameStarted = false;

	/** Indicates whether the game has ended. */
	private static boolean gameEnded = false;

	/**
	 * Initializes the game. Loops until solution is found or hangman is dead.
	 * 
	 * @param argv
	 *            Optional command line arguments.
	 * @throws Exception
	 */
	public static void main(String[] argv) throws Exception {
		// Initialize socket, global variables and hangman.
		serverSocket = new ServerSocket(portNumber);
		hangman = new Hangman();
		session = new Random().nextInt();

		while (true) {
			// Accept client request.
			try (Socket clientSocket = serverSocket.accept()) {
				BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

				if (!gameStarted && !gameEnded) {
					processInitRequest(br, bw);
				} else if (gameStarted && !gameEnded) {
					processGameRequest(br, bw);
				} else {
					processEndRequest(br, bw);
					if (curPlayer == NUM_PLAYERS)
						break;
				}
			}
		}
		serverSocket.close();
	}

	/**
	 * Handles HTTP conversation when game has not yet started. Waits for number
	 * of players {@link #NUM_PLAYERS NUM_PLAYERS} to be present.
	 * 
	 * @param br
	 *            The BufferedReader used to read the request.
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @throws Exception
	 */
	private static void processInitRequest(BufferedReader br, BufferedWriter bw) throws Exception {
		if (processHeaderLines(br, bw) == null) return;

		String cookieLines = "";
		String content = "<HTML><HEAD><META http-equiv=\"refresh\" content=\"2\"><TITLE>Hangman</TITLE></HEAD><BODY>"
				+ "<PRE>[ ] [ ]-[ ]<BR>         |<BR>[ ]     [ ]<BR> |     /<BR>[ ]-[ ]<BR>____________</PRE>"
				+ "Willkommen zu I7Hangman!<BR>Du bist Spieler ";

		if (sessionCookie != session) {
			cookieLines += "Set-Cookie: sessionId=" + session + EOL;
			cookieLines += "Set-Cookie: playerId=" + curPlayer + EOL;
			content += curPlayer;
			System.out.println("Player " + curPlayer + " connected");
			curPlayer++;
		} else {
			content += playerCookie;
		}

		content += ".<BR>Es darf reihum ein Buchstabe geraten werden.<BR>Die Seite lädt automatisch neu.<BR>"
				+ "Warte auf alle Spieler...</BODY></HTML>";

		sendOkResponse(bw, cookieLines, content);

		if (curPlayer == NUM_PLAYERS) {
			gameStarted = true;
			curPlayer = 0;
		}
	}

	/**
	 * Handles HTTP conversation when game is running. Differentiates between
	 * current player and other players.
	 * 
	 * @param br
	 *            The BufferedReader used to read the request.
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @throws Exception
	 */
	private static void processGameRequest(BufferedReader br, BufferedWriter bw) throws Exception {
		if (processHeaderLines(br, bw) == null) return;

		if (sessionCookie == session) {
			// This should be a 403 response
			return;
		}

		// Construct the response message.
		String content = "<HTML><HEAD><TITLE>Hangman</TITLE>";

		if (playerCookie == curPlayer && (letterGuess != null || wordGuess != null)) {
			// Player is current player and form was submitted.
			if (letterGuess != null) {
				System.out.println("Player " + playerCookie + " guessed letter '" + letterGuess + "'");
				prevMsg = hangman.checkCharHtml(letterGuess);
			} else {
				System.out.println("Player " + playerCookie + " guessed '" + wordGuess + "'");
				prevMsg = hangman.checkWordHtml(wordGuess);
			}
			curPlayer = (curPlayer + 1) % NUM_PLAYERS;

			content += "<META http-equiv=\"refresh\" content=\"0;url=/\"></HEAD><BODY>"
					+ "<PRE>[ ] [ ]-[ ]<BR>         |<BR>[ ]     [ ]<BR> |     /<BR>[ ]-[ ]<BR>____________</PRE>"
					+ prevMsg
					+ hangman.getHangmanHtml()
					+ "Spieler "
					+ curPlayer + " ist an der Reihe.";
		} else if (playerCookie == curPlayer) {
			content += "</HEAD><BODY>"
					+ "<PRE>[ ] [ ]-[ ]<BR>         |<BR>[ ]     [ ]<BR> |     /<BR>[ ]-[ ]<BR>____________</PRE>"
					+ prevMsg + hangman.getHangmanHtml()
					+ "Du bist an der Reihe, Spieler " + curPlayer + "!"
					+ "<FORM action=\"/\" method=\"get\">";
			for (char i = 'a'; i <= 'z'; ++i) {
				content += "<INPUT type=\"submit\" name=\"letter\" value=\""
						+ i + "\">";
			}
			content += "</FORM><BR><FORM action=\"/\" method=\"get\">"
					+ "<LABEL>Suchbegriff <INPUT name=\"solution\"></LABEL>"
					+ "<BUTTON>Lösen</BUTTON></FORM>";
		} else {
			content += "<META http-equiv=\"refresh\" content=\"2;url=/\"></HEAD><BODY>"
					+ "<PRE>[ ] [ ]-[ ]<BR>         |<BR>[ ]     [ ]<BR> |     /<BR>[ ]-[ ]<BR>____________</PRE>"
					+ prevMsg
					+ hangman.getHangmanHtml()
					+ "Spieler "
					+ curPlayer + " ist an der Reihe.";
		}
		content += "</BODY></HTML>";

		sendOkResponse(bw, null, content);

		if (hangman.win() || hangman.dead()) {
			gameStarted = false;
			gameEnded = true;
			curPlayer = 0;
		}
	}

	/**
	 * Handles HTTP conversation when game ended.
	 * 
	 * @param br
	 *            The BufferedReader used to read the request.
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @throws Exception
	 */
	private static void processEndRequest(BufferedReader br, BufferedWriter bw) throws Exception {
		if (processHeaderLines(br, bw) == null) return;

		if (sessionCookie == session) {
			// This should be a 403 response
			return;
		}

		String content = "<HTML><HEAD><TITLE>Hangman</TITLE></HEAD><BODY>"
				+ "<PRE>[ ] [ ]-[ ]<BR>         |<BR>[ ]     [ ]<BR> |     /<BR>[ ]-[ ]<BR>____________</PRE>"
				+ prevMsg + hangman.getHangmanHtml();

		if (hangman.win()) {
			content += "You Win :)<BR>You correctly guessed the word " + hangman.getWord();
		}
		if (hangman.dead()) {
			content += "You lose :(<BR>The correct word would have been " + hangman.getWord();
		}

		content += "</BODY></HTML>";

		curPlayer++;
		sendOkResponse(bw, null, content);
	}

	/**
	 * Processes the HTTP request and its header lines.
	 * 
	 * @param br
	 *            The BufferedReader used to read the request.
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @return The request line of the HTTP request if it is a valid game
	 *         related request, otherwise null.
	 * @throws Exception
	 */
	private static String processHeaderLines(BufferedReader br, BufferedWriter bw) throws Exception {
		//TODO: Make request parsing more resilient
	
		// ==== Process request url ====
		String inLine = br.readLine();
		if (inLine == null || inLine.isEmpty()) {
			return null;
		} else if (inLine.contains("/favicon.ico")) {
			sendNotFoundResponse(bw);
			return  null;
		}

		// Crashes if the first line in the request (GET ...) does not consist of three parts, but that will never happen with valid http requests
		String url = inLine.split(" ")[1];
		letterGuess = null;
		wordGuess = null;
		if (url.startsWith("/?letter=")) {
			letterGuess = url.charAt(9);
		} else if (url.startsWith("/?solution=")) {
			wordGuess = url.substring(11);
		}

		// ==== Process cookies ====
		sessionCookie = -1;
		playerCookie = -1;

		// Parse cookies from all header lines
		String line = br.readLine();
		while (line != null && !line.isEmpty()) {
			if (line == "" || line == null) break;
			if (!line.startsWith("Cookie")) {
				line = br.readLine();
				continue;
			}

			String cookies = line.substring(8);
			for (String cookie : cookies.split("; ")) {
				if (cookie.startsWith("sessionId")) {
					sessionCookie = Integer.parseInt(cookie.substring(10));
				} else if (cookie.startsWith("playerId")) {
					playerCookie = Integer.parseInt(cookie.substring(9));
				}
			}
			line = br.readLine();
		}

		return inLine;
	}

	/**
	 * Sends a 404 HTTP response.
	 * 
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @throws Exception
	 */
	private static void sendNotFoundResponse(BufferedWriter bw)
			throws Exception {
		// Construct and send a valid HTTP/1.0 404-response.
		bw.write("HTTP/1.0 404 Not Found" + EOL);
		bw.write("Content-Type: text/html" + EOL + EOL);
		bw.write("<!DOCTYPE html>" + EOL);
		bw.write("<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>404 Not Found</BODY></HTML>");
		bw.flush();
	}

	/**
	 * Sends a HTTP response with cookies (if set) and HTML content.
	 * 
	 * @param bw
	 *            The BufferedWriter used to write the HTTP response.
	 * @param cookieLines
	 *            Optional header lines to set cookies.
	 * @param content
	 *            The actual HTML content to be sent to the browser.
	 * @throws Exception
	 */
	private static void sendOkResponse(BufferedWriter bw, String cookieLines,
			String content) throws Exception {
		// Construct and send a valid HTTP/1.0 200-response with the given
		// cookies (if not null) and the given content.
		bw.write("HTTP/1.0 200 OK" + EOL);
		if (cookieLines != null) {
			bw.write(cookieLines);
		}
		bw.write("Content-Type: text/html" + EOL + EOL);
		bw.write("<!DOCTYPE html>" + EOL);
		bw.write(content);
		bw.flush();
	}
}
