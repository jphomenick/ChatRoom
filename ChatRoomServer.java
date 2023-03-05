//ECE 492-044 Lab 3 ChatRoomServer
//Joseph Homenick
//February 6 2023

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomServer implements Runnable
{
	private ConcurrentHashMap<String, String> passwords = new ConcurrentHashMap();
	
	private ConcurrentHashMap<String, ObjectOutputStream> whosIn = new ConcurrentHashMap();
	
	private ServerSocket ss;
	

	public static void main(String[] args)
	{
		System.out.println("This program was written by Joseph Homenick");
		
		if (args.length > 0)
		{
			System.out.println("Command line arguments are being ignored.");
		}
		
		try
		{
			new ChatRoomServer();
		}
		catch (Exception err1)
		{
			System.out.println(err1.getMessage());
		}

	}
	
	public ChatRoomServer() throws Exception
	{
		try
		{
			this.ss = new ServerSocket(2222);
		}
		catch(BindException err2)
		{
			throw new IllegalArgumentException("Port 2222 is not available.");
		}
		
		try
		{
			FileInputStream fis = new FileInputStream("passwords.ser");
			ObjectInputStream ois = new ObjectInputStream(fis);
			this.passwords = (ConcurrentHashMap) ois.readObject();
			ois.close();
		}
		catch (FileNotFoundException fnfe)
		{
			System.out.println("passwords.ser is not found, so an empty collection will be used.");
		}
		
		System.out.println("ChatRoomServer is up at " + InetAddress.getLocalHost().getHostAddress() 
				+ " on port " + this.ss.getLocalPort());
		
		System.out.println("Previously in the chat room: ");
		System.out.println(this.passwords);
		
		new Thread(this).start();
	}

	@Override
	public void run()
	{
		Socket s = null;
		ObjectInputStream ois = null;
		ObjectOutputStream oos = null;
		ObjectOutputStream previousOOS = null;
		String joinMessage = null;
		String chatName = null;
		String providedPassword = null;
		String storedPassword = null;
		String clientAddress = null;
		
		try
		{
			s = this.ss.accept();
			clientAddress = s.getInetAddress().getHostAddress();
			System.out.println("New client connecting from " + clientAddress);
			ois = new ObjectInputStream(s.getInputStream());
			joinMessage = ((String) ois.readObject()).trim();
			oos = new ObjectOutputStream(s.getOutputStream());
		}
		catch(Exception e)
		{
			System.out.println("Client " + clientAddress + " join protocol not OOS or 1st message not String. " + e );
			
			if (s.isConnected())
			{
				try {s.close();}
				catch (IOException ioe) {}
			}
			
			return;
		}
		
		finally
		{
			new Thread(this).start();
		}
		
		int blankOffset = joinMessage.indexOf(" ");
		
		try
		{
			if (blankOffset < 0)
			{
				try
				{
					System.out.println("No blank in join message: " + joinMessage);
					oos.writeObject("Invalid format in 1st message.");
					oos.close();
				}
				catch (Exception e) {}
				return;
			}
			
			chatName = joinMessage.substring(0, blankOffset).toUpperCase();
			providedPassword = joinMessage.substring(blankOffset).trim();
			
			if (passwords.containsKey(chatName))
			{
				storedPassword = (String)this.passwords.get(chatName);
				
				if (providedPassword.equals(storedPassword))
				{
					if (this.whosIn.containsKey(chatName))
					{
						previousOOS = whosIn.get(chatName);
						whosIn.replace(chatName, oos);
						previousOOS.writeObject("Session terminated due to rejoin from another location.");
						previousOOS.close();
						System.out.println(chatName + " is rejoining.");
					}
				}
				
				else
				{
					oos.writeObject("Your entered password " + providedPassword + " is not the same as the password stored for chat "
							+ "name " + chatName);
					oos.close();
					System.out.println("Invalid password: " + providedPassword + " instead of " + storedPassword + " for " + chatName);
					return;
				}
					
			}
			
			else
			{
				passwords.put(chatName, providedPassword);
				savePasswords();
				System.out.println(chatName + " is a new client in the chat room.");
			}
			
			oos.writeObject("Welcome to the chat room " + chatName + " !");
			
			this.whosIn.put(chatName, oos);
			
			System.out.println(chatName + " is joining.");
			
			String[] whosInArray = whosIn.keySet().toArray(new String[0]);
			
			Arrays.sort(whosInArray);
			
			this.sendToAllClients("Welcome " + chatName + " who has just joined (or rejoined) the chat room!");
			this.sendToAllClients(whosInArray);
			
			String whosInString = "";
			
			for (String name : whosInArray)
			{
				whosInString += name + ", ";
			}
			
			System.out.println("Currently in the chat room: " + whosInString);

		}
		
		catch(Exception e)
		{
			System.out.println("Connection failure during join procesing from " + chatName + " at " + clientAddress + " " + e);
			if (s.isConnected())
			{
				try {s.close();}
				catch(IOException ioe) {}
			}
			return;
		}
		
		try
		{
			while (true)
			{
				Object message = ois.readObject();
				System.out.println("Received '" + message + "' from " + chatName);
				this.sendToAllClients(chatName + " says: " + message);
			}
		}
		catch(Exception e)
		{
			ObjectOutputStream currentOOS = this.whosIn.get(chatName);
			
			if (currentOOS == oos)
			{
				//leave processing
				
				//write trace message to server console of who is leaving
				System.out.println(chatName + " is leaving the chat room.");
				//remove client so we don't send anything to them and don't include them in whosIn list
				this.whosIn.remove(chatName); 
				this.sendToAllClients("Goodbye to " + chatName + " who has just left that chat room!");
				//send updated whosIn list to all clients
				String[] whosInList = (String[])this.whosIn.keySet().toArray(new String[0]);
				Arrays.sort(whosInList);
				this.sendToAllClients(whosInList);
				//write trace message to server console of everyone who is now in the chat room
				String whosInString = "";
				
				for (String name : whosInList)
				{
					whosInString += name + ", ";
				}
				
				System.out.println("Currently in the chat room: " + whosInString);
				
			}
			else
			{
				System.out.println(chatName + " is REJOINING.");
			}
		}
		
		
	}
	
	private synchronized void savePasswords() 
	{
		try
		{
			FileOutputStream fos = new FileOutputStream("passwords.ser");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(this.passwords);
			oos.close();	
		}
		catch (Exception e)
		{
			System.out.println("passwords collection cannot be saved on disk: " + e);
		}
	}
	
	private synchronized void sendToAllClients(Object message)
	{
		ObjectOutputStream[] oosArray = whosIn.values().toArray(new ObjectOutputStream[0]);
		
		for (ObjectOutputStream clientOOS : oosArray)
		{
			try
			{
				clientOOS.writeObject(message);
			}
			catch (IOException ioe) {}
		}
	}

	

}
