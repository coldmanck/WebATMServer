import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
	private BufferedReader inputFromFile = null;
	private PrintWriter outputToFile = null;
	private BufferedReader inputFromClient = null;
	private PrintWriter outputToClient = null;
	private FileReader in = null;
	private FileWriter out = null;
	
	private String id, pwd;
	private String _id, _pwd;
	private int cash;
//	private String cashStr;
	
	private String choice;
	
	private ArrayList<String> accountHistory1 = new ArrayList<>();
	private ArrayList<String> accountHistory2 = new ArrayList<>();
	
	private String[] unableAccounts;
	private ArrayList<String> unableAccountsAL = new ArrayList<>();
	private static int loginFailTimes = 0;
	
    /** Construct a thread */
    public HandleAClient(Socket socket) {
      this.socket = socket;
    }

    /** Run a thread */
    public void run() {
        try{
	        in = new FileReader("account.txt");
//	        out = new FileWriter("account.txt");
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
//			outputToFile = new PrintWriter(out);
			
			// retrieve unable accounts
			unableAccounts = inputFromFile.readLine().split(":");
			unableAccountsAL.addAll(Arrays.asList(unableAccounts));
	
			// Continuously serve the client
			boolean founded = false;
			boolean containsID = false;
			
			while (!founded) {
				id = inputFromClient.readLine();
				pwd = inputFromClient.readLine();
  
				// Scan the database
				while(inputFromFile.ready()){
					_id = inputFromFile.readLine();
					// Check if id is an unable account
				    if(unableAccountsAL.contains(_id))
					    break;
				  
				    if(_id.equals(id)){
				    	containsID = true;
					    _pwd = inputFromFile.readLine();
					    if(_pwd.equals(pwd)){
						    cash = Integer.parseInt(inputFromFile.readLine());
						    outputToClient.println(String.valueOf(cash));
						    System.out.println("Success!");
						    addHistory(id ,String.valueOf(cash));
						  
						    founded = true;
						    break;
					    } 
				    }
			    }
				
				// if not found
				if(!founded){
					if(containsID){
						outputToClient.println("-1");
						System.out.println("Wrong pwd or unable accounts!");
					  
						if(++loginFailTimes == 3){
							unableAccountsAL.add(id);
							unableAccounts = unableAccountsAL.toArray(
									new String[unableAccountsAL.size()]);
							updateUnableAccounts();
							loginFailTimes = 0;
							System.out.println("Fail over 3 times! Lock.");
						}
					}
					else{
						createNewAccount(id, pwd);
						
						cash = 0;
						outputToClient.println(String.valueOf(cash));
						addHistory(id, String.valueOf(cash));
						
						System.out.println("Create new account!");
						founded = true;
						break;
					}
				}	// end if(!founded)
		    }	// end while(!founded)
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		
		int newCash = cash;
		String amount, targetBankNo, isInternal;
		
		while(true){
			try{
				choice = inputFromClient.readLine();
				if(choice.equals("deposit")){
					amount = inputFromClient.readLine();
					newCash = newCash + Integer.valueOf(amount);
					
					if(changeNowCash(newCash))
						outputToClient.println(String.valueOf(newCash));
					else
						outputToClient.println("-1");
				}
				else if(choice.equals("withdraw")){
					amount = inputFromClient.readLine();
					newCash = newCash - Integer.valueOf(amount);
					
					if(changeNowCash(newCash))
						outputToClient.println(String.valueOf(newCash));
					else
						outputToClient.println("-1");
				}
				else if(choice.equals("transfer")){
					amount = inputFromClient.readLine();
					targetBankNo  = inputFromClient.readLine();
					isInternal = inputFromClient.readLine();
					
					newCash = newCash - Integer.valueOf(amount);
					
					
					if(isInternal.equals("true") ? 
							changeNowCash(newCash) : changeNowCash(newCash - 15)){
						if(transfer(targetBankNo, amount))
							outputToClient.println(isInternal.equals("true") ? 
									String.valueOf(newCash) : String.valueOf(newCash - 15));
						else
							outputToClient.println("-1");
					}
					else
						outputToClient.println("-1");
					
				}
				else if(choice.equals("view_history")){
					if(id.equals("coldmanck@gmail.com")){
						outputToClient.println(accountHistory1.size());
//						outputToClient.print(accountHistory1);
					}
					else if(id.equals("coldman519@yahoo.com.tw")){
						outputToClient.println(accountHistory2.size());
//						outputToClient.print(accountHistory2);
					}
				}
				else if(choice.equals("log_out")){
					break;
				}
			
			}
			catch(IOException ex){
				ex.printStackTrace();
			}
			
			// Update now cash
			cash = newCash;
		}
		
		outputToClient.println("log_out_success");
		System.out.println("Log out!");
		
	}
    
    public boolean changeNowCash(int newCash){
    	boolean status = false;
    	
    	try{
    		in = new FileReader("account.txt");
	        inputFromFile = new BufferedReader(in);
    		
    		String totalStr = "";
	        while(inputFromFile.ready()) {
	            totalStr += inputFromFile.readLine();
	            totalStr += "\n";
	        }
	        totalStr = totalStr.replace(String.valueOf(cash), String.valueOf(newCash));
	        
	        in.close();
	        
	        // Change amount
	        out = new FileWriter("account.txt");
	        outputToFile = new PrintWriter(out);
	        outputToFile.print(totalStr);
	        out.close();
	        
	        status = true;
    	}
    	catch(IOException ex){
    		ex.printStackTrace();
    	}
    	
    	if(status){
			System.out.println("Now cash: " + newCash + " true");
			return true;
    	}
		else{
			System.out.println("Now cash: " + newCash + " false");
			return false;
		}
    }
    
    public boolean transfer(String targetBankNo, String amount){
    	boolean status = false;
    	int newCash = 0;
    	
    	try{
	    	in = new FileReader("account.txt");
	        inputFromFile = new BufferedReader(in);
	    	
	    	boolean founded = false;
			
		    while(inputFromFile.ready()){
			    _id = inputFromFile.readLine();
			  
			    if(_id.equals(targetBankNo)){
			    	founded = true;
				    inputFromFile.readLine();	// empty
		
				    // Temporaily cash = target bank's cash 
				    cash = Integer.parseInt(inputFromFile.readLine());
				    newCash = cash + Integer.valueOf(amount);
				    
				    break;
		        }
		    }
		    in.close();
		    
		    if(founded)
			    if(changeNowCash(newCash))
			    	status = true;
		    else
				 System.out.println("Wrong bank number!");
		}	// end try
    	catch(IOException ex){
    		ex.printStackTrace();
    	}
    	
    	return status;
    }
    
    public void addHistory(String id, String cash){
    	if(id == "coldmanck@gmail.com")
    		accountHistory1.add(String.valueOf(cash));
		else if(id == "coldman519@yahoo.com.tw")
			accountHistory2.add(String.valueOf(cash));
    }
    
    public void updateUnableAccounts(){
    	try{
    		in = new FileReader("account.txt");
	        inputFromFile = new BufferedReader(in);
	        
	        String totalStr = "";
	        // Replace the first record
	        for(int i = 0; i < unableAccounts.length; i++){
	        	totalStr += unableAccounts[i];
	        	if(i != unableAccounts.length - 1)
	        		totalStr += ":";
	        }
	        totalStr += "\n";
	        
	        inputFromFile.readLine();
	        while(inputFromFile.ready()) {
	            totalStr += inputFromFile.readLine();
	            totalStr += "\n";
	        }
	        in.close();
	        
	        // Change amount
	        out = new FileWriter("account.txt");
	        outputToFile = new PrintWriter(out);
	        outputToFile.print(totalStr);
	        out.close();
    	}
    	catch(IOException ex){
    		ex.printStackTrace();
    	}
    }
    
    public void createNewAccount(String id, String pwd){
    	try{
    		in = new FileReader("account.txt");
	        inputFromFile = new BufferedReader(in);
    		
    		String totalStr = "";
	        while(inputFromFile.ready()) {
	            totalStr += inputFromFile.readLine();
	            totalStr += "\n";
	        }
	        in.close();
	        
	        totalStr += (id + "\n" + pwd + "\n" + "0\n");
	        
	        out = new FileWriter("account.txt");
	        outputToFile = new PrintWriter(out);
	        outputToFile.print(totalStr);
	        out.close();
    	}
    	catch(IOException ex){
    		ex.printStackTrace();
    	}
        
    }
}