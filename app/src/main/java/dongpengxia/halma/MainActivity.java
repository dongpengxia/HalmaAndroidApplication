package dongpengxia.halma;

//Halma consists of two Players and a Board.
//A player wins when all of their pieces move to the opposite corner of the board.
//Players take turns moving one piece at a time. On each turn, a piece may move to
//an empty adjacent square or (exclusive or) 'skip' over a sequence of pieces sequentially,
//skipping over one adjacent piece on each 'skip'.

//The program has theBoard, which is a 2D array representation of the Halma board, and boardButtons,
//which is the UI representation of theBoard.

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

//main
public class MainActivity extends AppCompatActivity
{
    boolean jumped;             //jumped is true when a piece has already been hopped this turn, false otherwise
    boolean keepGoing = true;   //keepGoing is used in the while loop in the reader thread
    Player[] players;           //two players, 'r' is player[0] (red), 'b' is player[1] (black)
    char myColor = ' ';         //stores the color of this player
    Board theBoard;             //current board
    Space currentSpace;         //pointer to space that contains the piece that will be moved in next button tap
    Space startSpace;           //pointer to space that was starting space in currentTurn
    boolean firstStep;          //true if piece in currentSpace has not been moved in the current turn, false if piece in currentSpace has already moved at least once

    //Android GUI
    Button boardButtons[][];    //GUI button board version of theBoard
    TextView infoDisplay;       //textual display for which color has current turn
    Button switchTurns;         //button to finish a turn after hopping over a piece
    Button help;                //info button
    Button giveUp;              //button to forfeit game
    Button quitGame;            //button to close application
    Button close;               //closes dialog
    Context c = this;           //context for dialog
    final String rules = "The objective is to move pieces from one " +
                        "corner of the board to the opposite corner. " +
                        "Players take turns moving one piece at a time. " +
                        "A piece can be moved one space in any " +
                        "direction or may “hop” over a series of " +
                        "adjacent pieces for as many jumps as possible." +
                        "If you hop pieces, end your turn with the Finish Turn button.";

    //Server communications
    public static String machineName = "10.0.2.2";  //default ip for localhost of computer (not virtual device)
    private int portNumber = 11014; //socket number to connect to
    private Socket sock;        //socket connection to server
    Read reader;                //listens for communications from server
    PrintWriter pout;           //write communications to server
    BufferedReader bin;         //reads communications from server

    //onCreate == main method
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //default setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //at start of game, first piece selection has not been made yet
        firstStep = false;

        //no pieces have been hopped yet
        jumped = false;

        //temporary names for players
        String name1 = "Player 1";
        String name2 = "Player 2";

        //initialize two Players
        players = new Player[2];
        players[0] = new Player('r', name1);
        players[1] = new Player('b', name2);

        //initialize Board
        theBoard = new Board();

        //initialize GUI Board Buttons
        boardButtons = new Button[theBoard.getBoardSize()][theBoard.getBoardSize()];
        //Assumption: board buttons are numbered sequentially by row-major order from 0 through ((theBoard.boardSize()^2)-1)
        for(int row = 0; row < theBoard.getBoardSize(); row++)
        {
            for(int col = 0; col < theBoard.getBoardSize(); col++)
            {
                //initialize buttons
                String buttonID = "button" + (row * theBoard.getBoardSize() + col);
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                boardButtons[row][col] = (Button) findViewById(resID);

                //set red, black, and blank spaces
                if (theBoard.getTheSpaces()[row][col].getColor() == 'r')
                {
                    boardButtons[row][col].setBackgroundResource(R.drawable.red);
                    boardButtons[row][col].setTag("Red");
                }
                else if (theBoard.getTheSpaces()[row][col].getColor() == 'b')
                {
                    boardButtons[row][col].setBackgroundResource(R.drawable.black);
                    boardButtons[row][col].setTag("Black");
                }
                else if (theBoard.getTheSpaces()[row][col].getColor() == ' ')
                {
                    boardButtons[row][col].setBackgroundResource(R.drawable.empty);
                    boardButtons[row][col].setTag("Blank");
                }

                //add click listener to GUI button spaces
                boardButtons[row][col].setOnClickListener(new ButtonClickListener(row, col));

            }//end for loop
        }//end for loop

        //Text on Screen (whose turn it is)
        infoDisplay = (TextView) findViewById(R.id.infoDisplay);
        updateInfoDisplay();

        //switchTurns Button
        switchTurns = (Button) findViewById(R.id.switchTurn);
        switchTurns.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(theBoard.currentTurn() == myColor)
                {
                    //de-select last touched piece
                    if(currentSpace != null)
                    {
                        updateButton(currentSpace);
                    }

                    //switch turns on local board
                    safeSwitchTurns();
                    //send communication to opponent that turn is finished
                    Thread swtch = new Thread(new Write("SWITCH"));
                    swtch.start();
                }
            }//end onClick(View)
        });

        //help button creates a dialog to display the rules of Halma
        help = (Button) findViewById(R.id.help);
        help.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                final Dialog d = new Dialog(c);
                d.setContentView(R.layout.customdialog);
                d.setTitle("Rules");
                TextView t = (TextView) d.findViewById(R.id.text);
                t.setText(rules);
                close = (Button) d.findViewById(R.id.closeDialog);
                close.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });
                d.show();
            }//end onClick(View)
        });

        //give up button allows the player to forfeit the game
        giveUp = (Button) findViewById(R.id.giveUp);
        giveUp.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                resetGame();
                Thread giveUp = new Thread(new Write("forfeit"));
                giveUp.start();
            }//end onClick(View)
        });

        //quit buttons lets player close the application
        quitGame = (Button) findViewById(R.id.quitGame);
        quitGame.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Thread quit = new Thread(new Write("quit"));
                quit.start();

                //socket will be closed
                keepGoing = false;

                //close socket
                try
                {
                    Thread.sleep(1000);
                    sock.close();
                }
                catch(Exception e)
                {
                    System.out.println("Caught exception: " + e);
                }

                finish();
                System.exit(0);
            }//end onClick(View)
        });

        //add click listener to background so user can deselect a piece
        LinearLayout rlayout = (LinearLayout) findViewById(R.id.mainlayout);
        rlayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //make sure a hop/move has not been made already
                if(!jumped)
                {
                    System.out.println("Deselected piece");
                    if(currentSpace != null)
                    {
                        //de-select piece on GUI
                        updateButton(currentSpace);
                    }
                    //de-select piece in game
                    currentSpace = null;
                }

            }//end onClick(View)
        });

        //start reader to connect to server and listen for communications
        try
        {
            reader = new Read(); //a thread
            reader.start();

        }//end try
        catch ( Exception e )
        {
            System.out.println("ERROR in reader: " + e);

        }//end catch

    }//end onCreate

    //update the local board to switch turns after the opponent's board has already switched turns and sent the switch turns communication
    public void safeSwitchTurns()
    {
        //switch to UI thread if function call initiated by communication from server
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //new turn, no pieces have been hopped over yet
                jumped = false;

                //switch to next player
                theBoard.switchTurns();

                //reset startSpace and currentSpace
                startSpace = null;
                currentSpace = null;

                //update current turn display
                updateInfoDisplay();
            }//end run
        });
    }//end safeSwitchTurns()

    //updates a board button to reflect an updated space in theBoard
    public void updateButton(final Space sp)
    {
        //switch to UI thread if function call initiated by communication from server
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //avoid null-pointer exceptions
                if(sp != null)
                {
                    int row = sp.getRow();
                    int col = sp.getColumn();
                    char color = theBoard.getTheSpaces()[row][col].getColor();

                    //red
                    if (color == 'r') {
                        boardButtons[row][col].setBackgroundResource(R.drawable.red);
                        boardButtons[row][col].setTag("Red");
                    }
                    //black
                    else if (color == 'b') {
                        boardButtons[row][col].setBackgroundResource(R.drawable.black);
                        boardButtons[row][col].setTag("Black");
                    }
                    //blank
                    else if (color == ' ') {
                        boardButtons[row][col].setBackgroundResource(R.drawable.empty);
                        boardButtons[row][col].setTag("Blank");
                    }
                }
            }//end run()
        });
    }//end updateButton(Space)

    //updateButtonBoard updates all the spaces on the GUI button board to reflect theBoard[][]
    public void updateButtonBoard()
    {
        //switch to UI thread if function call initiated by communication from server
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int row = 0; row < theBoard.getBoardSize(); row++) {
                    for (int col = 0; col < theBoard.getBoardSize(); col++) {
                        updateButton(theBoard.getTheSpaces()[row][col]);
                    }
                }
            }
        });
    }//end updateButtonBoard()

    //ButtonClickListener class listens for button presses (user attempts to move pieces)
    public class ButtonClickListener implements View.OnClickListener
    {
        //row and column of space clicked (0 through theBoard.getBoardSize()-1)
        int row;
        int column;

        //constructor ButtonClickListener(int, int)
        public ButtonClickListener(int r, int c)
        {
            row = r;
            column = c;

        }//end ButtonClickListener(int, int)

        //onClick handles moves when a GUI button is clicked
        public void onClick(View view) {
            //find space that was clicked
            Space sp = theBoard.getTheSpaces()[row][column];

            if (theBoard.currentTurn() == myColor)
            {
                //attempting to move a piece to an empty square
                if (sp.getColor() == ' ' && currentSpace != null)
                {
                    //move piece to adjacent space, then switch turns
                    if (firstStep && currentSpace.adjacent(sp))
                    {
                        //space is blank, see if we can move a piece
                        if (theBoard.movePiece(currentSpace, sp))
                        {
                            //update GUI
                            updateButton(currentSpace);
                            updateButton(sp);

                            //send communication of move to other client/server
                            //turn is automatically switched because move was made to adjacent square
                            Thread mv = new Thread( new Write("" + currentSpace.row + " " + currentSpace.column + " " + sp.row + " " + sp.column + " switch"));
                            mv.start();
                            try {
                                Thread.sleep(500);
                            }
                            catch(Exception e)
                            {}
                            currentSpace = sp;
                            firstStep = false;
                        }
                        //switch turns
                        safeSwitchTurns();
                    }
                    else if (!currentSpace.adjacent(sp))
                    {
                        //space is blank, see if we can move a piece (by skipping an adjacent piece)
                        if (theBoard.movePiece(currentSpace, sp))
                        {
                            //update GUI
                            updateButton(currentSpace);
                            updateButton(sp);
                            setSelected(sp);

                            jumped = true;

                            //communication of move to other client/server
                            Thread mv = new Thread( new Write(""+currentSpace.row + " " + currentSpace.column + " " + sp.row + " " + sp.column));
                            mv.start();
                            try {
                                Thread.sleep(500);
                            }
                            catch(Exception e)
                            {}

                            //reset values
                            currentSpace = sp;
                            firstStep = false;
                        }
                    }
                }
                else if (sp.getColor() == theBoard.currentTurn() && (currentSpace == null || (currentSpace != null && currentSpace.getColor() != theBoard.currentTurn()))) //last check is to make sure its the selection step
                {
                    //selected a piece to be moved in the next click
                    setSelected(sp);
                    startSpace = sp;
                    currentSpace = sp;
                    firstStep = true;
                }

                //check for winner
                checkWinner();
            }
        }//end onClick(View)
    }//end ButtonClickListener()

    //this class reads from the input stream from server (receives messages from the other user)
    public class Read extends Thread
    {
        @Override
        public void run()
        {
            //this try block handles initial connection to server
            try
            {
                //this user plays the role of CLIENT
                //ClIENT calls SERVER when connecting
                System.out.println("CLIENT: Connecting to machine " + machineName + " port " + portNumber);

                //connect to socket
                sock = new Socket(machineName, portNumber);

                //after a successful connection, print a line confirming successful connection
                System.out.println("CLIENT: made successesful connection to server\n");

                //set up input stream to get communications from server
                InputStream in = sock.getInputStream();
                bin = new BufferedReader( new InputStreamReader(in) );

                //set up the output stream to send communications to server
                pout = new PrintWriter(sock.getOutputStream(), true);

                setTurn(); //determines the color of the player (and whether they go first or second)

            }//end try
            catch (IOException ioException)
            {
                System.out.println("ERROR in Read: " +ioException);

            }//end catch
            catch(Exception e)
            {
                System.out.println("ERROR: " + e);

            }//end catch

            //this try block handles further connections to server
            try
            {
                while(keepGoing)
                {
                    String readMe = bin.readLine();
                    if(readMe == null)
                    {
                        System.out.println("Empty message was read from server.");
                    }
                    else
                    {
                        System.out.println("READ from server: " + readMe);

                        //parse the input
                        StringTokenizer st = new StringTokenizer( readMe );

                        //turn complete message
                        if(readMe.equals("SWITCH"))
                        {
                            System.out.println("Switching turns based on switch message from server.");
                            safeSwitchTurns();
                        }
                        //other user disconnected message
                        else if(readMe.equals("quit"))
                        {
                            System.out.println("Other player quit");
                            keepGoing = false;

                            quitDialog();
                        }
                        //other user gave up message
                        else if(readMe.equals("forfeit"))
                        {
                            System.out.println("Other player forfeited");
                            forfeitDialog();
                        }
                        else
                        {
                            //handle a move

                            //get start space row and column
                            int row = Integer.parseInt(st.nextToken());
                            int column = Integer.parseInt(st.nextToken());

                            //Diagnostic Output
                            System.out.println("Start row: " + row);
                            System.out.println("Start col: " + column);

                            //get start space
                            Space currentSpace = theBoard.getTheSpaces()[row][column];

                            //get end space row and column
                            row = Integer.parseInt(st.nextToken());
                            column = Integer.parseInt(st.nextToken());

                            //Diagnostic Output
                            System.out.println("End row: " + row);
                            System.out.println("End col: " + column);

                            //get end space
                            Space sp = theBoard.getTheSpaces()[row][column];

                            if (theBoard.movePiece(currentSpace, sp))
                            {
                                //update GUI based on move communicated from server
                                updateButton(currentSpace);
                                Thread.sleep(500);
                                updateButton(sp);
                                Thread.sleep(500);
                                if (st.hasMoreTokens())
                                {
                                    if (st.nextToken().equals("switch"))
                                    {
                                        safeSwitchTurns();
                                    }
                                }

                                //check for winner
                                checkWinner();
                            }
                        }//end else
                    }//end else
                }//end while
            }//end try
            catch(Exception e)
            {
                System.out.println("Exception in Read: " + e);
            }//end catch()
        }//end run()
    }//end Read

    //highlights the piece that is selected
    public void setSelected(Space sp)
    {
        int row = sp.getRow();
        int col = sp.getColumn();

        if(sp.getColor() == 'r')
        {
            //set to red piece selected
            boardButtons[row][col].setBackgroundResource(R.drawable.redselect);
        }
        else if(sp.getColor() == 'b')
        {
            //set to black piece selected
            boardButtons[row][col].setBackgroundResource(R.drawable.blackselect);
        }
    } //end setSelected

    //checkWinner checks if there is a winner after a turn ends
    public void checkWinner()
    {
        //check for winner
        char winner = theBoard.winner();
        if(winner != ' ')
        {
            //pop-up message when a player wins or loses
            winDialog(winner);
            //reset the game
            resetGame();
        }
    }//end checkWinner

    //winDialog opens a dialog box telling the user if they won or lost when the game ends
    public void winDialog(final char winner)
    {
        //switch to UI thread if function call initiated by communication from server
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String winnerMsg = "";
                if(winner != ' ')
                {
                    if (winner == myColor)
                    {
                        winnerMsg = "You Won!";
                    }
                    else
                    {
                        winnerMsg = "You Lost";
                    }

                    final Dialog winDialog = new Dialog(c);
                    winDialog.setContentView(R.layout.winnerdialog);
                    winDialog.setTitle("Winner");

                    TextView text = (TextView) winDialog.findViewById(R.id.winner);
                    text.setText(winnerMsg);

                    Button ng = (Button) winDialog.findViewById(R.id.ngDialog);
                    ng.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            winDialog.dismiss();
                        }
                    });
                    winDialog.show();
                }
            }
        });
    }//end winDialog(char)

    //other player disconnected, send player a message about situation
    public void quitDialog()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //open dialog box
                String q = "The other player has disconnected, please restart the app";
                final Dialog quitDialog = new Dialog(c);
                quitDialog.setContentView(R.layout.quitdialog);
                quitDialog.setTitle("Opponent Has Quit");

                TextView text = (TextView) quitDialog.findViewById(R.id.quit);
                text.setText(q);

                Button ng = (Button) quitDialog.findViewById(R.id.okbutton);
                ng.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {

                        keepGoing = false;

                        //close socket
                        try
                        {
                            Thread.sleep(1000);
                            sock.close();
                        }
                        catch(Exception e)
                        {
                            System.out.println("Caught exception: " + e);
                        }

                        //exit application
                        finish();
                        System.exit(0);
                    }
                });
                quitDialog.show();
            }
        });
    } //end quitDialog()

    //other player gave up, send player a message about win or loss
    public void forfeitDialog()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //open dialog box
                String f = "YOU WIN! The other player has given up, please start a new game";
                final Dialog forfeitDialog = new Dialog(c);
                forfeitDialog.setContentView(R.layout.forfeitdialog);
                forfeitDialog.setTitle("YOU WIN");

                TextView text = (TextView) forfeitDialog.findViewById(R.id.forfeit);
                text.setText(f);

                Button ng = (Button) forfeitDialog.findViewById(R.id.restart);
                ng.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        resetGame();
                        forfeitDialog.dismiss();
                    }
                });
                forfeitDialog.show();
            }
        });
    } //end forfeitDialog()

    //reset the game (board and GUI)
    public void resetGame()
    {
        //reset the game
        theBoard = new Board();
        updateButtonBoard(); //update GUI
        currentSpace = null;
        startSpace = null;
        firstStep = false;
        //update current turn display
        updateInfoDisplay();

    } //end resetGame

    //setTurn sets the starting color and turn of user
    //only to be used in the beginning when you are expecting a message from the server
    //("black" (1st connection), "red" (2nd connection))
    public void setTurn()
    {
        String readMe = "";
        try
        {
            System.out.println("Attempting to read line in setTurn()");
            readMe = bin.readLine();
            System.out.println("Read Line: " + readMe);
            if (readMe != null)
            {
                if(readMe.equals("black"))
                {
                    myColor = 'b';
                }
                else if(readMe.equals("red"))
                {
                    myColor = 'r';
                }
            }
            System.out.println("myColor: " + myColor);

            updateInfoDisplay();

        }//end try
        catch(Exception e)
        {
            System.out.println("Exception in setTurn(): " + e);
        }//end catch

    }//end setTurn()

    //update infoDisplay to show current color and whether it's a player's turn or not
    public void updateInfoDisplay()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (theBoard.currentTurn() == 'r' && theBoard.currentTurn() == myColor)
                {
                    infoDisplay.setText("Your (Red's) Turn");
                }
                else if (theBoard.currentTurn() == 'r' && theBoard.currentTurn() != myColor)
                {
                    infoDisplay.setText("Opponent's (Red's) Turn");
                }
                else if(theBoard.currentTurn() == 'b' && theBoard.currentTurn() == myColor)
                {
                    infoDisplay.setText("Your (Black's) Turn");
                }
                else if(theBoard.currentTurn() == 'b' && theBoard.currentTurn() != myColor)
                {
                    infoDisplay.setText("Opponent's (Black's) Turn");
                }
            }
        });
    }//end updateInfoDisplay

    //thread for sending messages to server
    public class Write implements Runnable
    {
        //message to write
        String msg;

        public Write(String str)
        {
            msg = str;

        }//end Write(String)

        //send message to server
        public void run()
        {
            try
            {
                pout.println( msg );
                pout.flush();
            }
            catch(Exception e)
            {
                System.out.println("Write error to server: " + e);
            }
        }//end run()

    }//end Write

}//end MainActivity