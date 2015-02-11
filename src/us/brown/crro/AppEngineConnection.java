package us.brown.crro;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by David on 2/7/15.
 */
public class AppEngineConnection {
    /*
    So the proitocol is as follows:

    - Once we have the code we send it to the server along with the notes
    - POST Notes - Once
    - POst index - occasionally
    - Get Index - periodically
     */
    private HttpClient _httpClient;
    private String _sessionCode;
    private Timer _timerIndex; //This one checks remotely and updates locally
    private Timer _timerUpdate; //This one checks locally and updates remotely

    private int _currentIndex;

    public AppEngineConnection(String code) throws Exception {
        _sessionCode = code;
        _httpClient = new HttpClient();
        _httpClient.start();
        _currentIndex = 0;
        //The timer that checks for the current position and updates the server.
        _timerUpdate = new Timer(5, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    _timerUpdate.stop();
                    int index = Main.getCurrentIndex() - 1; //We subtract one since the position returned is 1 indexed
                    if (index >= 0 && index != _currentIndex) {
                        //Make an HTTP request to update the current index
                        updateIndexRemote(index);
                        _currentIndex = index;
                    }
                    _timerUpdate.start();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (ScriptException e1) {
                    e1.printStackTrace();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        //This timer checks the server and updates locally
        _timerIndex = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    _timerIndex.stop();
                    updateIndexLocal();
                    _timerIndex.start();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    public void startTimers() {
        _timerIndex.start();
        _timerUpdate.start();
    }
    public void stopTimers() {
        _timerIndex.stop();
        _timerUpdate.stop();
    }

    public void publishNotes() {
        //We send the notes abroad.
        try {
            Request req = _httpClient.POST("http://1-dot-firm-aria-738.appspot.com/desktopappserver").param("SESSION", _sessionCode).param("ACTION", "NOTES");
            File notes = Main.getAllNotes();
            req = req.content(new StringContentProvider(Main.presentationNotes), "text/plain");
            ContentResponse resp = req.send();
            String responseTxt;
            int status = resp.getStatus();
            if (status != 200) {
                responseTxt = "Unabled to fetch notes";
            } else {
                //We dont do anything once we posted the notes
                responseTxt = "Notes published";
                //If we are successful we send the images to the server
                processNotes(Main.presentationNotes);
            }
            System.out.println(responseTxt);
        }   catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This method process the notes in order to preload the necesary equations and store them
     * in a Hashtable.
     * @param notes
     */
    public void processNotes(String notes) {
        System.out.println("Processing: " + notes);
        String[] notesWords = notes.split("\n");
        for (String note : notesWords) {
            BufferedImage bImage = null;
            String equation = null;
            if (note.contains("PROCSLIDE")) {
                //then we change the slide
                if (note.contains("<<")) {
                    String[] notesDivided = note.split("<<");
                    //we generate the image here and add it to the HashTable
                    equation = notesDivided[1].substring(0, notesDivided[1].length() - 2);
                    bImage = Main.createImage(equation);
                }
            } else if (note.contains("<<") && note.contains(">>")) {
                //we post and get an image to post
                equation = note.substring(2, note.length() - 2);
                bImage = Main.createImage(equation);
            }
            if (bImage != null && equation != null) {
                //Send it to server
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(bImage, "png", baos);
                    byte[] bytes = baos.toByteArray();
                    //We generate the request and send the file to the server to the given session
                    //equation = URLEncoder.encode(equation, "UTF-8"); //We do not encode so that we match
                    System.out.println("Equation posted to session" + _sessionCode + "with key: " + equation);
                    Request req = _httpClient.POST("http://1-dot-firm-aria-738.appspot.com/desktopappserver").param("SESSION", _sessionCode).param("ACTION", "IMAGE")
                            .param("EQUATION", equation);

                    req = req.content(new BytesContentProvider(bytes), "image/png");
                    ContentResponse resp = req.send();
                    String responseTxt;
                    int status = resp.getStatus();
                    if (status != 200) {
                        responseTxt = "Unabled to push image";
                    } else {
                        //We dont do anything once we posted the notes
                        responseTxt = "Image Published";
                        //If we are successful we send the images to the server
                        //processNotes(Main.presentationNotes);
                    }
                    System.out.println(responseTxt);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void updateIndexLocal() {
        try {
            ContentResponse response = _httpClient.GET("http://1-dot-firm-aria-738.appspot.com/desktopappserver?ACTION=INDEX&SESSION="+_sessionCode);
            //Now we just get the index and update the presentation.
            String index = response.getContentAsString();
            if (index.equals("")) {
                return;
            }
            int newIndex = Integer.parseInt(index.trim());

            // We can only go to the next slde or to the previous, so we need to figure out the current location and
            //then move it only based on that.
            int currIndex = Main.getCurrentIndex() - 1;//The position returned by the index is 1-indexed.
            while (currIndex != newIndex && currIndex > 0) {
                if (currIndex > newIndex) {
                    Main.executePresentationTask(Main.PREVIOUS_SLIDE);
                    currIndex--;
                } else {
                    //currIndex < newIndex
                    Main.executePresentationTask(Main.NEXT_SLIDE);
                    currIndex++;
                }
            }
            System.out.println("Updated locally");
        }   catch (IOException e) {
        e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateIndexRemote(int index) {
        try {
            String size = String.valueOf(index);
            Request req = _httpClient.POST("http://1-dot-firm-aria-738.appspot.com/desktopappserver").param("SESSION", _sessionCode)
                    .param("ACTION", "INDEX").param("INDEX", size).header(HttpHeader.CONTENT_LENGTH, String.valueOf(size.length()));
            req = req.content(new StringContentProvider(size));
            ContentResponse resp = req.send();
            String responseTxt;
            int status = resp.getStatus();
            if (status != 200) {
                responseTxt = "Unabled to update the index remotely";
            } else {
                //We dont do anything once we posted the index
                responseTxt = "Index published";
            }
            System.out.println(responseTxt);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
