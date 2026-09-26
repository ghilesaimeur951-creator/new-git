package com.ghiles.quizubuntu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal GitHub REST client used only for account/repository discovery.
 * Git data operations are performed by RealGitClient/JGit over HTTPS.
 */
public final class GitHubApiClient {

    public static final class RepoInfo {
        public final String fullName;
        public final String cloneUrl;
        public final String defaultBranch;
        public final boolean isPrivate;

        RepoInfo(String fullName, String cloneUrl, String defaultBranch, boolean isPrivate) {
            this.fullName = fullName;
            this.cloneUrl = cloneUrl;
            this.defaultBranch = defaultBranch;
            this.isPrivate = isPrivate;
        }

        @Override
        public String toString() {
            return fullName + (isPrivate ? "  🔒" : "");
        }
    }

    private final String token;

    public GitHubApiClient(String token) {
        this.token = token == null ? "" : token.trim();
    }

    public String getLogin() throws Exception {
        JSONObject user = new JSONObject(get("https://api.github.com/user"));
        return user.optString("login", "");
    }

    public String addAuthenticationSshKey(
        String title,
        String publicKey
    ) throws Exception {
        if (token.isEmpty()) {
            throw new IllegalStateException("Aucun jeton GitHub n'est enregistré.");
        }

        JSONObject body = new JSONObject();
        body.put("title", title);
        body.put("key", publicKey);

        JSONObject result = new JSONObject(
            request(
                "POST",
                "https://api.github.com/user/keys",
                body.toString(),
                true
            )
        );

        return String.valueOf(result.optLong("id", 0L));
    }

    public static List<String> githubSshKnownHostLines() throws Exception {
        String json = requestAnonymous(
            "GET",
            "https://api.github.com/meta"
        );

        JSONObject meta = new JSONObject(json);
        JSONArray keys = meta.optJSONArray("ssh_keys");
        List<String> result = new ArrayList<>();

        if (keys == null) return result;

        for (int i = 0; i < keys.length(); i++) {
            String key = keys.optString(i, "").trim();

            if (!key.isEmpty()) {
                result.add("github.com " + key);
            }
        }

        return result;
    }

    public List<RepoInfo> listRepositories() throws Exception {
        String json = get(
            "https://api.github.com/user/repos" +
            "?per_page=100&sort=updated&affiliation=owner,collaborator,organization_member"
        );

        JSONArray array = new JSONArray(json);
        List<RepoInfo> result = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject repo = array.getJSONObject(i);

            result.add(
                new RepoInfo(
                    repo.optString("full_name"),
                    repo.optString("clone_url"),
                    repo.optString("default_branch", "main"),
                    repo.optBoolean("private", false)
                )
            );
        }

        return result;
    }

    private String get(String urlText) throws Exception {
        return request("GET", urlText, null, true);
    }

    private String request(
        String method,
        String urlText,
        String jsonBody,
        boolean authenticated
    ) throws Exception {
        if (authenticated && token.isEmpty()) {
            throw new IllegalStateException("Aucun jeton GitHub n'est enregistré.");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();

        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
            connection.setRequestProperty("User-Agent", "Ubuntu-Git-Academy-Android");

            if (authenticated) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }

            if (jsonBody != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                connection.getOutputStream().write(bytes);
            }

            int code = connection.getResponseCode();
            InputStream input = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

            String body = readAll(input);

            if (code < 200 || code >= 300) {
                String message = body;
                try {
                    message = new JSONObject(body).optString("message", body);
                } catch (Exception ignored) {
                }

                throw new IllegalStateException(
                    "GitHub HTTP " + code + " : " + message
                );
            }

            return body;
        } finally {
            connection.disconnect();
        }
    }

    private static String requestAnonymous(
        String method,
        String urlText
    ) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();

        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("X-GitHub-Api-Version", "2026-03-10");
            connection.setRequestProperty("User-Agent", "Ubuntu-Git-Academy-Android");

            int code = connection.getResponseCode();
            InputStream input = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

            String body = readStatic(input);

            if (code < 200 || code >= 300) {
                throw new IllegalStateException("GitHub HTTP " + code);
            }

            return body;
        } finally {
            connection.disconnect();
        }
    }

    private static String readStatic(InputStream input) throws Exception {
        if (input == null) return "";

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        );

        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }

        return builder.toString();
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(input, StandardCharsets.UTF_8)
        );

        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }

        return builder.toString();
    }
}
