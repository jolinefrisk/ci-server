package com.group2.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONObject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * Skeleton of a ContinuousIntegrationServer which acts as webhook
 * See the Jetty documentation for API documentation of those classes.
 */
public class ContinuousIntegrationServer extends AbstractHandler {

    public static boolean runTests(File directory, ProcessBuilder processBuilder) {
        boolean testsPassed = false;

        try {
            processBuilder.directory(directory);

            processBuilder.command("bash", "-c", "mvn test");
            
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("Failures: 0, Errors: 0, Skipped: 0")) {
                    testsPassed = true;
                }
            }
            return testsPassed;
        } catch (Exception e) {
                System.out.println(e.getMessage());
                return testsPassed;
        }
    }

    public static JSONObject getPayload(BufferedReader reader) {
        StringBuilder jsonData = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
            JSONObject json = new JSONObject(jsonData.toString());
            return json;

        } catch (IOException | org.json.JSONException e) {
            return new JSONObject();
        }
    }

    public static boolean cloneRepo(String url, File directory) {

        if (directory.exists()) {
            System.out.println("Directory does already exist " + directory);
            return false;
        }
        try {
            Git.cloneRepository().setURI(url).setDirectory(directory).call();
            return true;
        } catch (GitAPIException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);

        System.out.println(target);

        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code

        BufferedReader reader = request.getReader();
        JSONObject json = getPayload(reader);
        if (json.has("repository")) {
            if (json.getJSONObject("repository").has("clone_url")) {
                File dir = new File("D:\\Github\\github\\server");
                boolean cloned = cloneRepo(json.getJSONObject("repository").getString("clone_url"),
                        dir);
                if (cloned) {
                    // compile the code
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    // test the code
                    boolean passedTests = runTests(dir, processBuilder);
                    if (passedTests) {
                        System.out.println("All tests passed!");
                        // Notify the status
                    } else {
                        System.out.println("One or more tests failed!");
                        // Notify the status
                    }

                } else {
                    // pull the code from the branch that the code was pushed to
                    // compile the code
                    // test the code
                    // notify the status
                }
            }
        }
        response.getWriter().println("CI job done");
    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer());
        server.start();
        server.join();
    }
}
