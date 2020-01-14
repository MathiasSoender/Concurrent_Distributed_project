package project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import org.jspace.ActualField;
import org.jspace.FormalField;
import org.jspace.RemoteSpace;
import org.jspace.SequentialSpace;
//IMPORTANT: remember to change tcp://xxx for your current wifi!

public class client {
	static final String mainUri = "tcp://192.168.8.109/";
    public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
    	
    	//Connection client to server
		String uri = mainUri + "clientServerSpace?keep";
		RemoteSpace clientServerSpace = new RemoteSpace(uri);


    	System.out.println("Client is connected");

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.print("Username: ");
		String userName = input.readLine();
		String createGame = "";
		while(!createGame.equals("Y") && !createGame.equals("N")) {
			System.out.print("Create game? (Y/N): ");
			createGame = input.readLine();
		}
		
		//The user is now a host of a game
		if(createGame.equals("Y")){
			//We tell the server to create a game, with us as Host, The -1 has to be there!
			clientServerSpace.put("newUser","createGame", userName, -1);
			
			//We get the confirm status, with the randomly generated gamePin:
			Object[] gamePinInfo =clientServerSpace.get(new ActualField("gameCreated"),new FormalField(Integer.class), new ActualField(userName));
			System.out.println("Game started with pin: " + gamePinInfo[1]);
			
			//Connect host to the localUserData:
			String uriLocalData = mainUri +"localUserData"+gamePinInfo[1]+"?keep";
			RemoteSpace localUserData = new RemoteSpace(uriLocalData);
			
			//Time to start the game! (Only host does this)
			String startGame = "";
			while(!startGame.equals("Y")) {
				System.out.print("Start game? (Y): ");
				startGame = input.readLine();
			}
			
			//Game has been started
			//We tell the others that the game is ready to go:
			localUserData.put("GameStarted");
			new Thread(new startGame(localUserData,userName)).start();

		}
		
		//The user is now a regular player of a game
		if(createGame.equals("N")){

			RemoteSpace localUserData;
			
				System.out.print("Game pin?: ");
				String gamePinStr =  input.readLine();
				int gamePin = Integer.parseInt(gamePinStr);
				
				clientServerSpace.put("newUser","joinGame", userName, gamePin);		
				System.out.println("Player is connected!");
				
				//Connect Player to local game
				String uriLocalData = mainUri +"localUserData"+gamePin+"?keep";
				localUserData = new RemoteSpace(uriLocalData);

				localUserData.query(new ActualField("GameStarted"));
				System.out.println("Game has started!");

				new Thread(new startGame(localUserData,userName)).start();
			}



		



    	

}
}


class startGame implements Runnable {
	private RemoteSpace localUserSpace;
	private String userName;


	startGame(RemoteSpace space, String userName) {
		this.localUserSpace = space;
		this.userName = userName;

	}

	public void questionVoting() throws IOException, InterruptedException {

		List<Object[]> allQuestions = localUserSpace.queryAll(new ActualField("QuestionMain"),
				new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));



			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			Integer Counter = 1;


			for (Object[] q : allQuestions) {


				System.out.println(Counter + ") Question:" + q[2]);
				Counter++;
			}
			String QuestionChosen = "10000000";
			while (Integer.parseInt(QuestionChosen) >= Counter || Integer.parseInt(QuestionChosen) <= 0) {
				System.out.println("Choose your number: ");
				QuestionChosen = reader.readLine();
			}

			System.out.println("You chose question number:" + QuestionChosen);

			int i = Integer.parseInt(QuestionChosen);
			Object[] Question = allQuestions.get(i - 1);


			Object[] QuestionValue = localUserSpace.get(new ActualField("QuestionMain"), new ActualField(Question[1]), new ActualField(Question[2]), new FormalField(Integer.class));

			Integer Value = (Integer) QuestionValue[3] + 1;


			localUserSpace.put("QuestionMain", Question[1], Question[2], Value);


			//Value of chosen pair
			System.out.println("Your chosen question now has value: " + Value);


		}




	public void pairVoting() throws InterruptedException, IOException {



		List<Object[]> allPairs = localUserSpace.queryAll(new ActualField("pair"), new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));
		Object[] Question = localUserSpace.query(new ActualField("RandomInitialQuestion"),new FormalField(String.class));

		System.out.println("Initial Question: "+(String) Question[1]);

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		Integer Counter = 1;
		for (Object[] p : allPairs) {


			System.out.println(Counter + ") pair usernames:" + p[1] + " and " + p[2]);
			Counter++;
		}
		String PairChosen = "10000000";
		while(Integer.parseInt(PairChosen)>= Counter || Integer.parseInt(PairChosen)<=0 ) {
			System.out.println("Choose your number: ");
			PairChosen = reader.readLine();
		}

		System.out.println("You chose pair number:" +PairChosen);

		int i = Integer.parseInt(PairChosen);
		Object[] Pair = allPairs.get(i-1);


		Object[] PairValue = localUserSpace.get(new ActualField("pair"), new ActualField(Pair[1]), new ActualField(Pair[2]),new FormalField(Integer.class));

		Integer Value = (Integer) PairValue[3]+1;


		localUserSpace.put("pair", Pair[1], Pair[2],Value);


		//Value of chosen pair
		System.out.println("Your chosen pair now has value: "+ Value);




	}

	public void questionPairVoting() throws IOException, InterruptedException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String answer = "";
		while(!answer.equals("Y") && !answer.equals("N")) {
			System.out.println("CHOOSE Y/N");
			answer = reader.readLine();
		}
		localUserSpace.put("QuestionPairVoting",userName,answer);




	}
	public void showScore(boolean isFinal) throws InterruptedException {
		List<Object[]> allPlayers = localUserSpace.queryAll(new FormalField(String.class),  new FormalField(String.class),new FormalField(Integer.class));


		for (Object[] p : allPlayers) {
			if(p[0].equals(userName)){
				System.out.println("You have: " + p[2]);
			}
			else {

				if (isFinal) {
					System.out.print(p[0] + ": ");
				} else {
					System.out.print("Some user: ");
				}
				System.out.println(p[2]);
			}
		}



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

					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);

				}

				if (type.equals("hostmessage")) {

					System.out.println(output);
					String Question = "";
					while(!Question.equals("Y")) {
						Question = reader.readLine();
					}

					localUserSpace.put("continueGame");

				}

				if (type.equals("InputPairVoting")) {
					System.out.println(output);
					pairVoting();

					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);





				}
				if (type.equals("InputBackToBack")) {
					System.out.println(output);


				}
				if (type.equals("InputExit")) {
					System.out.println(output);

					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);

					System.exit(0);
				}

				if(type.equals("InputFinalScoreBoard")){
					System.out.println(output);
					showScore(true);
				}

				if(type.equals("InputScoreBoard")){
					System.out.println(output);
					showScore(false);
				}

				if (type.equals("InputAnswerQuestion")) {

					Object[] Question = localUserSpace.query(new ActualField("QuestionForPair"), new FormalField(String.class));
					System.out.println(output + Question[1]);
					questionPairVoting();

					//Counter stuff
					Object[] fooCounterB2B = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounterB2B[0], (Integer) fooCounterB2B[1]+1);



				}
				if (type.equals("InputQuestion")) {
					System.out.println(output);

					String Question = reader.readLine();
					//Gives the option of passing, if no questions comes to mind.
					//^This ensures game is kept on playing.
					if(!Question.equals("pass")) {
						localUserSpace.put("QuestionMain", userName, Question, 0);
					}
					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);




				}

				if(type.equals("InputQuestionVoting")) {

					questionVoting();

					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);


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

