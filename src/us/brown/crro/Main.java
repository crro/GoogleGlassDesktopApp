package us.brown.crro;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Future;

/**
 * This is the main class that performs all of the AppleScript interaction.
 * Created by David Correa on 09/30/14
 */
public class Main {

    /**
     * These are the file paths of the scripts corresponding to the possible actions to be executed.
     */
    public static final String START_PRESENTATION = "/Users/David/Development/scripts/RunSlideShow.txt";
    public static final String NEXT_SLIDE = "/Users/David/Development/scripts/NextSlide.txt";
    public static final String PREVIOUS_SLIDE = "/Users/David/Development/scripts/PreviousSlide.txt";
    public static final String END_PRESENTATION = "/Users/David/Development/scripts/ExitSlideShow.txt";
    public static final String GET_NOTES = "/Users/David/Development/scripts/FINALNotesPPTX.txt";
    public static final String GET_POSITION = "/Users/David/Development/scripts/CurrentPosition.scpt";

    //These are the presentation notes in text.
    public static String presentationNotes = "";

    /**
     * This class is in charge of executing the AppleScript located in the pathScript.
     * @param pathScript - path to the script
     * @throws IOException
     * @throws ScriptException
     */
    public static void executePresentationTask(String pathScript) throws IOException, ScriptException {
        BufferedReader br = new BufferedReader(new FileReader(pathScript));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        String script = sb.toString();
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("AppleScript");
        engine.eval(script);
    }
    /**
     * This function returns the current position of the active presentation.
     * It is called by a timer in charge of keeping track of the current status of the presentation
     */
    public static int getCurrentIndex() throws IOException, ScriptException {
        BufferedReader br = new BufferedReader(new FileReader(GET_POSITION));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append("\n");
            line = br.readLine();
        }
        String script = sb.toString();
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("/usr/bin/osascript");
        cmd.add(GET_POSITION);
        String[] cmdArray = (String[]) cmd.toArray(new String[0]);
        Process result = Runtime.getRuntime().exec(cmdArray);
        try {
            result.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        line = null;
        StringBuffer output = new StringBuffer();
        if (result.exitValue() != 0) {
            System.out.println("Presentation is not in presentation mode.");
            //We return negative one because there isn't a new presentation
            return -1;
        } else {
             /* read in the output */
            BufferedReader out = new BufferedReader(new
                    InputStreamReader(result.getInputStream()));
            while ((line = out.readLine()) != null) {
                output.append(line);
            }
            int position = Integer.parseInt(output.toString());
            //we return postion once we have an active presentation
            return position;
        }
    }

    /**
     * This script gets all the notes from the presentation and puts them in the instance variable
     * presentationNotes.
     */
    public static void getAllNotes() throws IOException, ScriptException {
        //Create the file to store the notes if it doesn't exits
        File file = new File("/Users/David/Desktop/Notes.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        executePresentationTask(GET_NOTES);

        BufferedReader reader = new BufferedReader(new FileReader("/Users/David/Desktop/Notes.txt"));
        String line = null;
        while ((line = reader.readLine()) != null) {
            presentationNotes = presentationNotes + line + "\n";
        }
    }

    /**
     * Main line method, this is where it all begins.
     */
    public static void main(String[] args) {

        URI uri = URI.create("ws://googleglassserver.herokuapp.com/incoming/");
        try {
            // We first get all the notes
            getAllNotes();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        // We create the socket
        WebSocketClient client = new WebSocketClient();
        try
        {
            client.start();
            // The socket that receives events
            PowerPointSocket socket = new PowerPointSocket();
            // Attempt Connect
            Future<Session> fut = client.connect(socket,uri);
            // Wait for Connect
            Session session = fut.get();
            // Send a message
            session.getRemote().sendString("Join tkraska");
            //new ImageUploader();

        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }
}
