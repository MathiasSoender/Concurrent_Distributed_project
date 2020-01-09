package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RandomSpace;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

//IMPORTANT: remember to change tcp://xxx for your current wifi!
public class server {
	static final String mainUri = "tcp://192.168.0.166/";
    public static void main(String[] args) throws InterruptedException {
    	
    	//Connection server - client
		SpaceRepository clientServerRepo = new SpaceRepository();
		SequentialSpace clientServerSpace = new SequentialSpace();
		clientServerRepo.add("clientServerSpace", clientServerSpace);
		clientServerRepo.addGate(mainUri+"?keep");
		
		//Generate random game pins:
		RandomSpace gamePins = new RandomSpace();
		for(int i = 0; i<1000; i++) {
			gamePins.put(i);
		}
		
		
		while(true) {
			System.out.println("Server is now finding requests for newPlayers");
			//Arguments: newUser, joinGame/CreateGame, Username,  GamePin
			Object[] newUserObj = clientServerSpace.get(new ActualField("newUser"), new FormalField(String.class), 
					new FormalField(String.class), new FormalField(Integer.class));

			//This is join game
			if(newUserObj[1].equals("joinGame")) {
		        new Thread(new JoinGame(clientServerSpace, (String) newUserObj[2],(Integer) newUserObj[3])).start();

			}
			//This is create game
			else {
		        new Thread(new CreateGame(gamePins,clientServerSpace, (String) newUserObj[2], clientServerRepo)).start();

			}
			
			
		}
			
    	
    	
    }


}

class CreateGame implements Runnable{
	private RandomSpace gamePins;
	private SequentialSpace clientServerSpace;
	private String userName;
	private SpaceRepository clientServerRepo;
	private SequentialSpace localUserData;
	
	CreateGame(RandomSpace gamePins, SequentialSpace clientServerSpace,
			String userName, SpaceRepository clientServerRepo) {
		
		this.gamePins = gamePins;
		this.clientServerSpace = clientServerSpace;
		this.userName = userName;
		this.clientServerRepo = clientServerRepo;
	}
	
	@Override
	public void run() {
		//Create new spaces of specific game
		    //Create a specific UserName space for this game
		this.localUserData = new SequentialSpace();

		// (USERNAME, TYPE, SCORE, XXX)
		
		

		try {
			//Get a random game pin
			Object[] localGamePin = gamePins.get(new FormalField(Integer.class));

			System.out.println(localGamePin[0]);
						
			//Add spaces to our repo, so Client can connect
			clientServerRepo.add("localUserData"+localGamePin[0], localUserData);

			//Add our user as host. We assume host also plays, so add him as player!
			localUserData.put(userName, "host", 0);

			//And finally we add a return statement, saying everything went fine and giving the game Pin:
			clientServerSpace.put("gameCreated",localGamePin[0]);
			
			//Search for the host to start the game
			localUserData.query(new ActualField("GameStarted"));

			System.out.println("serverside: game started");

			//Ask the players to input their inital game for pairing round
			Questions("Input your initial question","Initial");

			//Ask the host to continue the game
			HostMessage("Continue Game (Y/N)?");

			//Check when the host continues the game
			localUserData.get(new ActualField("continueGame"));
			System.out.println("Serverside: Initial questions given, Host has continued game");
			
			System.out.println("Trying to find all questions");
			List<Object[]> allQuestions = localUserData.queryAll(new ActualField("QuestionInitial"), new FormalField(String.class), new FormalField(String.class));
			for(Object[] p : allQuestions) {
				System.out.println("Question asked: "+p[2] + " from: " + p[1]);
			}
			
			
			







			

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
						
		
	}
	public void Questions(String output, String Round) throws InterruptedException {

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new FormalField(String.class), new FormalField(Integer.class));

		for(Object[] p : allPlayers){
			localUserData.put(p[0],"Input"+Round,output);

		}

	}


	public void HostMessage(String message) throws InterruptedException {

		localUserData.put(userName,"hostmessage",message);

	}

	
}

class JoinGame implements Runnable{
	private SequentialSpace clientServerSpace;
	private String userName;
	private Integer gamePin;

	
	JoinGame(SequentialSpace clientServerSpace, String userName, Integer gamePin) {
		this.clientServerSpace = clientServerSpace;
		this.userName = userName;
		this.gamePin = gamePin;
		
	}
	
	@Override
	public void run() {
		//We open up communication to the correct pin!
		String uriLocalData =server.mainUri+"localUserData"+gamePin+"?keep";

		try {
			RemoteSpace localUserData = new RemoteSpace(uriLocalData);

			//We add the name to the local user names:
			try {
				localUserData.put(userName, "Player", 0);
				//Also add an inital score:
				System.out.println("Player added to game lobby");
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			//Now the player is added!
			
			//For debugging, test the player list:


			
			
			
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

						
		
	}
	

	
}
