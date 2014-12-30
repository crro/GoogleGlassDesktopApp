package us.brown.crro;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.PathContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This class has no usefulness to the application as a whole but it was a great example of how to
 * get an image to the server and back. I used it to implement the display of images on the glass
 * device. I kept it for future reference.
 * Created by David on 11/3/14.
 */
public class ImageUploader {
    public ImageUploader() {
        String math = "\\sum\\limits_{\\beta=0}^{\\infty} \\Alpha^{\\pi} {n \\choose n+1} \\text{yolo}";

        TeXFormula fomule = new TeXFormula(math);
        TeXIcon ti = fomule.createTeXIcon(
                TeXConstants.STYLE_DISPLAY, 40);
        BufferedImage b = new BufferedImage(ti.getIconWidth(), ti
                .getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        ti.paintIcon(new JLabel(), b.getGraphics(), 0, 0);

        try {
            File outputfile = new File("/Users/David/Desktop/saved.png");
            ImageIO.write(b, "png", outputfile);
            //Now I'm going to try and put it into an http request
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            int status;
            Request request = httpClient.POST("http://googleglassserver.herokuapp.com");
            //request.file(Paths.get("/Users/David/Desktop/saved.png")); take this out since we are not posting anymore
            request.header(HttpHeader.CONNECTION, "Keep-Alive");
            //Request r = request.content(new PathContentProvider(outputfile.toPath()), "image/png");
            request.param("Equation", math);
            ContentResponse response = request.param("Action", "GET IMAGE").send();
            String responseTxt;
            status = response.getStatus();
            if (status != 200) {
                responseTxt = "Unabled to fetch image";
            } else {
                byte[] content = response.getContent();
                InputStream in = new ByteArrayInputStream(content);
                BufferedImage bImage = ImageIO.read(in);

                ImageIO.write(bImage, "png", new File("/Users/David/Desktop/new-saved.png"));
            }


        } catch (IOException e) {
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
}
