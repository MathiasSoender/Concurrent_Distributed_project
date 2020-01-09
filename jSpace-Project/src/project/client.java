package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.List;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
//IMPORTANT: remember to change tcp://xxx for your current wifi!

public class client {
	static final String mainUri = "tcp://192.168.1.100:5001/";
    public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
    	
    	//Connection client to server
		String uri = "tcp://192.168.1.100:5001/clientServerSpace?keep";
		RemoteSpace clientServerSpace = new RemoteSpace(uri);


    	System.out.println("Client is connected");

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print("Username: ");
		String userName = input.readLine();

		
		System.out.print("Create game? (Y/N): ");
		String createGame = input.readLine();
		
		//The user is now a host of a game
		if(createGame.equals("Y")){
			//We tell the server to create a game, with us as Host, The -1 has to be there!
			clientServerSpace.put("newUser","createGame", userName, -1);
			
			//We get the confirm status, with the randomly generated gamePin:
			Object[] gamePinInfo =clientServerSpace.get(new ActualField("gameCreated"),new FormalField(Integer.class));
			System.out.println("Game started with pin: " + gamePinInfo[1]);
			
			//Connect host to the localUserData:
			String uriLocalData = "tcp://192.168.1.100:5001/localUserData"+gamePinInfo[1]+"?keep";
			RemoteSpace localUserData = new RemoteSpace(uriLocalData);
			
			//Time to start the game! (Only host does this)
			System.out.print("Start game? (Y): ");
			String startGame = input.readLine();
			
			//Game has been started
			if(startGame.equals("Y")) {
				//We tell the others that the game is ready to go:
				localUserData.put("GameStarted");


				new Thread(new startGame(localUserData,userName)).start();
												
			}
			//Game did not start correct
			else {
				System.out.println("Game did not start correctly, reset.");
			}

		}
		
		//The user is now a regular player of a game
		else {
			System.out.print("Join game? (Y): ");
			String joinGame = input.readLine();
			RemoteSpace localUserData;
			
			if(joinGame.equals("Y")) {
				System.out.print("Game pin?: ");
				String gamePinStr =  input.readLine();
				int gamePin = Integer.parseInt(gamePinStr);
				
				clientServerSpace.put("newUser","joinGame", userName, gamePin);		
				System.out.println("Player is connected!");
				
				//Connect Player to local game
				String uriLocalData = "tcp://192.168.1.100:5001/localUserData"+gamePin+"?keep";
				localUserData = new RemoteSpace(uriLocalData);

				localUserData.query(new ActualField("GameStarted"));
				System.out.println("Game has started!");

				new Thread(new startGame(localUserData,userName)).start();
			}
			else {
				System.out.println("Goodbye.");
			}
		}
		
		
		//More to come about game info (Questions, voting etc)
		

		
		//game logic
		//


    	

}
}


class startGame implements Runnable {
	private RemoteSpace localUserSpace;
	private String userName;


	startGame(RemoteSpace space, String userName) {
		this.localUserSpace = space;
		this.userName = userName;

	}

	@Override
	public void run() {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));



		while (true) {

			try {
				//Grab the object from the server. 
				Object[] message = localUserSpace.get(new ActualField(userName), new FormalField(String.class), new FormalField(String.class));
				String type = (String) message[1];
				String output = (String) message[2];
				
				//Check what instructions are given:
				if (type.equals("InputInitial")) {

					System.out.println(output);
					String Question = reader.readLine();
					
					localUserSpace.put("QuestionInitial",userName, Question);


				}
				if (type.equals("hostmessage") && userName.equals(message[0])) {

					System.out.println(output);
					String Question = reader.readLine();
					if (Question.equals("Y")) {

						localUserSpace.put("continueGame");



					}



				}
			}















			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

