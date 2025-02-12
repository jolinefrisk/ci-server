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
import org.openl.rules.repository.git.MergeConflictException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;


/**
 * Skeleton of a ContinuousIntegrationServer which acts as webhook
 * See the Jetty documentation for API documentation of those classes.
 */
public class ContinuousIntegrationServer extends AbstractHandler {

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

    public static boolean compileCode(File directory, ProcessBuilder processBuilder, Boolean bash) {

        try {
            processBuilder.directory(directory);
            if (bash) {
                processBuilder.command("bash", "-c", "mvn clean compile");
            } else {
                processBuilder.command("cmd.exe", "/c", "mvn clean compile");
            }

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

    public static boolean pullBranch(File directory, ProcessBuilder processBuilder, String branchP){
        try{

            Git git = Git.open(directory);
            String branch = git.getRepository().getBranch();

            if (!branch.equals(branchP)){
                try {
                    git.checkout().setName(branchP).call();
                } catch (Exception e) {
                    git.checkout().setCreateBranch(true).setName(branchP).setStartPoint("origin/" + branchP).call();
                }
            }

            git.pull().call();
 
            return compileCode(directory, processBuilder, false);
        }catch (MergeConflictException e){
            System.out.println("merge conflict during pull: "+ e.getMessage());
            return false;
        }catch (IOException | GitAPIException e){
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

        // here you do all the continuous integration tasks
        // for example
        // 1st clone your repository
        // 2nd compile the code

        BufferedReader reader = request.getReader();
        JSONObject json = getPayload(reader);

        /*if (json.has("pull_request")){
        JSONObject pullRequest = json.getJSONObject("pull_request");
        String repoUrl = pullRequest.getJSONObject("head").getJSONObject("repo").getString("clone_url");
        String branch = pullRequest.getJSONObject("head").getString("ref");
        boolean cloned = boolean cloned = cloneRepo(branch,url);*/
        if (json.has("repository")) {
            if (json.getJSONObject("repository").has("clone_url")) {
                File dir = new File("D:\\Github\\github\\server");
                boolean cloned = cloneRepo(json.getJSONObject("repository").getString("clone_url"),
                        dir);
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (cloned) {
                    // compile the code
                    response.getWriter().println("compiling the code");
                    
                    Boolean compiled = compileCode(dir, processBuilder, false);
                    if (compiled) {
                        response.getWriter().println("compiled!");
                    } else {
                        response.getWriter().println("not compiled!");
                    }
                    // test the code
                    // notify the status
                } else {
                   
                    // pull the code from the branch that the code was pushed to
            
                    response.getWriter().println("Clone exists, trying pull");
                    String branchName =json.getString("ref").replaceFirst("ref/heads/", "");

                    Boolean compiled = pullBranch(dir, processBuilder, branchName);
                        // test the code
                    // notify the status
                    if (compiled) {
                        response.getWriter().println("compiled!");
                    } else {
                        response.getWriter().println("not compiled!");
                    }
                }
            }
        }
        //}
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
