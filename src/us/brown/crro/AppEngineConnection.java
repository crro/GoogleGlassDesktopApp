package us.brown.crro;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;

import java.io.IOException;
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

    public AppEngineConnection() {
        _httpClient = new HttpClient();
    }

    public void publishNotes(String note, String code) {
        //We send the notes abroad.
        try {
            Request req = _httpClient.POST("http://localhost:888/desktopserver").param("SESSION", code).param("ACTION", "NOTES");
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

    public void getIndex() {
        try {
            ContentResponse response = _httpClient.GET("http://localhost:888/desktopserver?ACTION=INDEX&SESSION="+_sessionCode);
            //Now we just get the index and update the presentation.
            String index = response.getContentAsString();
            index = index.trim();
            Main.
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

    public void postIndex(int index) {

    }

}
