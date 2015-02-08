package us.brown.crro;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;

/**
 * Created by David on 2/6/15.
 */
public class Manager extends JFrame {

    public Manager() {

        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.add(new MainPanel());
        this.pack();
        this.setVisible(true);
    }

    public class MainPanel extends JPanel {
        private JLabel _label;
        private JTextArea _textArea;
        private JButton _button;

        public MainPanel() {
            this.setPreferredSize(new Dimension(250, 75));
            this.setLayout(new BorderLayout());
            _button = new JButton("ENTER");
            _label = new JLabel("Please enter the code that appears in the Glass App");
            _textArea = new JTextArea();

            _button.addActionListener(new ButtonListener());

            this.add(_label, BorderLayout.NORTH);
            this.add(_textArea, BorderLayout.CENTER);
            this.add(_button, BorderLayout.SOUTH);
        }

        private class ButtonListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent evß) {
                String code = _textArea.getText();
                URI uri = URI.create("ws://googleglassserver.herokuapp.com/incoming/");
                try {
                    // We first get all the notes
                    Main.getAllNotes();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ScriptException e) {
                    e.printStackTrace();
                }
                // This now has to sned them to app engine.
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
    }

}
