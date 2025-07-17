package com.example.game_server.ws;

import com.example.game_server.auth.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;

    //Active sessions by username
    private final Map<String, WebSocketSession> sessions =  new ConcurrentHashMap<>();

    public GameWebSocketHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        //received token from query parameters ?token=.
        String query = session.getUri().getQuery();
        String token = extractToken(query);

        if (token == null || !jwtUtil.validateToken(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or missing token"));
            return;
        }

        String username = jwtUtil.getUsername(token);
        sessions.put(username, session);

        log.info("✅ Connected: {}", username);

        session.sendMessage(new TextMessage("{\"status\":\"connected\",\"user\":\"" + username + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.values().remove(session);
        log.info("🔌 Disconnected: {}", status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("📩 Received: {}", message.getPayload());

        String json = message.getPayload();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        String type = node.has("type") ? node.get("type").asText() : null;
        if (type == null) {
            session.sendMessage(new TextMessage("{\"error\":\"Missing field 'type'\"}"));
            return;
        }

        switch (type) {
            case "move" -> {
                JsonNode data = node.get("data");
                int x = data.get("x").asInt();
                int y = data.get("y").asInt();
                log.info("🚶 Move to {}, {}", x, y);
                session.sendMessage(new TextMessage("{\"status\":\"moved\",\"x\":" + x + ",\"y\":" + y + "}"));
            }
            case "attack" -> {
                JsonNode data = node.get("data");
                int targetId = data.get("targetId").asInt();
                log.info("⚔️ Attack target {}", targetId);
                session.sendMessage(new TextMessage("{\"status\":\"attacked\",\"targetId\":" + targetId + "}"));
            }
            case "end_turn" -> {
                log.info("⏭️ End of turn");
                session.sendMessage(new TextMessage("{\"status\":\"turn_ended\"}"));
            }
            default -> {
                log.warn("❗ Unknown command: {}", type);
                session.sendMessage(new TextMessage("{\"error\":\"Unknown command: " + type + "\"}"));
            }
        }
    }

    private String extractToken(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring("token=".length());
            }
        }
        return null;
    }
}
