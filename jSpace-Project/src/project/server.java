package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RandomSpace;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
import org.jspace.Space;
import org.jspace.SpaceRepository;

//IMPORTANT: remember to change tcp://xxx for your current wifi!
public class server {
	static final String mainUri = "tcp://192.168.1.92/";
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
			localUserData.put(userName, "Player", 0);

			//And finally we add a return statement, saying everything went fine and giving the game Pin:
			clientServerSpace.put("gameCreated",localGamePin[0]);
			
			//Search for the host to start the game
			localUserData.query(new ActualField("GameStarted"));

			System.out.println("serverside: game started");

			//Ask the players to input their inital game for pairing round
			Questions("Input your initial question","Initial","Player");

			//Ask the host to continue the game
			HostMessage("Continue Game (Y/N)?");

			//Check when the host continues the game
			localUserData.get(new ActualField("continueGame"));
			System.out.println("Serverside: Initial questions given, Host has continued game");

			while (RandomInitialQuestion()) {

                System.out.println("Trying to find random Question");

                CreatePairs();


                Questions("Vote for your favorite pair", "PairVoting", "Player");

                //Ask host to continue game
                HostMessage("Continue Game (Y/N)?");


                localUserData.get(new ActualField("continueGame"));


                InitBackToBack("GO BACK TO BACK!!!!");


                //Now pair needs to answer the question chosen

                System.out.println("Waiting for question");
                Object[] Question = localUserData.get(new ActualField("RandomInitialQuestion"), new FormalField(String.class));

                System.out.println("Got the question");
                localUserData.put("QuestionForPair", Question[1]);

                Counter = 0;


                System.out.println("Before while");


                while (Counter != 2) {
                    System.out.println(Counter);
                    Questions("Answer the question: ", "AnswerQuestion", "BackToBack");
                    PointsForPair();

                    if (Counter != 2) {

                        HostMessage("Continue Game (Y/N)?");
                        localUserData.get(new ActualField("continueGame"));


                        Questions("Input your new question for the pair", "Question", "Player");
                        HostMessage("Continue Game (Y/N)?");
                        localUserData.get(new ActualField("continueGame"));

                        Questions(" Vote for your favorite Question", "QuestionVoting", "Player");

                        HostMessage("Continue Game (Y/N)?");


                        localUserData.get(new ActualField("continueGame"));

                        QuestionVoting();


                        System.out.println("Inside While");

                    }


                }

            }

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
						
		
	}

	public void PointsForPair() throws InterruptedException {
        Object[] Player1 = localUserData.get(new FormalField(String.class), new ActualField("BackToBack"), new FormalField(Integer.class));
        Object[] Player2 = localUserData.get(new FormalField(String.class), new ActualField("BackToBack"), new FormalField(Integer.class));

        Object[] Answer1 =localUserData.get(new ActualField("QuestionPairVoting"),new ActualField(Player1[0]), new FormalField(String.class));
        Object[] Answer2 =localUserData.get(new ActualField("QuestionPairVoting"),new ActualField(Player2[0]), new FormalField(String.class));

        localUserData.get(new ActualField("QuestionForPair"), new FormalField(String.class));

        if (Answer1[2].equals(Answer2[2])) {
            Counter++;
            localUserData.put(Player1);
            localUserData.put(Player2);



        }

        else {
            localUserData.put(Player1[0],Player1[1],((Integer) Player1[2])+1);
            localUserData.put(Player2[0],Player2[1],((Integer) Player2[2])+1);
            Counter=0;
        }


    }

	public void Questions(String output, String Round, String Type) throws InterruptedException {

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new ActualField(Type), new FormalField(Integer.class));

		for(Object[] p : allPlayers) {


			localUserData.put(p[0],"Input"+Round,output);

		}

	}

    public void QuestionVoting() throws InterruptedException {
        List<Object[]> Questions = localUserData.getAll(new ActualField("QuestionMain"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
        Integer max = 0;
        String q1 = new String();
        String user = new String();
        for(Object[] q : Questions) {
            if ((Integer) q[3] > max) {
                max = (Integer) q[3];
                q1 = (String) q[2];
                user = (String) q[1];
            }

        }


        Object[] winner = localUserData.get(new ActualField(user),new ActualField("Player"),new FormalField(Integer.class));
        localUserData.put(user,"Player",((Integer) winner[2])+1);

        localUserData.put("QuestionForPair",q1);
    }


    public void InitBackToBack(String output) throws InterruptedException {
		List<Object[]> Pairs = localUserData.queryAll(new ActualField("pair"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
		Integer max = 0;
		String pair1 = new String();
		String pair2 = new String();

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

		Questions(pair1 +" and "+pair2+" GO BACK TO BACK","BackToBack","Player");



	}


	public void HostMessage(String message) throws InterruptedException {

		localUserData.put(userName,"hostmessage",message);

	}

	public void CreatePairs() throws InterruptedException {

		Integer size = 4;

		List<String> usernames = new ArrayList<String>();

		List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new ActualField("Player"), new FormalField(Integer.class));

		Random randomizer = new Random();

		for (Object[] p : allPlayers) {

			usernames.add((String) p[0]);


		}

		if (usernames.size()<4) {
			size = 2;
		}

		for (Integer i = 0; i < size; i++) {

			//Finding random pairs


			String random1 = usernames.get(randomizer.nextInt(usernames.size()));

			String random2 = usernames.get(randomizer.nextInt(usernames.size()));


			//Check if they aren't the same person
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

		localUserData.put("RandomInitialQuestion", randomQ[2]);

		return true;

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
			
			try {
				System.out.println("Trying to find all players");
				List<Object[]> allPlayers = localUserData.queryAll(new FormalField(String.class), new ActualField("Player"), new FormalField(Integer.class));
				for(Object[] p : allPlayers) {
					System.out.println("Player: "+p[0]);
					
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

						
		
	}
	

	
}
