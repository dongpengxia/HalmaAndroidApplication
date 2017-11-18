package dongpengxia.halma;

//Halma consists of two Players and a Board.
//A player wins when all of their pieces move to the opposite corner of the board.
//Players take turns moving one piece at a time. On each turn, a piece may move to
//an empty adjacent square or (exclusive or) 'skip' over a sequence of pieces sequentially,
//skipping over one adjacent piece on each 'skip'.

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{

    Player[] players; //two players, 'r' is player[0] (red), 'b' is player[1] (black)
    Board theBoard;   //current board
    Space currentSpace; //pointer to space that contains the piece that will be moved in next button tap
    Space startSpace; //pointer to space that was starting space in currentTurn
    boolean firstStep;    //true if piece in currentSpace has not been moved in the current turn, false if piece in currentSpace has already moved at least once

    //Android GUI
    Button boardButtons[][]; //GUI button board version of theBoard
    TextView infoDisplay;    //textual display - currently has no purpose
    Button switchTurns;
    Button quitGame;

    //onCreate == main method for Android
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //We need to add a switchTurns listener and move listener to update our game when the opponent sends communications (move a piece, switch turns)
        //if they move a piece, we update our GUI and theBoard
        //if the switch turns, we call the safeSwitchTurns function


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //at start of game, first piece selection has not been made yet
        firstStep = false;

        //temporary names
        String name1 = "Player 1";
        String name2 = "Player 2";

        //initialize two players
        players = new Player[2];
        players[0] = new Player('r', name1);
        players[1] = new Player('b', name2);

        //initialize Board
        theBoard = new Board();

        //initialize GUI Board Buttons
        boardButtons = new Button[theBoard.getBoardSize()][theBoard.getBoardSize()];
        //This relies on the board buttons being numbered sequentially by row-major order from 0 through ((theBoard.boardSize()^2)-1)
        for(int row = 0; row < theBoard.getBoardSize(); row++)
        {
            for(int col = 0; col < theBoard.getBoardSize(); col++)
            {
                //initialize buttons
                String buttonID = "button" + (row * theBoard.getBoardSize() + col);
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                boardButtons[row][col] = (Button) findViewById(resID);

                //set red, black, and blank spaces
                if (theBoard.getTheSpaces()[row][col].getColor() == 'r') {
                    boardButtons[row][col].setText("R");
                    boardButtons[row][col].setTag("Red");
                } else if (theBoard.getTheSpaces()[row][col].getColor() == 'b') {
                    boardButtons[row][col].setText("B");
                    boardButtons[row][col].setTag("Black");
                } else if (theBoard.getTheSpaces()[row][col].getColor() == ' ') {
                    boardButtons[row][col].setText(" ");
                    boardButtons[row][col].setTag("Blank");
                }

                //add click listener to GUI button spaces
                boardButtons[row][col].setOnClickListener(new ButtonClickListener(row, col));

            }
        }

        //Text on Screen
        infoDisplay = (TextView) findViewById(R.id.infoDisplay);
        if(theBoard.currentTurn() == 'r') { infoDisplay.setText("Red's Turn"); }
        else { infoDisplay.setText("Black's Turn"); }

        //switchTurns Button
        switchTurns = (Button) findViewById(R.id.switchTurn);
        switchTurns.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //when someone clicks the switch turns button:

                //switch to next player
                theBoard.switchTurns();

                //update infoDisplay
                if(theBoard.currentTurn() == 'r') { infoDisplay.setText("Red's Turn"); }
                else { infoDisplay.setText("Black's Turn"); }

                //at this point, startSpace is the starting space of the turn, currentSpace is the ending space of the turn

                //Diagnostic Output
                System.out.println("Start Row: " + startSpace.getRow() + " Start Column: " + startSpace.getColumn());
                System.out.println("Dest Row: " + currentSpace.getRow() + " Dest Column: " + currentSpace.getColumn());

                startSpace = null;
                currentSpace = null;

                //send communication to other game that I have finished my turn
                //start listener to other game instance--------------------------------------------------------------------------------------------------------
            }
        });
    }//end onCreate

    //update the local board to switch turns after the opponent's board has already switched turns and sent the switch turns communication
    private void safeSwitchTurns()
    {
        theBoard.switchTurns();
        startSpace = null;
        currentSpace = null;

        //update infoDisplay
        if(theBoard.currentTurn() == 'r') { infoDisplay.setText("Red's Turn"); }
        else { infoDisplay.setText("Black's Turn"); }

    }//end safeSwitchTurns()

    //updates a button to reflect an updated space in theBoard
    private void updateButton(Space sp)
    {
        int row = sp.getRow();
        int col = sp.getColumn();

        char color = theBoard.getTheSpaces()[row][col].getColor();

        //red
        if (color == 'r')
        {
            boardButtons[row][col].setText("R");
            boardButtons[row][col].setTag("Red");
        }
        //black
        else if (color == 'b')
        {
            boardButtons[row][col].setText("B");
            boardButtons[row][col].setTag("Black");
        }
        //blank
        else if (color == ' ')
        {
            boardButtons[row][col].setText(" ");
            boardButtons[row][col].setTag("Blank");
        }

    }//end updateButton(Space)

    //updateButtonBoard updates all the spaces on the GUI button board to reflect theBoard[][]
    private void updateButtonBoard()
    {
        for(int row = 0; row < theBoard.getBoardSize(); row++)
        {
            for(int col = 0; col < theBoard.getBoardSize(); col++)
            {
                updateButton(theBoard.getTheSpaces()[row][col]);
            }
        }

    }//end updateButtonBoard()

    //ButtonClickListener class listens for button presses (user attempts to move pieces)
    private class ButtonClickListener implements View.OnClickListener
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
        public void onClick(View view)
        {
            //find space that was clicked
            Space sp = theBoard.getTheSpaces()[row][column];

            //---------------------------------------------------------------------------------------------------------------------------
            //do not delete this segment of comments
            //we need to add a part so that if the user clicks outside the board, currentSpace is set to null if firstStep is true
            /*
            //check if space exists
            if(sp == null)
            {
                //reset piece to be moved if click was outside board
                if(firstStep)
                {
                    currentSpace = null;
                }
            }
            *///-------------------------------------------------------------------------------------------------------------------------

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

                        //send communication of move to other client/server-------------------------------------------------------------------------------------

                        currentSpace = sp;
                        firstStep = false;
                    }
                    //theBoard.switchTurns();
                    switchTurns.performClick();
                }
                else if (!currentSpace.adjacent(sp))
                {
                    //space is blank, see if we can move a piece (by skipping an adjacent piece)
                    if (theBoard.movePiece(currentSpace, sp))
                    {
                        //update GUI
                        updateButton(currentSpace);
                        updateButton(sp);

                        //send communication of move to other client/server-------------------------------------------------------------------------------------

                        currentSpace = sp;
                        firstStep = false;
                    }
                }
            }
            else if (sp.getColor() == theBoard.currentTurn() && (currentSpace == null || (currentSpace != null && currentSpace.getColor() != theBoard.currentTurn()))) //last check is to make sure its the selection step
            {
                //we've selected a piece to be moved in the next click
                startSpace = sp;
                currentSpace = sp;
                firstStep = true;
            }

            //check for winner
            char winner = theBoard.winner();

            //if there is a winner
            if (winner != ' ')
            {
                String winnerName = "";

                //red wins
                if (winner == 'r')
                {
                    winnerName = players[0].getName();
                    if (winnerName == null || winnerName.equals(""))
                    {
                        winnerName = "Player 1";
                    }
                }
                //black wins
                else if (winner == 'b')
                {
                    winnerName = players[1].getName();
                    if (winnerName == null || winnerName.equals(""))
                    {
                        winnerName = "Player 2";
                    }
                }

                //add a pop-up message when a player wins

                //reset the game
                theBoard = new Board();
                updateButtonBoard(); //update GUI
                currentSpace = null;
                startSpace = null;
                firstStep = false;
            }

        }//end onClick(View)

    }//end ButtonClickListener()

}//end MainActivity