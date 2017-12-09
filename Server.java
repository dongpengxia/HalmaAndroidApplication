//Server sets up the Server socket so that Halma clients can pair up and send messages over the server
//to communicate game actions (move, finish turns, etc).

//import statements
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

//Server class
public class Server
{
    //server socket used for connection
    ServerSocket server;
    
    //socket for connection request from client
    protected Socket sock;
    
    //port number for connection
    private int portNumber = 11014;
    
    //number of clients
    private int counter = 0;
    
    //List of listeners to clients
    private LinkedList<ServerListener> listOfListeners = new LinkedList<ServerListener>();
    
    //listener to last client if total number of clients is odd
    private ServerListener lastOne = null;
    
    //List of pairs (of clients)
    private LinkedList<Pair> pairList = new LinkedList<Pair>();
    
    //output writer to send communications to clients
    BufferedWriter bout;
    
    //main
    public static void main ( String[] args ) throws IOException
    {
        //start new server
        new Server();
    }
    
    //Server constructor
    public Server() throws IOException
    {
        //Open server socket
        //Throws an exception if server socket is already open.
        server = new ServerSocket(portNumber);
        
        //loop to continue reading and writing
        while(true)
        {
            System.out.println("Server waiting for connection requests");
            sock = server.accept(); //blocks until a client calls and a connection is made
            counter++;
            System.out.println("Client # " + counter + " accepted");
            
            //add listener to listen for client-sent messages
            ServerListener sListenerThread = new ServerListener(sock);
            listOfListeners.add(sListenerThread);
            sListenerThread.start();
            
            //odd number of clients
            if(listOfListeners.size() % 2 == 1)
            {
                lastOne = sListenerThread;
                
                //assign black pieces to the first client
                lastOne.write("black");
            }
            //even number of clients
            else
            {
                //pair the two together
                Pair p = new Pair();
                p.first = lastOne;
                p.second = sListenerThread;
                pairList.add(p);
                
                //assign pointers in server listeners to pair they are contained in
                lastOne.p = p;
                sListenerThread.p = p;
                
                //assign red pieces to the first client
                sListenerThread.write("red");
            }
            
        }//end while(true)
    }//end Server()
    
    //Pair is a pair of server listeners
    public class Pair
    {
        public ServerListener first; 	//first client
        public ServerListener second;	//second client
        
        //function to get the opponent
        public ServerListener findOpponent(ServerListener servListen)
        {
            if(servListen == first)
            {
                return second;
            }
            else if (servListen == second)
            {
                return first;
            }
            return null;
            
        }//end findOpponent(servListen)
        
    }//end Pair
    
    //ServerListener listens for a client's outgoing messages to server
    public class ServerListener extends Thread
    {
        public Pair p; //points to pair that this ServerListener belongs to
        public Socket theSocket;
        private BufferedReader bin; //reads input from clients
        private BufferedWriter bout;//writes output to clients
        
        @Override
        public void run()
        {
            //wait for communications from a client and send message to client's opponent
            while(!theSocket.isClosed())
            {
                //wait to receive input
                String line = this.read();
                
                if(line != null)
                {
                    System.out.println("Read line: " + line);
                    
                    //check that client has an opponent
                    if(p.findOpponent(this) != null)
                    {
                        p.findOpponent(this).write(line);
                        System.out.println("Wrote line to opponent: " + line);
                    }
                    else
                    {
                        System.out.println("Opponent does not exist. Message failed to send.");
                    }
                    
                    //if the user has decided to quit, close the socket and
                    //subsequently exit the while loop
                    if(line.equals("quit"))
                    {
                        try
                        {
                            //close the socket
                            theSocket.close();
                            System.out.println("Socket closed");
                        }
                        catch (IOException e)
                        {
                            System.out.println("Caught exception: " + e);
                        }
                    }
                }
            }//end while
            
            //iterator for the list of ServerListeners
            Iterator<ServerListener> it = listOfListeners.iterator();
            boolean foundThis = false;//set to true once this ServerListener is found in the list
            boolean foundOpponent = false;//set to true once opponent ServerListener is found in the list
            //while there is another in list
            while(it.hasNext() && (!foundThis || !foundOpponent))
            {
                ServerListener listener = it.next();
                if(listener.equals(this))
                {
                    it.remove(); //remove this serverListener from the list
                    counter--;
                    foundThis = true;
                }
                else if(listener.equals(p.findOpponent(this)))
                {
                    //close opponent's socket
                    try
                    {
                        p.findOpponent(this).theSocket.close();
                        System.out.println("Closed opponent's socket");
                    }
                    catch(IOException e)
                    {
                        System.out.println("Caught exception: " + e);
                    }
                    
                    it.remove(); //remove the opponent from the list
                    counter--;
                    foundOpponent = true;
                }
            }
        }//end run()
        
        //constructor
        public ServerListener(Socket s)
        {
            try
            {
                //assign socket
                theSocket = s;
                
                //instream and outstreams to clients
                InputStream in = theSocket.getInputStream();
                OutputStream out = theSocket.getOutputStream();
                bin = new BufferedReader( new InputStreamReader(in) );
                bout = new BufferedWriter( new OutputStreamWriter(out) );
                
                System.out.println("Input and Output streams set up for client");
                
            }//end try
            catch(Exception e)
            {
                System.out.println("Exception: " + e);
            }//end catch
            
        }//end ServerListener(Socket)
        
        //read returns message string that was sent from client
        public String read() 
        {	
            String msg = "";
            try
            {
                msg = bin.readLine();
            }
            catch(Exception e)
            {
                System.out.println("Exception: " + e);
            }
            return msg;
            
        }//end read()
        
        //write sends a message to a client
        public void write(String msg)
        {
            try
            {
                bout.write(msg + "\n");
                bout.flush();
                
            }//end try
            catch (Exception e)
            {
                System.out.println("Exception: " + e);
                
            }//end catch
        }//end write(String)
    }//end ServerListener
}//end Server
