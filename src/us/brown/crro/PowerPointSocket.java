package us.brown.crro;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import javax.script.ScriptException;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This is the class that models the WebSocket that establishes the permanent connection with the
 * server. Depending on the message received, it executes a particular AppleScript to perform a
 * task.
 *
 * This class also creates two timers. One in charge of keeping the connection open by constantly
 * pinging the server. The other one in charge of constantly requesting the current slide of the
 * presentation
 * Created by David on 10/4/14.
 */
public class PowerPointSocket extends WebSocketAdapter {
    //This was based on a simple EchoSocket
    public Session session;
    private boolean keepConnection = true;
    private Timer _timerConnect;
    private Timer _timerIndex;
    private HttpClient _updateIndexClient;

    /**
     * This method executes when the socket gets connected. It is used to
     * initialize the two timers.
     * @param sess: The established session
     */
    @Override
    public void onWebSocketConnect(Session sess)
    {
        super.onWebSocketConnect(sess);
        session = sess;
        _updateIndexClient = new HttpClient();

        System.out.println("Socket Connected: " + sess);
        //The timer that keep the connection open
        _timerConnect = new Timer(50000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                session.getRemote().sendStringByFuture("PING");
            }
        });
        _timerConnect.start();
        //The timer that checks for the current position and updates the server.
        _timerIndex = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    _timerIndex.stop();
                    int position = Main.getCurrentIndex();
                    if (position >= 0) {
                        //Make an HTTP request to update the current index
                        sendUpdate(position);
                    }
                    _timerIndex.start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (ScriptException e1) {
                    e1.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        //This is just for testing, in reality this is started by the Glass application
        try {
            _updateIndexClient.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _timerIndex.start();
    }

    /**
     * This class sends an HTTP request updating the current position in the server
     * @param position - the new position of the presentation
     * @throws Exception
     */
    public void sendUpdate(int position) throws Exception {
        int status;
        Request request = _updateIndexClient.POST("http://googleglassserver.herokuapp.com");

        int index = position - 1;//Because position take the Cover slide as number one when that one is number 0.
        request.param("Action", "Update Index");
        request.param("Current Index", Integer.toString(index));
        ContentResponse response = request.send();
        String responseTxt = "No Action";
        status = response.getStatus();
        if (status != 200) {
            System.out.println("There was a problem with the server, status code:" + status);
        } else {
            String text = response.getContentAsString();
            System.out.println("Index successfully updated with only one client");
        }
    }

    /**
     * This method gets called when a message is received through the WebSocket.
     * @param message -  The message received
     */
    @Override
    public void onWebSocketText(String message)
    {
        super.onWebSocketText(message);
        System.out.println("Received TEXT message: " + message);
        try {
            String words[] = message.split(" ");
            if (words[0].equals("Action")) {
                if (words[1].equals("Start")) {
                    Main.executePresentationTask(Main.START_PRESENTATION);
                    //This also begins the constant check of status
                    _timerIndex.start();
                } else if (words[1].equals("Next")) {
                    Main.executePresentationTask((Main.NEXT_SLIDE));
                } else if (words[1].equals("Previous")) {
                    Main.executePresentationTask(Main.PREVIOUS_SLIDE);
                } else if (words[1].equals("End")) {
                    Main.executePresentationTask(Main.END_PRESENTATION);
                    _timerIndex.stop();
                    _updateIndexClient.stop();
                } else if (words[1].equals("Notes")) {
                    getSession().getRemote().sendStringByFuture(Main.presentationNotes);
                } else {
                    System.out.println("Invalid command");
                }

           }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ScriptException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason)
    {
        super.onWebSocketClose(statusCode,reason);
        System.out.println("Socket Closed: [" + statusCode + "] " + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause)
    {
        super.onWebSocketError(cause);
        cause.printStackTrace(System.err);
    }

}
