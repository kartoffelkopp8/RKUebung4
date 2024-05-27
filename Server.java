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
  // ServerSocket
  private static ServerSocket serverSocket;

  // Client Socket array
  private static Socket[] clientSockets;

  /** A BufferedWriter for each player. */
  private static BufferedWriter[] writers;

  /** A BufferedReader for each player; */
  private static BufferedReader[] readers;

  /** Number of players. */
  private static final int NUM_PLAYERS = 1;

  /** Currently active player (0 - n-1). */
  private static int curPlayer;

  private static final int portnumber = 2345;
  /**
   * Initializes the game. Loops until solution is found or hangman is dead.
   * 
   * @param argv
   *             Optional command line arguments.
   * @throws Exception
   */
  public static void main(String[] argv) throws Exception {
    //initialise game and Hangman
    initGame();
    Hangman hangman = new Hangman();

    while (!hangman.win() && !hangman.dead())// Loop until solution is found or hangman is dead.
    {
      // Inform players and read input.
        writers[curPlayer].write("its your turn to guess \n");
        writers[curPlayer].flush();

        //Process input
        String output;
        String input = readers[curPlayer].readLine();
        if(input.length() > 1){
          output = hangman.checkWord(input);
        }else if(input.length() == 1){
          output = hangman.checkChar(input.charAt(0));
        }else{
          output = "No character/word entered";
          writers[curPlayer].write("Please enter a character");
          writers[curPlayer].flush();
        }
      writeToAllButCur("Player " + (curPlayer + 1) + " guessed " + input
              + "\n " + output);

      //  Process input and inform players.
      writeToAll(hangman.getHangman());
      //  Set curPlayer to next player.
      curPlayer++;
    }

    // Inform players about the game result.
    if(hangman.dead()){
      writeToAll("You lost :(");
    } else if (hangman.win()) {
      writeToAll("You won!!!");
    }
    // Close player sockets.
    for (Socket socket : clientSockets){
      socket.close();
    }
  }

  /**
   * Initializes sockets until number of players {@link #NUM_PLAYERS
   * NUM_PLAYERS} is reached.
   * 
   * @throws Exception
   */
  private static void initGame() throws Exception {

    serverSocket = new ServerSocket(portnumber);
    clientSockets = new Socket[NUM_PLAYERS];

    writers = new BufferedWriter[NUM_PLAYERS];
    readers = new BufferedReader[NUM_PLAYERS];

    curPlayer = 0;

    System.out.println("Server is running on Port " + portnumber + ", waiting for players...");


    while (curPlayer < NUM_PLAYERS)
    {
      Socket clientSocket = serverSocket.accept();
      clientSockets[curPlayer] = clientSocket;

      writers[curPlayer] = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
      readers[curPlayer] = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      writers[curPlayer].write("Connection successful, welcome to the Game \n ");
      writers[curPlayer].flush();

      curPlayer++;
    }
    // Reset current player.
    writeToAll("Game starting\n ");
    curPlayer = 0;

    // Prevent more connections to be established. Inform players about start of the game.

  }

  /**
   * Writes the String s to all players.
   * @param s // The String to be sent.
   * @throws Exception
   */
  private static void writeToAll(String s) throws Exception {
    for (int i = 0; i < NUM_PLAYERS; i++){
      BufferedWriter writer = writers[i];
      writer.write(s);
      writer.flush();
    }
  }

  /**
   * Writes the String s to all players but to the current player.
   * 
   * @param s //The String to be sent.
   * @throws Exception
   */
  private static void writeToAllButCur(String s) throws Exception {
    for (int i = 0; i < NUM_PLAYERS; i++){
      if(i == curPlayer){
        continue;
      }
      BufferedWriter writer = writers[i];
      writer.write(s);
      writer.flush();
    }
  }

}
