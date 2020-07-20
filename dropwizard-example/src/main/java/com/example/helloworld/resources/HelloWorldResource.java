package com.example.helloworld.resources;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.example.helloworld.api.Saying;
import com.example.helloworld.core.Template;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.params.DateTimeParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.net.*;


@Path("/")
@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
public class HelloWorldResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldResource.class);

    private final Template template;
    private final AtomicLong counter;
    private URL url;
    private InputStream is;
    private FileOutputStream fos;

    public HelloWorldResource(Template template) {
        this.template = template;
        this.counter = new AtomicLong();
    }

    @GET
    @Timed(name = "get-requests-timed")
    @Metered(name = "get-requests-metered")
    @CacheControl(maxAge = 1, maxAgeUnit = TimeUnit.DAYS)
    public String sayHello(@QueryParam("name") Optional<String> name) {
        
        if (name.isPresent()) {

            //ex: "https://github.com/AlexeyAB/darknet"
            
            try{
                this.url = new URL(name.get() + "/archive/master.zip");
            }catch(MalformedURLException ex){
                //do exception handling here
            }
            
            double d_random = (Math.random()*((10000-1)+1))+1;
            int int_rand = (int)d_random;
            File file = new File("file-" + String.valueOf(int_rand)  + ".zip");

            List<String> fileExtension = new ArrayList<>();
            long[][] total = new long[500][3];
            // CONSTANT
            int TOTAL_BYTES = 0;
            int TOTAL_LINES = 1;
            int TOTAL_FILES = 2;

            try{
                this.is = this.url.openStream();
            }catch(IOException ex){
                //do exception handling here
            }

            try {
                this.fos = new FileOutputStream(file);
            }catch(FileNotFoundException ex) {
                //
            }
            

            int bytes = 0;

            try {
                while ((bytes = this.is.read()) != -1) {
                    this.fos.write(bytes);
                }
                this.is.close();
                this.fos.close();
            }catch(IOException ex) {
                //
            }
            
            // map all files on the zip file
            try {
                ZipFile zf = new ZipFile(file);

                //System.out.println(String.format("Inspecting contents of: %s\n", zf.getName()));

                Enumeration<? extends ZipEntry> zipEntries = zf.entries();
                zipEntries.asIterator().forEachRemaining(entry -> {

                    // extension
                    String extension = "";
                    int i = entry.getName().lastIndexOf('.');
                    if (i > 0) {
                        extension = entry.getName().substring(i+1);
                    } else {
                        extension = "";
                    }

                    // content (Encoding, Lines)
                    long lineCount = 0;
                    String defaultEncoding = "";
                    try {
                        InputStream stream = zf.getInputStream(entry);

                        InputStreamReader defaultReader = new InputStreamReader(stream);
                        defaultEncoding = defaultReader.getEncoding();

                        byte[] buffer = new byte[8192];
                        long count = 1;
                        int n;
                        while ((n = stream.read(buffer)) > 0) {
                            for (int line = 0; line < n; line++) {
                                if (buffer[line] == '\n') count++;
                            }
                        }
                        if (count > 0) {
                            lineCount = count;
                        }

                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Process Count

                    // List file-extension
                    if (fileExtension.contains(extension)){
                        // sum
                        total[ fileExtension.indexOf(extension) ][TOTAL_BYTES] += (long)entry.getSize();
                        total[ fileExtension.indexOf(extension) ][TOTAL_LINES] += lineCount;
                        total[ fileExtension.indexOf(extension) ][TOTAL_FILES] += 1;

                    } else if (extension != "" && !extension.contains("/")) {
                        // add
                        fileExtension.add(extension);

                        // init
                        total[ fileExtension.indexOf(extension) ][TOTAL_BYTES] = (long)entry.getSize();
                        total[ fileExtension.indexOf(extension) ][TOTAL_LINES] = lineCount;
                        total[ fileExtension.indexOf(extension) ][TOTAL_FILES] = 1;
                    }

                });

                // Json
                //System.out.println("Initial ArrayList: {} " + fileExtension);
                //System.out.println(Arrays.deepToString(total));

            } catch (IOException e) {
                e.printStackTrace();
            }

            // prepare to print
            String output = "{";
            for (String extension : fileExtension) {
                output += '"' + extension + '"' +  ":{\"total_bytes\":" + total[ fileExtension.indexOf(extension) ][TOTAL_BYTES] + ",\"total_lines\":" + total[ fileExtension.indexOf(extension) ][TOTAL_LINES] + "},";
            }
            output += "}";

            return output.replace(",}","}");
        } else {
            return "ERROR! Is missing the parameter name, ex: ?name=https://github.com/indrekots/events-service";
        }
        
    }
 
}
