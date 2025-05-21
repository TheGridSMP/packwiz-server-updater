package sleepy_evelyn.packwizsu.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashedFileDownloader {

    private final Path destination;
    private final String url;
    private final String sha256Hash;

    public HashedFileDownloader(String url, String sha256Hash, Path destination) {
        this.url = url;
        this.sha256Hash = sha256Hash;
        this.destination = destination;
    }

    public void download() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(destination));

        if (response.statusCode() != 200)
            throw new IOException("Failed to download file. HTTP Response code: " + response.statusCode());
    }

    public boolean hashesMatch() throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (DigestInputStream digestInputStream = new DigestInputStream(Files.newInputStream(destination), digest)) {
            //noinspection StatementWithEmptyBody
            while (digestInputStream.read() != -1) {
                // Reading data and updating the digest automatically
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();

        for (byte b : hashBytes) {
            // Convert from signed to unsigned
            String hex = Integer.toHexString(0xff & b);
            // Handle single characters
            if (hex.length() == 1) hexString.append('0');
            // Add the string value
            hexString.append(hex);
        }
        return hexString.toString().equalsIgnoreCase(sha256Hash);
    }

    public void assertHashMatches() throws FailedHashMatchException, IOException, NoSuchAlgorithmException {
        if (!this.hashesMatch()) {
            if (!Files.deleteIfExists(destination)) {
                throw new IOException("Cannot verify the integrity of downloaded file 'packwiz-installer-bootstrap.jar'" +
                        "Please delete this file manually from your main server directory and replace with the correct file" +
                        "from https://github.com/packwiz/packwiz-installer-bootstrap/releases as it may be malicious");
            }

            throw new FailedHashMatchException();
        }
    }

    public static class FailedHashMatchException extends Exception {
        public FailedHashMatchException() {
            super("Failed to verify the Packwiz Bootstrap hashes match. Please manually download the " +
                    "bootstrapper from: https://github.com/packwiz/packwiz-installer-bootstrap/releases " +
                    "and place in the main directory for your server.");
        }
    }
}
