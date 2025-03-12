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
import org.eclipse.jgit.api.errors.RefNotFoundException;

import java.io.File;

/**
 * This class represents a Continuous Integration (CI) server which acts as
 * webhook.
 * See the Jetty documentation for API documentation of those classes.
 */
public class ContinuousIntegrationServer extends AbstractHandler {

    /**
     * Runs the tests for the CI server. This is accomplished by executing tests in
     * a Maven environment
     * via command line through the ProcessBuilder class and checking if all tests
     * pass.
     *
     * @param directory      the directory containing the Maven environment
     * @param processBuilder the process builder instance that runs commands
     * @return true if all tests are succesful
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
     * @param reader BufferedReader containing the payload.
     * @return payload data as a JSONObject
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

    /**
     * Clones a Git repository into the specified directory. This is accomplished
     * using JGit's cloneRepository() method to retrieve the repository from the
     * given URL.
     *
     * @param url       the URL of the Git repository to be cloned
     * @param directory the directory where the repository should be cloned
     * @return true if the cloning was successful, false otherwise
     * @see org.eclipse.jgit.api.Git#cloneRepository()
     */
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

    /**
     * Compiles the source code in the specified directory using Maven.
     * This is accomplished by executing the "mvn clean compile" command in a Bash
     * enviroment
     * via the ProcessBuilder class and checking if the build is successful.
     *
     * @param directory      the directory containing the Maven project to be
     *                       compiled
     * @param processBuilder the process builder instance that runs the compilation
     *                       command
     * @return true if the compilation is successful, false otherwise
     * @see ProcessBuilder
     */
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

    /**
     * Updates the status of a specific commit on GitHub using the GitHub API.
     * This method sends a POST request to the repository's commit status endpoint
     * with the specified state and description.
     *
     * @param repoOwner   the owner of the GitHub repository
     * @param repoName    the name of the GitHub repository
     * @param commitSHA   the SHA hash of the commit to update
     * @param state       the status to set for the commit (must be "error",
     *                    "failure", "pending", or "success")
     * @param description a brief description of the commit status
     * @param accessToken a GitHub personal access token with repo permissions
     * @return true if the status update is successful, false otherwise
     * @see <a href=
     *      "https://docs.github.com/en/rest/commits/statuses?apiVersion=2022-11-28">GitHub
     *      Commit Status API</a>
     */
    public static boolean setCommitStatus(String repoOwner, String repoName, String commitSHA, String state,
            String description, String accessToken) {
        boolean commitStatusSet = false;
        try {
            JSONObject json = new JSONObject();

            if (state == "error" || state == "failure" || state == "pending" || state == "success") {
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

    /**
     * Pulls the latest changes from the specified branch of a Git repository.
     * If the branch does not exist locally, it attempts to create and track it from
     * the remote repository.
     * After pulling, it returns true.
     *
     * @param directory      the directory containing the Git repository
     * @param processBuilder the process builder instance used for compilation
     * @param branchP        the name of the branch to pull
     * @return true if the pull and compilation are successful, false otherwise
     * @see Git#pull()
     */
    public static boolean pullBranch(File directory, String branchP) {
        if (branchP == null || branchP.isEmpty()) {
            System.out.println("Invalid branch name.");
            return false;
        }
        
            try(Git git = Git.open(directory);) {     
                String branch = git.getRepository().getBranch();
                git.fetch()
                .setRemote("origin")
                .call();
    
                boolean branchExists = git.lsRemote()
                .setRemote("origin")
                .call()
                .stream()
                .anyMatch(ref -> ref.getName().equals("refs/heads/" + branchP));
    
                if (!branchExists) {
                    System.out.println("Remote branch does not exist: " + branchP);
                    return false;
                }
    
                if (!branch.equals(branchP)) {
                    try {
                        git.checkout()
                        .setName(branchP)
                        .call();
                    } catch (RefNotFoundException e) {
                        git.checkout()
                        .setCreateBranch(true)
                        .setName(branchP)
                        .setStartPoint("origin/" + branchP)
                        .call();
                        System.out.println("Checked out new branch: " + branchP);
                    }
                }
    
                git.pull().call();
                System.out.println("Pulled latest changes into branch: " + branchP);
                return true;
            } catch (MergeConflictException e) {
                System.out.println("merge conflict during pull: " + e.getMessage());
    
            } catch (IOException | GitAPIException e) {
                System.out.println("Error during pull: " + e.getMessage());
    
            }
            return false;
    }

    public static String getRepoName(String repoUrl) {
        // Remove trailing ".git" if it exists
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 4);
        }
    
        // Extract the repository name after the last '/'
        return repoUrl.substring(repoUrl.lastIndexOf('/') + 1);
    }
    /**
     * Handles incoming HTTP requests for the CI server. This method processes
     * webhook payloads,
     * sets response headers, and asynchronously executes continuous integration
     * tasks.
     * The CI tasks may include cloning the repository, compiling the code, and
     * running tests.
     *
     * @param target      the request target (URL path)
     * @param baseRequest the original Jetty request
     * @param request     the HTTP servlet request containing the webhook payload
     * @param response    the HTTP servlet response
     * @throws IOException      if an input or output error occurs while handling
     *                          the request
     * @throws ServletException if the request could not be handled
     * @see #processCIJob(JSONObject, String)
     */
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

        BufferedReader reader = request.getReader();
        JSONObject json = getPayload(reader);
        CompletableFuture.runAsync(() -> processCIJob(json, accessToken));
    }

    /**
     * Processes a continuous integration (CI) job based on the webhook payload.
     * This method performs the following steps:
     * <ol>
     * <li>Clones the repository if it does not already exist.</li>
     * <li>If cloning fails, attempts to pull the latest changes from the pushed
     * branch.</li>
     * <li>Compiles the code using Maven.</li>
     * <li>Runs the test suite if compilation is successful.</li>
     * <li>Updates the commit status on GitHub based on the results.</li>
     * </ol>
     *
     * @param json        the JSON payload received from the webhook, containing
     *                    repository and commit details
     * @param accessToken the GitHub access token used for authentication in API
     *                    requests
     * @see #cloneRepo(String, File)
     * @see #pullBranch(File, ProcessBuilder, String)
     * @see #compileCode(File, ProcessBuilder)
     * @see #runTests(File, ProcessBuilder)
     * @see #setCommitStatus(String, String, String, String, String, String)
     */
    public static void processCIJob(JSONObject json, String accessToken) {
        try {
            if (json.has("repository")) {

                if (json.getJSONObject("repository").has("clone_url")) {
                    String repoUrl = json.getJSONObject("repository").getString("clone_url");
                    String repoName = getRepoName(repoUrl);
                    File dir = new File(System.getProperty("user.home") + "/Github/test-repo");
                    System.out.println("Cloning from: " + repoUrl);
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

                        Boolean pulled = pullBranch(dir, branchName);
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
                                        System.out.println("Test Success butFailed to set commit status");
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

    /**
     * The entry point for starting the Continuous Integration (CI) server.
     * This method initializes a Jetty server on port 8080, sets the request
     * handler,
     * and starts the server to listen for incoming webhook events.
     *
     * @param args command-line arguments
     * @throws Exception if the server fails to start or encounters an error
     * @see Server
     * @see ContinuousIntegrationServer
     */
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new ContinuousIntegrationServer());
        server.start();
        server.join();
    }
}
