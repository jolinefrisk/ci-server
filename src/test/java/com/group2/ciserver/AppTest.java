package com.group2.ciserver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

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
    public void positiveTestCompile() throws Exception {
        File testDir = mock(File.class);
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        Process process = mock(Process.class);

        when(processBuilder.start()).thenReturn(process);

        String message = "BUILD SUCCESS";
        InputStream inStream = new ByteArrayInputStream(message.getBytes());
        when(process.getInputStream()).thenReturn(inStream);

        assertTrue(ContinuousIntegrationServer.compileCode(testDir, processBuilder, true));
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

        assertFalse(ContinuousIntegrationServer.compileCode(testDir, processBuilder, true));
    }

    @Test
    public void invalidTestCompile() {
        File testDir = new File("jkhdfg");
        ProcessBuilder processBuilder = new ProcessBuilder();
        assertFalse(ContinuousIntegrationServer.compileCode(testDir, processBuilder, true));
    }

    @Test
    public void successCommitStatusNotification() {   
        String owner = "to be changed";
        String repo = "to be changed";
        String status = "success";
        String commitSHA = "to be changed";
        String desc = "Everything is fine";
        String accessToken = "toBeChanged";
        
        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc, accessToken);
        assertTrue(response);

    }

    @Test
    public void failureCommitStatusNotification() {   
        String owner = "to be changed";
        String repo = "to be changed";
        String status = "failure";
        String commitSHA = "to be changed";
        String desc = "Everything is not fine";
        String accessToken = "toBeChanged";
        
        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc, accessToken);
        assertFalse(response);
    }

    @Test
    public void failureCommitStatusNotificationWrongRepoParameters() {   
        String owner = "shaquile";
        String repo = "oneil";
        String status = "true";
        String commitSHA = "to be changed";
        String desc = "Shaq does not have a repo";
        String accessToken = "toBeChanged";
        
        boolean response = ContinuousIntegrationServer.setCommitStatus(owner, repo, commitSHA, status, desc, accessToken);
        assertFalse(response);
    }

}
