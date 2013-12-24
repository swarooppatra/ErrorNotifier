package errornotifier;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

/**
 * This class can be used to get the error logs in any log file.
 * @author swaroop
 *
 */
public class ErrorNotifier {
	Connection conn = null;
	boolean execute = true;
	public void startErrorNotifier(Properties environment){
		try {
			conn = new Connection(environment.getProperty("remote.host"));
			conn.connect();	
			String password = PasswordField.readPassword("Enter password for "+environment.getProperty("remote.user")+"@"+environment.getProperty("remote.host")+ ": ");
			boolean isAuthenticated = conn.authenticateWithPassword(environment.getProperty("remote.user"), password);
			if (isAuthenticated == false){				
				throw new IOException("Authentication failed.");				
			}
			Session sess = conn.openSession();
			sess.execCommand("tail -f "+environment.getProperty("logfile.path"));
			InputStream stdout = new StreamGobbler(sess.getStdout());
			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
			System.out.println("User Authticated");
			System.out.println("Type exit to quit the application");
			String previousLine = "";
			String errorMesg = "";
			String mailMessage = "";
			boolean error = false;
			ExitThread etObj = new ExitThread();
		    etObj.notifier = this;
		    Thread exitThread = new Thread(etObj);
		    exitThread.start();
			while (execute){
				errorMesg = "";				
				String line = br.readLine();
				if(line != null){
					if(!(line.equals("") || line.equals("\n") || line.startsWith("INFO") || line.startsWith("DEBUG") || line.startsWith("bean") || line.startsWith("request") || line.startsWith("http") ||line.startsWith("atomics") || line.startsWith("num"))){
						if(!(line.startsWith("Jan") || line.startsWith("Feb") || line.startsWith("Mar") || line.startsWith("Apr") || line.startsWith("May") || line.startsWith("Jun") || line.startsWith("Jul") || line.startsWith("Aug") || line.startsWith("Sep") || line.startsWith("Oct") || line.startsWith("Nov") || line.startsWith("Dec"))){
							error = true;
							if(line.startsWith("SEVERE")){
								errorMesg = errorMesg + "\n" + previousLine;								
							}
							errorMesg = errorMesg + "\n" + line;
						}
					}
					if(line.startsWith("INFO") || line.startsWith("DEBUG")){
						error = false;
					}
					previousLine = line;
				}
				if(error == true){
					mailMessage = mailMessage + errorMesg;
				}
				if(error == false && mailMessage != null && !(mailMessage.equals(""))){
					javax.mail.Session session = javax.mail.Session.getInstance(environment, null);
					//session.setDebug(true);
					Message mesg = new MimeMessage(session);
					mesg.setFrom(new InternetAddress(environment.getProperty("mail.from.id")));
					mesg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(environment.getProperty("mail.to.ids"),false));
					mesg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(environment.getProperty("mail.to.cc.ids"),false));
					mesg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(environment.getProperty("mail.to.bcc.ids"),false));
					mesg.setSubject("Error looged in "+environment.getProperty("logfile.path")+" file in server "+environment.getProperty("remote.host"));
					mesg.setSentDate(new Date());
					mesg.setText(mailMessage);
				    Transport.send(mesg);
					mailMessage = "";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			conn.close();
			System.exit(0);
		}
		if(conn != null){
			conn.close();
		}
	}
	
	public static void main(String args[]){
		try {
			String resources = args[0];
			if(resources != null && !(resources.equals(""))){
				String propFileLoc = "";
				File f = new File(resources);
				BufferedReader br = new BufferedReader(new FileReader(f));
				while((propFileLoc = br.readLine()) != null){
					Properties envProp = new Properties();
					envProp.load(new FileInputStream(propFileLoc));
					ErrorNotifier notifier = new ErrorNotifier();
					notifier.startErrorNotifier(envProp);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}