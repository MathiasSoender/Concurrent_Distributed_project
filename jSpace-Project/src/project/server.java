package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jspace.*;

public class server {
	//IMPORTANT: remember to change tcp://xxx for your current wifi!
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
		        new Thread(new JoinGame(clientServerSpace, (String) newUserObj[2],(Integer) newUserObj[3], gamePins)).start();


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
	public Integer Counter;
	CreateGame(RandomSpace gamePins, SequentialSpace clientServerSpace,
			String userName, SpaceRepository clientServerRepo) {

		this.gamePins = gamePins;
		this.clientServerSpace = clientServerSpace;
		this.userName = userName;
		this.clientServerRepo = clientServerRepo;


	}
	
	@Override
	public void run() {
		//Create a new space of specific game
		this.localUserData = new SequentialSpace();



		

		try {
			//Get a random game pin
			Object[] localGamePin = gamePins.get(new FormalField(Integer.class));

			System.out.println(localGamePin[0]);
						
			//Add space to our repo, so Client can connect
			clientServerRepo.add("localUserData"+localGamePin[0], localUserData);

			//Add a counter to determine if everybody entered a value
			localUserData.put("globalCounter",0);


			//Add the host as a Player.
			localUserData.put(userName, "Player", 0);

			//And finally we add a return statement, saying everything went fine and giving the game Pin:
			clientServerSpace.put("gameCreated",localGamePin[0], userName);
			
			//Search for the host to start the game
			localUserData.query(new ActualField("GameStarted"));

			System.out.println("serverside: game started");

			//Ask the players to input their inital game for pairing round
			Questions("Input your initial question","Initial","Player");

			//Check if all players are ready to continue
			checkIfPlayersInputted("Player");
			System.out.println("Serverside: Initial questions given, Host has continued game");

			//Check if any initial questions are left:
			boolean runGame = RandomInitialQuestion();
			while (runGame) {

                System.out.println("Trying to find random Question");


                CreatePairs();

                //Voting round, players vote for the pair
                Questions("Vote for your favorite pair", "PairVoting", "Player");

				checkIfPlayersInputted("Player");

				//Tell the players that back to back has been initialized
				InitBackToBack("GO BACK TO BACK!!!!");


                //Now pair needs to answer the question chosen
                Object[] Question = localUserData.get(new ActualField("RandomInitialQuestion"), new FormalField(String.class));

                localUserData.put("QuestionForPair", Question[1]);

                //Counter keeps track of how many questions the pair has answered incorrectly in arow
                Counter = 0;
                while (Counter != 2) {

                    Questions("Answer the question: ", "AnswerQuestion", "BackToBack");

					System.out.println("waiting for b2b");
					checkIfPlayersInputted("BackToBack");

					//Determine how the answering of the question went
					PointsForPair();


					//Players (not back2back) ask new questions for the same pair
                    if (Counter != 2) {


						Questions("Input your new question for the pair, you can type pass to skip"
								, "Question", "Player");

						checkIfPlayersInputted("Player");

                        Questions(" Vote for your favorite Question", "QuestionVoting", "Player");

						checkIfPlayersInputted("Player");

						//Finds the maximum voted question
						QuestionVoting();



                    }


                }
                //Check if game should still be run:
                runGame = RandomInitialQuestion();
                if(runGame) {
					Questions("Scoreboard: ", "ScoreBoard", "Player");
				}

            }
			//End off game.
			Questions("Scoreboard: ","FinalScoreBoard","Player");
			//Forces the clients to exit
			Questions("Goodbye, Thanks for playing", "Exit","Player");
			//All clients have exited
			checkIfPlayersInputted("Player");
			//Re insert the game pin
			gamePins.put((Integer) localGamePin[0]);
			//Remove the space:
			clientServerRepo.remove("localUserData"+localGamePin[0]);


		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
						
		
	}
	//Method for checking if all players are ready to proceed
	public void checkIfPlayersInputted(String type) throws InterruptedException {
		//Here the ping happens (to check if players are still connected)
		//It is smart to have the ping here, as it takes some runtime.
		//But the players are spending their time doing an action, so we have some wait time anyway.
		Thread Alive = new Thread(new playersAlive(localUserData));
		Alive.start();
		Alive.join();


		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class),
				new ActualField(type), new FormalField(Integer.class));

		//Finds the counter, matching the exact player size.
		Object[] fooCounter =  localUserData.get(new ActualField("globalCounter"),
				new ActualField(allPlayers.size()));
		localUserData.put(fooCounter[0], 0);

	}


	//Handles the answering of a question
	public void PointsForPair() throws InterruptedException {
        Object[] Player1 = localUserData.get(new FormalField(String.class), new ActualField("BackToBack"), new FormalField(Integer.class));
        Object[] Player2 = localUserData.get(new FormalField(String.class), new ActualField("BackToBack"), new FormalField(Integer.class));

        Object[] Answer1 =localUserData.get(new ActualField("QuestionPairVoting"),new ActualField(Player1[0]), new FormalField(String.class));
        Object[] Answer2 =localUserData.get(new ActualField("QuestionPairVoting"),new ActualField(Player2[0]), new FormalField(String.class));

        localUserData.get(new ActualField("QuestionForPair"), new FormalField(String.class));

        //The could not agreed (both back2back players answered themselves, or vice versa)
        if (Answer1[2].equals(Answer2[2])) {
            Counter++;

            //If back2back lose, put them back as players
            if (Counter == 2){
				Questions(("They lost this time!\n"+Player1[0]+" answered: "+Answer1[2]+"\n"+Player2[0]+" answered: "+Answer2[2] ),"BackToBack","Player");

				localUserData.put(Player1[0], "Player", Player1[2]);
				localUserData.put(Player2[0], "Player", Player2[2]);

				Questions(("Lost two times in a row, new round!"),"BackToBack","Player");


				}

            else {
				localUserData.put(Player1);
				localUserData.put(Player2);
				Questions(("YOU LOST  THIS ROUND, BE CAREFUL!\n Waiting for next question..."),"BackToBack","BackToBack");
				Questions(("They lost this time!\n"+Player1[0]+" answered: "+Answer1[2]+"\n"+Player2[0]+" answered: "+Answer2[2] ),"BackToBack","Player");

			}



        }
		//Correct answer rewards 1 point, and resets the counter.
        else {
            localUserData.put(Player1[0],Player1[1],((Integer) Player1[2])+1);
            localUserData.put(Player2[0],Player2[1],((Integer) Player2[2])+1);
            Counter=0;
			Questions(("YOU AGREED!"),"BackToBack","BackToBack");
			Questions(("They agreed this time!\n"+Player1[0]+" answered: "+Answer1[2]+"\n"+Player2[0]+" answered: "+Answer2[2] ),"BackToBack","Player");

		}


    }

    //Method for asking questions to the players
	public void Questions(String output, String Round, String Type) throws InterruptedException {

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new ActualField(Type), new FormalField(Integer.class));

		for(Object[] p : allPlayers) {


			localUserData.put(p[0],"Input"+Round,output);

		}

	}

	//Finds the question with max votes
    public void QuestionVoting() throws InterruptedException {
        List<Object[]> Questions = localUserData.getAll(new ActualField("QuestionMain"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));

		Integer max = 0;
		String q1 = new String();
		String user = new String();

		for (Object[] q : Questions) {
			if ((Integer) q[3] > max) {
				max = (Integer) q[3];
				q1 = (String) q[2];
				user = (String) q[1];
			}

		}


		Object[] winner = localUserData.get(new ActualField(user), new ActualField("Player"), new FormalField(Integer.class));
		//The player that asked the question is rewarded with 1 point.
		localUserData.put(user, "Player", ((Integer) winner[2]) + 1);

		localUserData.put("QuestionForPair", q1);
	}

	//Finds the must voted back 2 back pair.
    public void InitBackToBack(String output) throws InterruptedException {
		List<Object[]> Pairs = localUserData.queryAll(new ActualField("pair"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
		Integer max = 0;
		String pair1 = new String();
		String pair2 = new String();

		//Max of all pairs.
		for(Object[] p : Pairs) {
			if ((Integer) p[3] > max) {
				max = (Integer) p[3];
				pair1 = (String) p[1];
				pair2 = (String) p[2];
			}

		}
		localUserData.put(pair1,"InputBackToBack",output);
		localUserData.put(pair2,"InputBackToBack",output);
		Object[] FooPlayer1 = localUserData.get(new ActualField(pair1),new ActualField("Player"),new FormalField(Integer.class));
		Object[] FooPlayer2 = localUserData.get(new ActualField(pair2),new ActualField("Player"),new FormalField(Integer.class));

		localUserData.put(FooPlayer1[0],"BackToBack",FooPlayer1[2]);
		localUserData.put(FooPlayer2[0],"BackToBack",FooPlayer2[2]);

		//Tells the chosen pair to go back to back.
		Questions(pair1 +" and "+pair2+" GO BACK TO BACK","BackToBack","Player");



	}

	//method for creating random pairs
	public void CreatePairs() throws InterruptedException {


		//If pairs are still in the space, remove them
		List<Object[]> allPairs = localUserData.queryAll(new ActualField("pair"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
		if(!allPairs.isEmpty()){
			localUserData.getAll(new ActualField("pair"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
		}

		Integer size = 4;

		//Make a list of all usernames
		List<String> usernames = new ArrayList<String>();

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new ActualField("Player"), new FormalField(Integer.class));

		Random randomizer = new Random();

		//Adds all usernames from the allPlayers list<obj> to usernames list.
		for (Object[] p : allPlayers) {
			System.out.println("Player : " + p[0] + " Found");

			usernames.add((String) p[0]);


		}

		//If less than 4 players, make size 2.
		if (usernames.size()<4) {
			size = 2;
		}

		for (Integer i = 0; i < size; i++) {

			//Finding random pairs
			String random1 = usernames.get(randomizer.nextInt(usernames.size()));
			String random2 = usernames.get(randomizer.nextInt(usernames.size()));


			//Check if they aren't the same person
			//And check if they already are added.
			if ( random1.equals(random2) ||
				(localUserData.queryp(new ActualField("pair"),new ActualField(random1),new ActualField(random2),new ActualField(0)))!=null ||
				(localUserData.queryp(new ActualField("pair"),new ActualField(random2),new ActualField(random1),new ActualField(0)))!=null)
			{
				i--;
			}
			else {
				// pair created, with score 0.
				localUserData.put("pair",random1,random2,0);
			}


		}

	}

	//Method that grabs a random initial question.
	//At the same time it checks if any initial questions are left.
	public boolean RandomInitialQuestion() throws InterruptedException {

		//Removes the last question if it exists.
		Object[] InitialQ = localUserData.queryp(new ActualField("RandomInitialQuestion"), new FormalField(String.class));
		if (InitialQ!=null) {

			localUserData.get(new ActualField("RandomInitialQuestion"), new FormalField(String.class));
		}


		Random randomizer = new Random();

		List<Object[]> allQuestions = localUserData.queryAll(new ActualField("QuestionInitial"), new FormalField(String.class), new FormalField(String.class));
        if (allQuestions.isEmpty()) {
            return false;
        }

		Object[] randomQ = allQuestions.get(randomizer.nextInt(allQuestions.size()));
        //Remember to remove the initial question.
		localUserData.get(new ActualField(randomQ[0]), new ActualField(randomQ[1]), new ActualField(randomQ[2]));
		localUserData.put("RandomInitialQuestion", randomQ[2]);

		return true;

	}
}

//Thread that checks if disconnections occured.
class playersAlive implements  Runnable {
	private SequentialSpace localUserData;

	playersAlive(SequentialSpace localUserData) {
		this.localUserData = localUserData;
	}


	public void run() {
		try {
			//Pings and then sleeps
			this.Ping();
			Thread.sleep(2000);

			List<Object[]> allPings = localUserData.getAll(new FormalField(String.class),
					new ActualField("pinged"));

			//Finds all unremoved pings:
			for (Object[] ping : allPings) {
				localUserData.get(new ActualField(ping[0]),
						new ActualField("Player"), new FormalField(Integer.class));
				System.out.println("Player removed: " + ping[0]);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	//Pings all players
	public void Ping() throws InterruptedException {

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class),
				new ActualField("Player"), new FormalField(Integer.class));

		for (Object[] p : allPlayers) {
			//To deal with a hidden bug, we have to "flush".
			//The bug happens when a player disconnects during a "get" request. The "get" request still happens af-
			//ter the client is completely disconnected.
			localUserData.put(p[0], "flushed");
			localUserData.put(p[0], "pinged");
			System.out.println("1 ping inserted...to: " + p[0]);

		}

	}
}

class JoinGame implements Runnable{
	private SequentialSpace clientServerSpace;
	private String userName;
	private Integer gamePin;
	private RandomSpace gamePins;

	
	JoinGame(SequentialSpace clientServerSpace, String userName, Integer gamePin, RandomSpace gamePins) {
		this.clientServerSpace = clientServerSpace;
		this.userName = userName;
		this.gamePin = gamePin;
		this.gamePins = gamePins;
		
	}
	
	@Override
	public void run() {
		//We open up communication to the correct pin!
		String uriLocalData =server.mainUri+"localUserData"+gamePin+"?keep";

		try {
			//Check if game exists:
			Object[] checkGamePin = gamePins.queryp(new ActualField(gamePin));
			//Game pin has not been used:
			if(!(checkGamePin == null)){
				clientServerSpace.put(userName, "gamePinError");

			}
			else {


				RemoteSpace localUserData = new RemoteSpace(uriLocalData);
				List<Object[]> AllPlayers = localUserData.queryAll(new FormalField(String.class),
						new ActualField("Player"), new ActualField(0));

				//Check if we are unique
				boolean uniqueName = true;
				for (Object[] p : AllPlayers) {
					if (p[0].equals(userName)) {
						uniqueName = false;
					}

				}


				//We add the name to the local user names:
				if (uniqueName) {
					try {
						//insert ourself as a new player
						localUserData.put(userName, "Player", 0);
						clientServerSpace.put(userName, "joinedGame");
						System.out.println("Player added to game lobby");

					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				//if not unique, ask the player to insert new username
				else {
					clientServerSpace.put(userName, "duplicateName");

				}
			}


			
			//Now the player is added!

			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


	}
	

	
}
