package com.group2.ciserver;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.File;

import java.io.InputStream;


import org.json.JSONObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.*;


import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;


import org.eclipse.jgit.api.errors.GitAPIException;

import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;

import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Files;


/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void positiveTestGetPayload() {
        String jsonInput = "{ \"repository\": { \"clone_url\": \"https://github.com/test/repo.git\" } }";
        BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
        JSONObject json = ContinuousIntegrationServer.getPayload(reader);
        JSONObject expected = new JSONObject(jsonInput);
        assertEquals(expected.toString(), json.toString());
    }

    @Test
    public void negativeTestGetPayload() {
        String jsonInput = "{ \"repository\": { \"clone_url\": \"https://github.com/test/repo.git\" } }";
        BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
        JSONObject json = ContinuousIntegrationServer.getPayload(reader);
        assertNotEquals("hello", json.toString());
    }

    @Test
    public void invalidTestGetPayload() {
        String jsonInput = "hello";
        BufferedReader reader = new BufferedReader(new StringReader(jsonInput));
        JSONObject json = ContinuousIntegrationServer.getPayload(reader);
        assertEquals("{}", json.toString());
    }

    @Test
    public void dirExistTestCloneRepo() {
        File testDir = mock(File.class);
        when(testDir.exists()).thenReturn(true);
        assertFalse(ContinuousIntegrationServer.cloneRepo("https://github.com/test/repo.git", testDir));
    }

    @Test
    public void trueTestCloneRepo() throws GitAPIException {
        File testDir = mock(File.class);
        when(testDir.exists()).thenReturn(false);

        // asked ChatGPT how to test a positive case without actually cloning a real
        // repo
        try (MockedStatic<Git> mockedGit = Mockito.mockStatic(Git.class)) {
            CloneCommand mockCloneCommand = mock(CloneCommand.class);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mock(Git.class));

            mockedGit.when(Git::cloneRepository).thenReturn(mockCloneCommand);

            boolean result = ContinuousIntegrationServer.cloneRepo("https://github.com/test/repo.git", testDir);

            mockedGit.verify(Git::cloneRepository);
            assertTrue(result);
        }

    }

    @Test
    public void falseTestCloneRepo() {
        File testDir = new File("Fake\\path");
        assertFalse(ContinuousIntegrationServer.cloneRepo("hello", testDir));
    }

    @Test
    public void positiveTestTests() throws Exception {
        File testDir = mock(File.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(testDir.exists()).thenReturn(true);

        when(processBuilder.start()).thenReturn(process);

        String testMessage = "Failures: 0, Errors: 0, Skipped: 0";
        // String testMessage = "hey";
        InputStream testInput = new ByteArrayInputStream(testMessage.getBytes());
        when(process.getInputStream()).thenReturn(testInput);

        boolean result = ContinuousIntegrationServer.runTests(testDir, processBuilder);

        assertTrue(result);
    }

    @Test
    public void negativeTestTests() throws Exception {
        File testDir = mock(File.class);
        File testPomFile = mock(File.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(testDir.exists()).thenReturn(true);
        when(testPomFile.exists()).thenReturn(true);
        when(processBuilder.start()).thenReturn(process);

        String testMessage = "bro java is a-";
        InputStream testInput = new ByteArrayInputStream(testMessage.getBytes());
        when(process.getInputStream()).thenReturn(testInput);

        boolean result = ContinuousIntegrationServer.runTests(testDir, processBuilder);

        assertFalse(result);
    }

    @Test
    public void positiveTestCompile() throws Exception {
        File testDir = mock(File.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(processBuilder.start()).thenReturn(process);

        String message = "BUILD SUCCESS";
        InputStream inStream = new ByteArrayInputStream(message.getBytes());
        when(process.getInputStream()).thenReturn(inStream);

        assertTrue(ContinuousIntegrationServer.compileCode(testDir, processBuilder));
    }

    @Test
    public void negativeTestCompile() throws Exception {
        File testDir = mock(File.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(processBuilder.start()).thenReturn(process);

        String message = "Hey now, you're an all star\r\n" + //
                "Get your game on, go play";
        InputStream inStream = new ByteArrayInputStream(message.getBytes());
        when(process.getInputStream()).thenReturn(inStream);

        assertFalse(ContinuousIntegrationServer.compileCode(testDir, processBuilder));
    }

    @Test
    public void invalidTestCompile() {
        File testDir = new File("jkhdfg");
        ProcessBuilder processBuilder = new ProcessBuilder();
        assertFalse(ContinuousIntegrationServer.compileCode(testDir, processBuilder));
    }

    @Test
    public void successCommitStatusNotification() {
        // https://github.com/VictoriaHellstrom/testing-repo
        String owner = "VictoriaHellstrom";
        String repo = "testing-repo";
        String status = "success";
        String commitSHA = "2406ba3bb032a9660441c1fcdbabdda895ea9b93";
        String desc = "Everything is fine";
        String accessToken = "ACCESS_TOKEN";

        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc,
                accessToken);
        assertTrue(response);

    }

    @Test
    public void failureCommitStatusNotification() {
        String owner = "VictoriaHellstrom";
        String repo = "testing-repo";
        String status = "failure";
        String commitSHA = "2406ba3bb032a9660441c1fcdbabdda895ea9b93";
        String desc = "Everything is not fine";
        String accessToken = "ACCESS_TOKEN";

        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc,
                accessToken);
        assertTrue(response);
    }

    @Test
    public void failureCommitStatusNotificationWrongRepoParameters() {
        String owner = "shaquile";
        String repo = "oneil";
        String status = "true";
        String commitSHA = "to be changed";
        String desc = "Shaq does not have a repo";
        String accessToken = "toBeChanged";

        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc,
                accessToken);
        assertFalse(response);
    }
    /* Helped from chatgpt to set up fake repo */
    private Git createMockGitRepo() throws Exception {
        // Create a temporary directory for the local repo
        File tempRepoDir = Files.createTempDirectory("mockRepo").toFile();
        Git git = Git.init().setDirectory(tempRepoDir).call();
    
        // Create a temporary directory for the remote repo
        File remoteRepoDir = Files.createTempDirectory("mockRemoteRepo").toFile();
        Git remoteGit = Git.init().setDirectory(remoteRepoDir).call();
    
        // Create an initial commit in the remote repo
        remoteGit.commit().setMessage("Initial commit in remote").setAllowEmpty(true).call();
    
        // Create a test branch in the remote
        String branchName = "test-branch";
        remoteGit.checkout().setCreateBranch(true).setName(branchName).call();
        remoteGit.commit().setMessage("Commit on test branch").setAllowEmpty(true).call();
    
        // Push the test branch using the remote's URI instead of "origin"
        URIish remoteUri = new URIish(remoteRepoDir.toURI().toString());
        remoteGit.push()
                .setRemote(remoteUri.toString())
                .setRefSpecs(new RefSpec("refs/heads/" + branchName + ":refs/heads/" + branchName))
                .call();
    
        // Add the remote to the local repo
        git.remoteAdd().setName("origin").setUri(remoteUri).call();
    
        // Fetch and track the remote test branch
        git.fetch().setRemote("origin").call();
        git.checkout().setCreateBranch(true).setName(branchName).setStartPoint("origin/" + branchName).call();
    
        return git;
    }
    
    
    


    @Test
    void testPullBranchWithMockRepo() throws Exception {
        Git mockGit = createMockGitRepo();
        File testDir = mockGit.getRepository().getDirectory().getParentFile();
        String branchName = "test-branch";

        // Call the pullBranch method
        boolean result = ContinuousIntegrationServer.pullBranch(testDir, branchName);

        // Verify the result
        assertTrue(result, "pullBranch should return true");
        assertEquals(branchName, mockGit.getRepository().getBranch(), "Should be on the test branch");
    }



}


    
   



 
   



