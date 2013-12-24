package errornotifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ExitThread implements Runnable {
	
	ErrorNotifier notifier = null;

	public void run() {
		String line = "";		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			while((line = br.readLine()) != null ){				
				if(line.equalsIgnoreCase("exit")){
					notifier.execute = false;
					notifier.conn.close();
					System.exit(0);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
