package ru.bogdanov.gitlabreviewmcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StdioMcpServerIT {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void initializesListsToolsCallsToolAndKeepsStdoutProtocolOnly() throws Exception {
        Process process = startConfiguredProcess();
        try (BufferedWriter input = new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
                BufferedReader output = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            send(input, "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{"
                    + "\"protocolVersion\":\"2025-06-18\",\"capabilities\":{},"
                    + "\"clientInfo\":{\"name\":\"integration-test\",\"version\":\"1\"}}}");
            String initialization = readLine(output);

            assertThat(initialization).startsWith("{\"jsonrpc\"");
            assertThat(initialization).contains("gitlab-review-mcp", "2025-06-18");

            send(input, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}");
            send(input, "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}");
            assertThat(readLine(output)).contains("\"id\":2", "\"result\"");

            send(input, "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{}}");
            String tools = readLine(output);
            assertThat(tools).contains(
                    "gitlab_check_connection",
                    "gitlab_get_merge_request",
                    "gitlab_get_merge_request_diff",
                    "gitlab_get_merge_request_discussions",
                    "gitlab_prepare_review",
                    "gitlab_publish_review");
            assertThat(tools).contains(
                    "\"inputSchema\"",
                    "\"mergeRequestUrl\"",
                    "\"comments\"",
                    "\"proposalId\"",
                    "\"expectedDigest\"",
                    "\"readOnlyHint\":true",
                    "\"readOnlyHint\":false");

            send(input, "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{"
                    + "\"name\":\"gitlab_get_merge_request\",\"arguments\":{"
                    + "\"mergeRequestUrl\":\"https://untrusted.example.com/g/p/-/merge_requests/1\"}}}");
            assertThat(readLine(output)).contains("INVALID_MERGE_REQUEST_URL").doesNotContain("Exception");
        } finally {
            stop(process);
        }
    }

    @Test
    void exitsWithoutWritingProtocolWhenRequiredConfigurationIsMissing() throws Exception {
        ProcessBuilder builder = processBuilder();
        builder.environment().remove("GITLAB_BASE_URL");
        builder.environment().remove("GITLAB_TOKEN");
        Process process = builder.start();

        assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isNotZero();
        assertThat(new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).isBlank();
    }

    private Process startConfiguredProcess() throws IOException {
        ProcessBuilder builder = processBuilder();
        builder.environment().put("GITLAB_BASE_URL", "https://127.0.0.1:9");
        builder.environment().put("GITLAB_TOKEN", "integration-test-token");
        return builder.start();
    }

    private ProcessBuilder processBuilder() {
        Path jar = Path.of("target", "gitlab-review-mcp.jar").toAbsolutePath();
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", jar.toString());
        builder.directory(temporaryDirectory.toFile());
        builder.redirectError(temporaryDirectory.resolve("server.stderr.log").toFile());
        return builder;
    }

    private void send(BufferedWriter input, String json) throws IOException {
        input.write(json);
        input.newLine();
        input.flush();
    }

    private String readLine(BufferedReader output) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return executor.submit(output::readLine).get(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void stop(Process process) throws IOException, InterruptedException {
        process.getOutputStream().close();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroy();
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

}
