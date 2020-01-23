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

public class client {
	//IMPORTANT: remember to change tcp://xxx for your current wifi!
	static final String mainUri = "tcp://192.168.0.166/";
    public static void main(String[] args) throws InterruptedException, UnknownHostException, IOException {
    	
    	//Connection client to server
		String uri = mainUri + "clientServerSpace?keep";
		RemoteSpace clientServerSpace = new RemoteSpace(uri);



    	System.out.println("Client is connected");

		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Username: ");
		String userName = input.readLine();

		//Asks the user if they want to create a game
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

			//asks for the game pin. Host should provide this.
			System.out.print("Game pin?: ");
			String gamePinStr =  input.readLine();
			int gamePin = Integer.parseInt(gamePinStr);

			Object[] joinGameResp = {"",""};
			while(!joinGameResp[1].equals("joinedGame")) {
				//Asks the server to insert us as a player
				clientServerSpace.put("newUser", "joinGame", userName, gamePin);
				joinGameResp = clientServerSpace.get(new ActualField(userName), new FormalField(String.class));

				//Not a unique unsername, try to insert a new username
				if(joinGameResp[1].equals("duplicateName")) {
					System.out.println("Username not unique, enter new username: ");
					userName = input.readLine();
				}
			}

				System.out.println("Player is connected!");
				
				//Connect Player to local game space
				String uriLocalData = mainUri +"localUserData"+gamePin+"?keep";
				localUserData = new RemoteSpace(uriLocalData);

				localUserData.query(new ActualField("GameStarted"));
				System.out.println("Game has started!");

				new Thread(new startGame(localUserData,userName)).start();
			}



		



    	

}
}

//Thread that handles the clients connection with the server thread
class startGame implements Runnable {
	private RemoteSpace localUserSpace;
	private String userName;


	startGame(RemoteSpace space, String userName) {
		this.localUserSpace = space;
		this.userName = userName;

	}


	//Votes for a question
	public void questionVoting() throws IOException, InterruptedException {

		List<Object[]> allQuestions = localUserSpace.queryAll(new ActualField("QuestionMain"),
				new FormalField(String.class), new FormalField(String.class),new FormalField(Integer.class));


		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		//Counter for printing in console.
		Integer Counter = 1;
		//Display all questions
		for (Object[] q : allQuestions) {


			System.out.println(Counter + ") Question:" + q[2]);
			Counter++;
		}
		//Ensure that the number chosen is legit
		String QuestionChosen = "10000000";
		while (Integer.parseInt(QuestionChosen) >= Counter || Integer.parseInt(QuestionChosen) <= 0) {
			System.out.println("Choose your number: ");
			QuestionChosen = reader.readLine();
		}

		System.out.println("You chose question number:" + QuestionChosen);

		//Reinsert the question with incremented score
		int i = Integer.parseInt(QuestionChosen);
		Object[] Question = allQuestions.get(i - 1);
		Object[] QuestionValue = localUserSpace.get(new ActualField("QuestionMain"), new ActualField(Question[1]), new ActualField(Question[2]), new FormalField(Integer.class));

		Integer Value = (Integer) QuestionValue[3] + 1;
		localUserSpace.put("QuestionMain", Question[1], Question[2], Value);


		//Value of chosen pair
		System.out.println("Your chosen question now has value: " + Value);


	}

	//Votes for a pair
	public void pairVoting() throws InterruptedException, IOException {
		//Works the same way as questionVoting

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
		System.out.println("\n//////////////////////////");
		System.out.println("\nYou chose pair number:" +PairChosen);

		int i = Integer.parseInt(PairChosen);
		Object[] Pair = allPairs.get(i-1);
		Object[] PairValue = localUserSpace.get(new ActualField("pair"), new ActualField(Pair[1]), new ActualField(Pair[2]),new FormalField(Integer.class));

		Integer Value = (Integer) PairValue[3]+1;
		localUserSpace.put("pair", Pair[1], Pair[2],Value);


		//Value of chosen pair
		System.out.println("Your chosen pair now has value: "+ Value);


	}

	//Back 2 back players answering of question given
	public void questionPairVoting() throws IOException, InterruptedException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String answer = "";
		while(!answer.equals("Y") && !answer.equals("N")) {
			System.out.println("CHOOSE Y/N");
			answer = reader.readLine();
		}
		localUserSpace.put("QuestionPairVoting",userName,answer);




	}

	//Method for displaying score:
	public void showScore(boolean isFinal) throws InterruptedException {
		List<Object[]> allPlayers = localUserSpace.queryAll(new FormalField(String.class),  new FormalField(String.class),new FormalField(Integer.class));


		for (Object[] p : allPlayers) {
			//Prints your own score
			if(p[0].equals(userName)){
				System.out.println("You have: " + p[2]);
			}
			else {
				//prints the score of all players (Shown names)
				if (isFinal) {
					System.out.print(p[0] + ": ");
				//Prints the score of all players (anonymously)
				//Names are hidden, as we do not want a wining player being targeted.
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
		//Start the thread that answers pings
		new Thread(new aliveChecker(localUserSpace, userName)).start();



		while (true) {

			try {
				//Grab the object from the server.
				Object[] message = localUserSpace.get(new ActualField(userName), new FormalField(String.class), new FormalField(String.class));

				String type = (String) message[1];
				String output = (String) message[2];

				System.out.println("______________________________\n");


				
				//Check what instructions are given:
				if (type.equals("InputInitial")) {

					//Initial question given, and passed back
					System.out.println(output);
					String Question = reader.readLine();
					
					localUserSpace.put("QuestionInitial",userName, Question);

					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);

				}



				if (type.equals("InputPairVoting")) {
					System.out.println(output);
					//Voting for pairs
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

				//back to back players answer the question
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
					//However somebody must ask a question!
					if(!Question.equals("pass")) {
						localUserSpace.put("QuestionMain", userName, Question, 0);
					}
					//Counter stuff
					Object[] fooCounter = localUserSpace.get(new ActualField("globalCounter"), new FormalField(Integer.class));
					localUserSpace.put(fooCounter[0], (Integer) fooCounter[1]+1);




				}

				if(type.equals("InputQuestionVoting")) {
					//Votes for a question
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

//Thread that continuously handles the pings to clients.
class aliveChecker implements  Runnable{
	private RemoteSpace localUserSpace;
	private String UserName;

	aliveChecker(RemoteSpace localUserSpace, String UserName){
		this.localUserSpace = localUserSpace;
		this.UserName = UserName;


	}
	public void run(){

		while(true){
			try {
				localUserSpace.get(new ActualField(UserName), new FormalField(String.class));

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}
}
