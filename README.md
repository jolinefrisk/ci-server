# CI-Server Group 2
This project is built from [smallest-java-ci.](https://github.com/KTH-DD2480/smallest-java-ci)
It is a CI-server designed to be triggered as a webhook by GitHub and can be compiled and run using Maven.



## Install Dependencies and Run CI-Server:


First, clone the repository:
```
git clone https://github.com/jolinefrisk/ci-server
cd ci-server
```


Before running the server, ensure all dependencies are installed:

* Apache Maven 3.9.9
[Installation guide for Maven](https://maven.apache.org/install.html)

* Ngrok
[Installation guide for Ngrok](https://ngrok.com/docs/guides/device-gateway/linux/)

* Java 21.0.5 (Ensure it is installed and set as the active Java version)

Also, ensure to assign your access token to the variable accessToken in the handle() method of ContinuousIntegrationServer.java to be able to set commit statuses. The access token must have repository permissions, specifically the repo scope, to update the commit status on GitHub.

Furthermore, add where you would like to clone your repo in "File dir = new File("D:\\Github\\github\\server");" in ContinuousIntegrationServer.java.

#### 1. Compile the project:

```
mvn clean package
```

#### 2. Start the server:

```
java -jar target/ci-server-1.0-SNAPSHOT.jar
```

#### 3. Expose the server via Ngrok:

Open a new terminal window and run:

```
ngrok http 8080
```

#### 4. Connect CI-server to the Repository:

Ngrok will generate a URL that can be used to connect the server to a webhook.

To set up the GitHub webhook:
1. Go to Settings → Webhooks → Click Add webhook.
2. Paste the forwarding URL you got from the terminal (e.g., http://8929b010.ngrok.io) in the Payload URL field.
3. When choosing Content type *, pick application/json
4. Click Add webhook.

#### 5. Verify that everything works:

Open a browser and go to http://localhost:8080 to check if the CI server is running locally.

## Code logic:

**compileCode():** The CI server compiles the code using the command mvn clean compile in a Bash environment. It then reads the output from the process and checks if it contains the line "BUILD SUCCESS", which indicates that the compilation was successful. If the code has been compiled successfully, the method returns true; otherwise, it returns false.To test the method, three unit tests are implemented: one positive, one negative, and one with invalid input.
Positive Test: Uses mocking to simulate an output containing "BUILD SUCCESS", verifying that the method returns true.
Negative Test: Similar to the positive test, but the output does not contain "BUILD SUCCESS", ensuring that the method returns false.
Invalid Input Test: Tests the method with an invalid directory to check how it handles incorrect input, expecting it to return false.


**runTests() covering P2:** implemented by running Maven command "mvn test" with help of the Java ProcessBuilder package. After that the method checked output for signature that all tests ran were succesful. runTests() was tested with mock library and using an expected true and false test.

**setCommitStatus() Method - P3**: implemented to update the status of a specified commit on a GitHub repository directly from the CI server. The status can be set to one of the following states: "success", "failure", "pending", or "error". The method constructs an HTTP POST request to the GitHub Commit Statuses API. A JSON payload containing the status parameters is created and written to the request body. After sending the request, the response code is checked. If the response code is 201 (Created), the method successfully updates the commit status and returns true. If the response code is not 201, it returns false. To validate the functionality, three tests were implemented:
- Success Status Test: A commit status is set to "success", and the method checks if the commit status matches the expected success status.
- Failure Status Test: A commit status is set to "failure", and the method verifies that the commit status matches the expected failure status.
- Error Handling Test: A request is made to a non-existing repository to ensure the method correctly handles errors and returns false.

## Essence standard evaluation:
Our team has previously reached the "In Use" stage of the Essence Standard Evaluation, where we had well-established practices, clear roles, and effective collaboration. However, for this project, we find ourselves at a lower stage of the Essence Ladder. We're still in the early stages of setting up our processes and tools, and while we’ve started to lay the foundation, things are less refined compared to before. We’re actively working to establish clear rules, refine our methods, and improve how we communicate and coordinate. The team is focused on figuring out the best tools and practices to move forward, and we’re committed to improving our approach so we can reach a higher level of maturity in our work.

## Statement of contributions:
| Name                     | Contribution                          |
|--------------------------|--------------------------------------|
| Jacob Lindström Bjäreklint| Wrote the methods getPayload, cloneRepo, and compileCode with the corresponding unit testing.             |
| Roger Chen               | Testing functionality (P2) and a little bit of documentation.          |
| Joline Frisk             | Created the pull functionality and tests for it. And part of the README |
| Victoria Hellström       | Created feature #3 notification of CI results by setting commit status in the repository and the tests for it. |
