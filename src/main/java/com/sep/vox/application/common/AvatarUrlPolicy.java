package com.sep.vox.application.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Validity rules for an avatar URL. DELIBERATELY unaware of where the image is stored.
 *
 * <p>Avatars do not go through {@code StoragePort}/S3 like question assets: the client uploads
 * them to its own store (currently Firebase Storage -- see
 * {@code vox-client-web/src/shared/firebase/uploadToStorage.ts}) and sends the backend a single
 * URL string, and the backend only stores the reference. So this class does NOT embed the URL
 * format of any particular provider: changing the storage means changing configuration, not code.
 *
 * <p>Two levels of checking, kept apart because they exist for different reasons:
 *
 * <p><b>1. Scheme -- always on, cannot be turned off.</b> {@code avatar_url} flows straight into
 * {@code <img src>} on other people's machines. A free-form string without a scheme check lets
 * {@code javascript:}, {@code data:} (SVG with an embedded script) or {@code blob:} into the DB,
 * where it waits for a careless place to render it. Requiring {@code https} also rejects
 * {@code http}, which would be blocked as mixed content when the web app runs on https -- a
 * failure that shows up as "the image does not load" and is very hard to trace.
 *
 * <p><b>2. Host allowlist -- optional, configured with {@code app.avatar.allowed-hosts}.</b>
 * Without a host restriction, a user can point the image at a foreign server, and EVERY teacher
 * who opens a grading list or an attendance screen will call it automatically: leaking their IP
 * and the time they looked, and letting the host owner change the image behind our back.
 * Leaving it empty accepts that trade-off knowingly (convenient in dev), so a real environment
 * should fill in the host of the storage in use.
 */
public final class AvatarUrlPolicy {

    /** Equals the length of the {@code users.avatar_url} column (varchar 4096) -- reject early to avoid a DB error. */
    public static final int MAX_URL_LENGTH = 4096;

    private AvatarUrlPolicy() {
    }

    /**
     * Normalizes the URL sent by the client into a value that can be written to the column.
     *
     * @param allowedHosts comma-separated host list, taken from {@code app.avatar.allowed-hosts}.
     *                     Empty = no host restriction (only the scheme is checked).
     * @param rawUrl       URL sent by the client. {@code null}/empty means REMOVE the avatar and returns {@code null}.
     * @return the trimmed URL, or {@code null} if the request removes the avatar.
     */
    public static String normalizeOrThrow(String allowedHosts, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String normalized = rawUrl.trim();
        if (normalized.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("The avatar URL is too long");
        }

        String host = httpsHostOf(normalized);
        List<String> hosts = parseHosts(allowedHosts);
        if (!hosts.isEmpty() && !hosts.contains(host)) {
            throw new IllegalArgumentException("The avatar must be uploaded to the system storage");
        }

        return normalized;
    }

    /** Returns the host, and forces the URL to be an absolute https URL with a host. */
    private static String httpsHostOf(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The avatar URL is invalid");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || !scheme.toLowerCase(Locale.ROOT).equals("https") || host == null || host.isBlank()) {
            throw new IllegalArgumentException("The avatar URL must be a valid https URL");
        }

        return host.toLowerCase(Locale.ROOT);
    }

    private static List<String> parseHosts(String allowedHosts) {
        if (allowedHosts == null || allowedHosts.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedHosts.split(","))
            .map(s -> s.trim())
            .filter(value -> !value.isBlank())
            .map(value -> value.toLowerCase(Locale.ROOT))
            .toList();
    }
}
