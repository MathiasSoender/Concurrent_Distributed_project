package common.src.main;

import org.jspace.FormalField;
import org.jspace.RandomSpace;
import org.jspace.Space;

public class App {

	public static void main(String[] argv) throws InterruptedException {
		Space inbox = new RandomSpace();

		inbox.put("Hello World!");
		Object[] tuple = inbox.get(new FormalField(String.class));
		System.out.println(tuple[0]);

	}

}