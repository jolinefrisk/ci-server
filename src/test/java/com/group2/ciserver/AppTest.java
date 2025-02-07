package com.group2.ciserver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.StringReader;

import org.json.JSONObject;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
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
}
