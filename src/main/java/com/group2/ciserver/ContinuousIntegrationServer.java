package com.group2.ciserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import org.json.JSONObject;
import org.openl.rules.repository.git.MergeConflictException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;

/**
 * This class represents a Continuous Integration (CI) server which acts as webhook.
 * See the Jetty documentation for API documentation of those classes.
 */
public class ContinuousIntegrationServer extends AbstractHandler {

    /**
    * Runs the tests for the CI server. This is accomplished by executing tests in a Maven environment
    * via command line through the ProcessBuilder class and checking if all tests pass.
    *
    * @param directory         the directory containing the Maven environment
    * @param processBuilder    the process builder instance that runs commands
    * @return                  true if all tests are succesful
    * @see ProcessBuilder 
    */
    public static boolean runTests(File directory, ProcessBuilder processBuilder) {
        boolean testsPassed = false;

        try {
            processBuilder.directory(directory);

            processBuilder.command("bash", "-c", "mvn test");

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
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

    /**
    * Get payload from request and returns it in the form of a JSONObject.
    * 
    * @param reader    BufferedReader containing the payload.
    * @return          payload data as a JSONObject
    * @see JSONObject
    */
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
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean compileCode(File directory, ProcessBuilder processBuilder) {

        try {
            processBuilder.directory(directory);
            processBuilder.command("bash", "-c", "mvn clean compile");
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            Boolean compiled = false;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                if (line.contains("BUILD SUCCESS")) {
                    compiled = true;
                }
            }
            return compiled;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean setCommitStatus(String repoOwner, String repoName, String commitSHA, String state,
            String description, String accessToken) {
        boolean commitStatusSet = false;
        try {
            JSONObject json = new JSONObject();

            if (state == " error" || state == "failure" || state == "pending" || state == "success") {
                json.put("state", state);
            } else {
                return commitStatusSet;
            }
            if (!description.isBlank()) {
                json.put("description", description);
            }
            json.put("context", "ci-server");

            URI uri = new URI("https://api.github.com/repos/" + repoOwner + "/" + repoName + "/statuses/" + commitSHA);
            URL url = uri.toURL();
            HttpURLConnection UrlCon = (HttpURLConnection) url.openConnection();

            UrlCon.setRequestMethod("POST");
            UrlCon.setRequestProperty("Authorization", "Bearer " + accessToken);
            UrlCon.setRequestProperty("Accept", "application/vnd.github.v3+json");
            UrlCon.setRequestProperty("Content-Type", "application/json");
            UrlCon.setDoOutput(true);

            OutputStream outStream = UrlCon.getOutputStream();
            outStream.write(json.toString().getBytes());
            outStream.flush();
            outStream.close();

            int responseCode = UrlCon.getResponseCode();

            if (responseCode == 201) {
                commitStatusSet = true;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return commitStatusSet;
    }

    public static boolean pullBranch(File directory, ProcessBuilder processBuilder, String branchP) {
        try {

            Git git = Git.open(directory);
            String branch = git.getRepository().getBranch();

            if (!branch.equals(branchP)) {
                try {
                    git.checkout().setName(branchP).call();
                } catch (Exception e) {
                    git.checkout().setCreateBranch(true).setName(branchP).setStartPoint("origin/" + branchP).call();
                }
            }

            git.pull().call();

            return compileCode(directory, processBuilder);
        } catch (MergeConflictException e) {
            System.out.println("merge conflict during pull: " + e.getMessage());
            return false;
        } catch (IOException | GitAPIException e) {
            System.out.println("Error during pull: " + e.getMessage());
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

        String accessToken = "to be changed";
        if (accessToken == "to be changed") {
            System.out.println("Failed to insert access token in handle()");
        }

        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code

        BufferedReader reader = request.getReader();
        JSONObject json = getPayload(reader);
        CompletableFuture.runAsync(() -> processCIJob(json, accessToken));
    }

    public static void processCIJob(JSONObject json, String accessToken) {
        try {
            if (json.has("repository")) {

                if (json.getJSONObject("repository").has("clone_url")) {
                    File dir = new File("D:\\Github\\github\\server");
                    boolean cloned = cloneRepo(json.getJSONObject("repository").getString("clone_url"),
                            dir);
                    ProcessBuilder processBuilder = new ProcessBuilder();
                    if (cloned) {
                        // compile the code
                        System.out.println("compiling the code");

                        Boolean compiled = compileCode(dir, processBuilder);
                        if (compiled) {
                            System.out.println("compiled!");
                            // test the code
                            boolean passedTests = runTests(dir, processBuilder);

                            if (passedTests) {
                                System.out.println("Passed test!");
                            } else {
                                System.out.println("Failed to set commit status");
                            }
                        } else {
                            System.out.println("not compiled!");
                        }

                    } else {

                        // pull the code from the branch that the code was pushed to
                        String owner = json.getJSONObject("repository").getJSONObject("owner").getString("name");
                        String repo = json.getJSONObject("repository").getString("name");
                        String commitSHA = json.getString("after");
                        System.out.println("Clone exists, trying pull");
                        String branchName = json.getString("ref").replaceFirst("refs/heads/", "");

                        Boolean pulled = pullBranch(dir, processBuilder, branchName);
                        if (pulled) {
                            Boolean compiled = compileCode(dir, processBuilder);
                            // notify the status
                            if (compiled) {
                                System.out.println("compiled!");
                                // test the code
                                boolean passedTests = runTests(dir, processBuilder);
                                if (passedTests) {
                                    String status = "success";
                                    String desc = "All tests passed and the code compiled!";
                                    System.out.println(desc);
                                    boolean setStatus = setCommitStatus(owner, repo, commitSHA, status, desc,
                                            accessToken);
                                    if (!setStatus) {
                                        System.out.println("Failed to set commit status");
                                    }
                                } else {
                                    String status = "failure";
                                    String desc = "One or more tests failed but the code compiled!";
                                    System.out.println(desc);
                                    boolean setStatus = setCommitStatus(owner, repo, commitSHA, status, desc,
                                            accessToken);
                                    if (!setStatus) {
                                        System.out.println("Failed to set commit status");
                                    }
                                }
                            } else {
                                System.out.println("not compiled!");
                                String status = "failure";
                                String desc = "Failed to compile the code!";
                                System.out.println(desc);
                                boolean setStatus = setCommitStatus(owner, repo, commitSHA, status, desc,
                                        accessToken);
                                if (!setStatus) {
                                    System.out.println("Failed to set commit status");
                                }
                            }
                        } else {
                            System.out.println("Pull failed!");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing CI job: " + e.getMessage());
        }

    }

    // used to start the CI server in command line
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer());
        server.start();
        server.join();
    }
}
