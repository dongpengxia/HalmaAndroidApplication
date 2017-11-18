//The Space class has a row and column which provide the row and column
//numbers of the space on the board. The color attribute tells what color
//the space is ('r' for red, 'b' for black, ' ' for no color).
//Dongpeng Xia

package dongpengxia.halma;

//Space
public class Space
{
    int row, column;  //index in the array in Board
    char color; //'r' for red, 'b' for black, ' ' for unfilled space

    //constructor for empty space with row (r) and column (c) location in the Board
    public Space( int r, int c)
    {
        row = r;
        column = c;
        color = ' ';

    }//end Space(int, int)

    //returns true if and only if space is empty
    public boolean empty()
    {
        return color == ' ';

    }//end empty()

    //returns true if and only if Space sp is adjacent to current space
    public boolean adjacent(Space sp)
    {
        return (Math.abs(sp.row - row) <= 1 && Math.abs(sp.column - column) <= 1);

    }//end adjacent(Space)

    //setters and getters
    public char getColor() { return color; }
    public int getRow() { return row; }
    public int getColumn() { return column; }
    public void setColor( char c ) { color = c; }
    private void setRow( int r ) { row = r; }
    private void setColumn( int c ) {column = c; }

}//end Space