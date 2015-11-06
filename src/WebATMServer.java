import java.io.*;
import java.net.*;
import java.util.Date;

public class WebATMServer {

	public static void main(String[] args) {
		
		//int clientNo = 0;
		
		new Thread( () -> {
			try {
		        // Create a server socket
		        ServerSocket serverSocket = new ServerSocket(8004);
		        System.out.println("MultiThreadServer started at " + new Date() + '\n');
		    
		        while (true) {
		        	// Listen for a new connection request
		        	Socket socket = serverSocket.accept();
		    
		          	// Increment clientNo
		          	//clientNo++;
		            System.out.println("Starting thread at " + new Date() + '\n');
	
		            // Find the client's host name, and IP address
		            InetAddress inetAddress = socket.getInetAddress();
		            System.out.println("Host name is "+ inetAddress.getHostName() + "\n");
		            System.out.println("IP Address is " + inetAddress.getHostAddress() + "\n");
		          
		            // Create and start a new thread for the connection
		            new Thread(new HandleAClient(socket)).start();
		        }
		    }
		    catch(IOException ex) {
		        System.err.println(ex);
		    }
		}).start();
		
	}
}

class HandleAClient implements Runnable {
	private Socket socket; // A connected socket

    /** Construct a thread */
    public HandleAClient(Socket socket) {
      this.socket = socket;
    }

    /** Run a thread */
    public void run() {
    	BufferedReader inputFromFile = null;
    	PrintWriter outputToFile = null;
        BufferedReader inputFromClient = null;
        PrintWriter outputToClient = null;
        FileReader in = null;
        FileWriter out = null;
        
        try{
	        in = new FileReader("account.txt");
	        //out = new FileWriter("account.txt");
        }
        catch(IOException ex){
        	ex.printStackTrace();
        }
        
		try {
			// Create data input and output streams
			inputFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			outputToClient = new PrintWriter(new BufferedWriter (
					new OutputStreamWriter(socket.getOutputStream())),true);
			inputFromFile = new BufferedReader(in);
			//outputToFile = new PrintWriter(out);
	
			// Continuously serve the client
			boolean founded = false;
			while (!founded) {
			  String id = inputFromClient.readLine();
			  String pwd = inputFromClient.readLine();
			  String _id, _pwd;
			  
			  while(inputFromFile.ready()){
				  _id = inputFromFile.readLine();
				  _pwd = inputFromFile.readLine();
				  
				  if((_id.equals(id)) && (_pwd.equals(pwd))){
					  outputToClient.println("1");
					  System.out.println("Success!");
					  founded = true;
					  break;
				  }
			  }
			  
			  if(!founded){
				  outputToClient.println("0");
				  System.out.println("Wrong id or pwd!");
			  }
			  
		    }
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		
	}
}