import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class ChatRoomClient implements ActionListener
{
	//Instance variables (dynamic storage)
	Socket s;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	String newLine = System.lineSeparator();
	int fontSize = 20;
	int maxFontSize = 50;
	int minFontSize = 5;
	
	//GUI Objects
	JFrame      chatWindow          = new JFrame(); // like int x = 5; to provide an inital value to the pointer field
	JPanel      topPanel            = new JPanel();
	JPanel      middlePanel         = new JPanel();
	JPanel      bottomPanel         = new JPanel();
	JLabel      sendMsgLabel        = new JLabel("Enter a message here and push SEND below.");
	JLabel      whosInLabel         = new JLabel("Who's in the chat room:");
	JLabel      receivedMsgsLabel   = new JLabel("Received messages (including our sends)");
	JButton     sendPublicButton    = new JButton("Send To Everyone In");
	JButton     availableButton     = new JButton();
	JTextField  errMsgTextField     = new JTextField("Error messages will show here.");
	JTextArea   sendChatArea        = new JTextArea();
	JList<String> whosInList        = new JList<String>(); // note JList is declared to hold String objects (chat names)
	JTextArea   receiveChatArea     = new JTextArea();
	JScrollPane sendScrollPane      = new JScrollPane(sendChatArea);
	JScrollPane whosInScrollPane    = new JScrollPane(whosInList); 
	JScrollPane receiveScrollPane   = new JScrollPane(receiveChatArea); 
	 
	//Window Menu items
	MenuBar  menuBar             = new MenuBar();
	Menu     fontMenu            = new Menu("Font");
	MenuItem biggerFontMenuItem  = new MenuItem("Bigger");
	MenuItem smallerFontMenuItem = new MenuItem("Smaller");
	

	public static void main(String[] args)
	{
		System.out.println("This program was written by Joseph Homenick for ECE 492 Spring 2023\n");
		
		if (args.length != 3)
		{
			System.out.println("Restart! Provide 3 command line paramaters: Server Address, Chat Name & Password.");
			return;
		}
		
		if (args.length == 3)
		{
			try
			{
				ChatRoomClient crc = new ChatRoomClient(args[0], args[1], args[2]);
				crc.receive();
			}
			catch(Exception e)
			{
				System.out.println(e);
				return;
			}
			
					
		}

	}
	
	public ChatRoomClient(String serverAddress, String chatName, String pw) throws Exception
	{
		if (serverAddress.contains(" ") || chatName.contains(" ") || pw.contains(" ") ) //check for blanks in parameters
		{
			throw new IllegalArgumentException("Parameters may not contain blanks.");
		}
		else
		{
			
			System.out.println("Connecting to the chat room server at " + serverAddress + " on port 2222.");
			
			try
			{
				this.s  = new Socket(serverAddress, 2222);
			}
			catch(Exception e2)
			{
				throw new IllegalArgumentException("Connect to chat server at " + serverAddress + "on port 2222 has failed." 
						+ this.newLine + "Is the server running? Is the server address correct?"+ this.newLine + e2);
			}
			
			System.out.println("Connected to the chat server!");
			
			this.oos = new ObjectOutputStream(s.getOutputStream()); //oos is instance variable
			this.oos.writeObject(chatName + " " + pw); //send join message
			this.ois = new ObjectInputStream(s.getInputStream()); //ois is instance variable
			String reply = (String) this.ois.readObject(); //wait for sever response (cast Object to String)
			
			if (reply.startsWith("Welcome"))
			{
				System.out.println("Join was successful!");
				
				UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
				
				this.topPanel.setLayout(new GridLayout(1,3)); // a format with 1 row and 3 columns
				this.topPanel.add(sendMsgLabel);              // goes in row 1 column 1
				this.topPanel.add(whosInLabel);               // goes in row 1 column 2
				this.topPanel.add(receivedMsgsLabel);         // goes in row 1 column 3
				this.chatWindow.getContentPane().add(topPanel,"North");

				this.middlePanel.setLayout(new GridLayout(1,3)); 
			    this.middlePanel.add(sendScrollPane);
				this.middlePanel.add(whosInScrollPane);
				this.middlePanel.add(receiveScrollPane);
				this.chatWindow.getContentPane().add(middlePanel,"Center");

				this.bottomPanel.setLayout(new GridLayout(1,3)); 
				this.bottomPanel.add(sendPublicButton);
				this.bottomPanel.add(availableButton); // anticipate some future function
				this.bottomPanel.add(errMsgTextField);
				this.chatWindow.getContentPane().add(bottomPanel,"South");
				
				this.chatWindow.setTitle(chatName + "'s CHAT ROOM    (Close this window to leave the chat room.)");
				
				this.sendPublicButton.setBackground(Color.green);
				this.whosInLabel.setForeground(Color.blue);
				
				this.receiveChatArea.setLineWrap(true);     // cause long text added to be properly
				this.receiveChatArea.setWrapStyleWord(true);// "wrapped" to the next line.
				this.sendChatArea.setLineWrap(true);
				this.sendChatArea.setWrapStyleWord(true);
				
				this.receiveChatArea.setEditable(false); // keep user from changing the output area!
				this.errMsgTextField.setEditable(false); // keep user from changing the error message area!
				
				this.sendPublicButton.addActionListener(this); // sendPublicButton can now call us when the user pushes it! 
				
				this.chatWindow.setMenuBar(menuBar);
				this.menuBar.add(fontMenu);
				this.fontMenu.add(biggerFontMenuItem);
				this.fontMenu.add(smallerFontMenuItem);
				this.biggerFontMenuItem.addActionListener(this); // so these FontMenuItems (buttons!) 
				this.smallerFontMenuItem.addActionListener(this);// in the font menu can call us!
				
				this.chatWindow.setSize(800,500);    // width,height
				this.chatWindow.setLocation(400,0);  // x,y (x is "to the right of the left margin", y is "down-from-the-top")
				this.chatWindow.setVisible(true);    // show it
				this.chatWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // terminate if user closes window
                
			}
			else 
			{
				throw new IllegalArgumentException("Join of " + chatName + " with password " + pw + " was not successful.");
			}
			
			
		}
	}

	
	public void actionPerformed(ActionEvent ae)
	{
		this.errMsgTextField.setText("");                // clear any error message
		this.errMsgTextField.setBackground(Color.white); // and remove highlight
		
		if (ae.getSource() == sendPublicButton)
		{
			String chatMessageToSend = sendChatArea.getText().trim();
			
			if (chatMessageToSend.length() == 0)
			{
				this.errMsgTextField.setText("No message entered to send.");
			    this.errMsgTextField.setBackground(Color.pink); // highlight to get attention
			    return; // return button's thread to the button.
			}
			
			else
			{
				System.out.println("Your message '" + chatMessageToSend + "' is being sent to the server.");
				
				try
				{
					this.oos.writeObject(chatMessageToSend);
					this.sendChatArea.setText(""); // clear the input field.(indication to user that the message was sent.)
				}
				
				catch(IOException e3) //Network or server is down
				{
					this.errMsgTextField.setText("Connection to the chat server has failed. Restart client.");
                    this.errMsgTextField.setBackground(Color.pink);
                    this.sendChatArea.setEditable(false);
                    this.sendPublicButton.setEnabled(false);
					
				}
				
			}
			
			return; 
		}
		
		if (ae.getSource() == biggerFontMenuItem)
		{
			//increase font size in the in and out chatTextAreas
			if (fontSize < maxFontSize) { fontSize += 1; } 
			this.sendChatArea.setFont(new Font("default", Font.BOLD, fontSize)); 
		    this.receiveChatArea.setFont(new Font("default", Font.BOLD,fontSize));
		    return;
			
		}
		
		if (ae.getSource() == smallerFontMenuItem)
		{
			//increase font size in the in and out chatTextAreas
			if (fontSize > minFontSize) { fontSize -= 1; } 
			this.sendChatArea.setFont(new Font("default", Font.BOLD, fontSize)); 
		    this.receiveChatArea.setFont(new Font("default", Font.BOLD,fontSize));
		    return;
			
		}
		
		

	}
	
	public void receive()
	{
		while (true)
		{
			try
			{
				while (true)
				{
					Object incomingMessage = this.ois.readObject(); //wait for message from server
					if (incomingMessage instanceof String)
					{
						String receivedChatMessage = (String) incomingMessage;
                        this.receiveChatArea.append(this.newLine + receivedChatMessage);
                        this.receiveChatArea.setCaretPosition(this.receiveChatArea.getDocument().getLength());
					}
					if (incomingMessage instanceof String[])
					{
						String[] listOfWhosIn = (String[]) incomingMessage;
						whosInList.setListData(listOfWhosIn);
					}
					
				}
							
			}
			catch(Exception e4)
			{
					this.errMsgTextField.setBackground(Color.pink); // this will get their attention!
					this.errMsgTextField.setText("CHAT ROOM SERVER CONNECTION HAS FAILED!");
					this.receiveChatArea.append(this.newLine + "You must close this chat window and then restart the client to reconnect to the server to continue.");
					this.receiveChatArea.setCaretPosition(receiveChatArea.getDocument().getLength()); // scroll down
					// disable the GUI function
					this.sendChatArea.setEditable(false); // keep user from trying to send any more messages.
					this.sendPublicButton.setEnabled(false);    // stop button pushing
					return;
			}
		}
		
		
		
	}

	

}
