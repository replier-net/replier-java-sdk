package net.replier;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replier.net WhatsApp API client.
 *
 * <pre>
 * Replier replier = Replier.builder().instanceId("12345").token("your-token").build();
 * replier.sendMessage("15551234567", "Hello!");
 * </pre>
 */
public class Replier {
    public static final String VERSION = "1.0.0";
    public static final String DEFAULT_BASE_URL = "https://api.replier.net";

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final String instanceId;
    private final String token;
    private final String email;
    private final String apiKey;
    private final String baseUrl;
    private final Duration timeout;
    private final HttpClient http;

    private Replier(Builder b) {
        this.instanceId = b.instanceId;
        this.token = b.token;
        this.email = b.email;
        this.apiKey = b.apiKey;
        this.baseUrl = b.baseUrl.replaceAll("/+$", "");
        this.timeout = b.timeout;
        this.http = b.httpClient != null ? b.httpClient
                : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String instanceId, token, email, apiKey;
        private String baseUrl = DEFAULT_BASE_URL;
        private Duration timeout = Duration.ofSeconds(70);
        private HttpClient httpClient;

        public Builder instanceId(String v) { this.instanceId = v; return this; }
        public Builder token(String v) { this.token = v; return this; }
        /** Needed only for listInstances / newInstance / renewInstance. */
        public Builder email(String v) { this.email = v; return this; }
        /** Needed only for listInstances / newInstance / renewInstance. */
        public Builder apiKey(String v) { this.apiKey = v; return this; }
        public Builder baseUrl(String v) { this.baseUrl = v; return this; }
        public Builder timeout(Duration v) { this.timeout = v; return this; }
        public Builder httpClient(HttpClient v) { this.httpClient = v; return this; }
        public Replier build() { return new Replier(this); }
    }

    // ------------------------------------------------------------------ core

    /** Call any instance endpoint; instanceid and token are added automatically. */
    public Map<String, Object> call(String method, Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (instanceId != null) body.put("instanceid", instanceId);
        if (token != null) body.put("token", token);
        if (params != null) body.putAll(params);
        return request(method, body);
    }

    public Map<String, Object> call(String method) { return call(method, null); }

    /** Call an account endpoint; email and apikey are added automatically. */
    public Map<String, Object> callAccount(String method, Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (email != null) body.put("email", email);
        if (apiKey != null) body.put("apikey", apiKey);
        if (params != null) body.putAll(params);
        return request(method, body);
    }

    private Map<String, Object> request(String method, Map<String, Object> params) {
        params.values().removeIf(v -> v == null);
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/" + method))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "replier-java/" + VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(params), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ReplierException("Request failed: " + e.getMessage(), null, null, 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReplierException("Request interrupted", null, null, 0, e);
        }

        int status = res.statusCode();
        Map<String, Object> data;
        try {
            data = GSON.fromJson(res.body(), MAP_TYPE);
        } catch (JsonSyntaxException e) {
            throw new ReplierException("Invalid JSON response (HTTP " + status + ")", null, null, status, e);
        }
        if (data == null) {
            throw new ReplierException("Invalid JSON response (HTTP " + status + ")", null, null, status, null);
        }
        Object success = data.get("success");
        boolean failed = Boolean.FALSE.equals(success) || "false".equals(success);
        if (status >= 400 || failed) {
            Object code = data.get("errorCode");
            throw new ReplierException(reason(data, status >= 400 ? "HTTP " + status : "Request failed"),
                    code == null ? null : String.valueOf(code), data, status, null);
        }
        return data;
    }

    private static String reason(Map<String, Object> data, String fallback) {
        Object reason = data.get("reason");
        if (reason != null && !String.valueOf(reason).isEmpty()) return String.valueOf(reason);
        Object msg = data.get("msg");
        if (msg instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            for (Object m : (Collection<?>) msg) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(m);
            }
            return sb.toString();
        }
        if (msg != null) return String.valueOf(msg);
        return fallback;
    }

    private static Map<String, Object> p(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    // ------------------------------------------------------------------ instance

    public Map<String, Object> status() { return call("status"); }
    public Map<String, Object> qrCode() { return call("qr_code"); }
    public Map<String, Object> pairCode(String phone) { return call("pair_code", p("phone", phone)); }
    public Map<String, Object> logout() { return call("logout"); }
    public Map<String, Object> takeover() { return call("takeover"); }
    public Map<String, Object> expiry() { return call("expiry"); }
    public Map<String, Object> retry() { return call("retry"); }
    public Map<String, Object> reboot() { return call("reboot"); }
    public Map<String, Object> getSettings() { return call("getSettings"); }
    public Map<String, Object> setSettings(Map<String, Object> settings) { return call("settings", new LinkedHashMap<>(settings)); }
    public Map<String, Object> me() { return call("me"); }
    public Map<String, Object> version() { return call("version"); }
    public Map<String, Object> screenshot() { return call("screenshot"); }

    // ------------------------------------------------------------------ messages

    public Map<String, Object> sendMessage(String phone, String body) { return sendMessage(phone, body, null); }

    public Map<String, Object> sendMessage(String phone, String body, Map<String, Object> options) {
        return call("sendMessage", p("phone", phone, "body", body, "options", options));
    }

    /** @param body file URL or base64 data URI */
    public Map<String, Object> sendFile(String phone, String body, String filename, String caption) {
        return call("sendFile", p("phone", phone, "body", body, "filename", filename, "caption", caption));
    }

    public Map<String, Object> sendPTT(String phone, String audio) { return call("sendPTT", p("phone", phone, "audio", audio)); }
    public Map<String, Object> sendLink(String phone, String body) { return call("sendLink", p("phone", phone, "body", body)); }
    public Map<String, Object> sendContact(String phone, String contactId) { return call("sendContact", p("phone", phone, "contactId", contactId)); }

    public Map<String, Object> sendLocation(String phone, double lat, double lng, String address) {
        return call("sendLocation", p("phone", phone, "lat", lat, "lng", lng, "address", address));
    }

    public Map<String, Object> sendVCard(String phone, String vcard) { return call("sendVCard", p("phone", phone, "vcard", vcard)); }

    public Map<String, Object> sendButton(String phone, String body, Object buttons, String header, String footer) {
        return call("sendButton", p("phone", phone, "body", body, "buttons", buttons, "header", header, "footer", footer));
    }

    public Map<String, Object> sendList(String phone, String body, String title, Object list, String header, String footer) {
        return call("sendList", p("phone", phone, "body", body, "title", title, "list", list, "header", header, "footer", footer));
    }

    public Map<String, Object> forwardMessage(String phone, String messageId) { return call("forwardMessage", p("phone", phone, "messageId", messageId)); }
    public Map<String, Object> deleteMessage(String phone, String messageId) { return call("deleteMessage", p("phone", phone, "messageId", messageId)); }
    public Map<String, Object> sendSeen(String phone) { return call("sendSeen", p("phone", phone)); }

    public Map<String, Object> messages(String phone, Integer limit, Long lastMessageNumber) {
        return call("messages", p("phone", phone, "limit", limit, "lastMessageNumber", lastMessageNumber));
    }

    public Map<String, Object> showMessagesQueue() { return call("showMessagesQueue"); }
    public Map<String, Object> clearMessagesQueue() { return call("clearMessagesQueue"); }
    public Map<String, Object> showActionsQueue() { return call("showActionsQueue"); }
    public Map<String, Object> clearActionsQueue() { return call("clearActionsQueue"); }

    // ------------------------------------------------------------------ chats

    public Map<String, Object> dialogs() { return call("dialogs"); }
    public Map<String, Object> chats() { return call("chats"); }
    public Map<String, Object> contacts() { return call("contacts"); }

    // ------------------------------------------------------------------ groups

    public Map<String, Object> createGroup(String groupName, List<String> phones) {
        return call("group", p("groupName", groupName, "phones", String.join(",", phones)));
    }

    public Map<String, Object> leaveGroup(String groupId) { return call("leaveGroup", p("groupId", groupId)); }

    public Map<String, Object> addGroupParticipant(String groupId, List<String> phones) {
        return call("addGroupParticipant", p("groupId", groupId, "phones", String.join(",", phones)));
    }

    public Map<String, Object> removeGroupParticipant(String groupId, List<String> phones) {
        return call("removeGroupParticipant", p("groupId", groupId, "phones", String.join(",", phones)));
    }

    public Map<String, Object> promoteGroupParticipant(String groupId, List<String> phones) {
        return call("promoteGroupParticipant", p("groupId", groupId, "phones", String.join(",", phones)));
    }

    public Map<String, Object> demoteGroupParticipant(String groupId, List<String> phones) {
        return call("demoteGroupParticipant", p("groupId", groupId, "phones", String.join(",", phones)));
    }

    // ------------------------------------------------------------------ channels

    public Map<String, Object> createChannel(String name, String description, String picture) {
        return call("channel", p("name", name, "description", description, "picture", picture));
    }

    public Map<String, Object> followChannel(String channelId) { return call("channelFollow", p("channelId", channelId)); }
    public Map<String, Object> unfollowChannel(String channelId) { return call("channelUnfollow", p("channelId", channelId)); }
    public Map<String, Object> channelInfo(String channelId) { return call("channelInfo", p("channelId", channelId)); }

    // ------------------------------------------------------------------ webhook / bot / ban

    public Map<String, Object> setWebhook(String webhookUrl) { return call("webhook", p("webhookUrl", webhookUrl)); }
    public Map<String, Object> getWebhookStatus() { return call("getWebhookStatus"); }
    public Map<String, Object> getBanSettings() { return call("getBanSettings"); }
    public Map<String, Object> botSettings() { return call("botSettings"); }
    public Map<String, Object> getBotSettings() { return call("getbotSettings"); }

    // ------------------------------------------------------------------ account

    public Map<String, Object> listInstances() { return callAccount("listInstances", null); }

    public Map<String, Object> newInstance(int packageId, String voucher) {
        return callAccount("newInstance", p("packageid", packageId, "voucher", voucher));
    }

    public Map<String, Object> renewInstance(String instanceId, int packageId, String voucher) {
        return callAccount("renewInstance", p("instanceid", instanceId, "packageid", packageId, "voucher", voucher));
    }
}
