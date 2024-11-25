package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@ServerEndpoint("/ws/{sid}")
public class WebsocketServer {

    private static Map<String, Session> sessionMap = new HashMap<>();

    @OnOpen
    public void onOpen(@PathParam("sid") String sid, Session session) {
        log.info("建立ws连接，sid：{}", sid);
        sessionMap.put(sid, session);
    }

    @OnMessage
    public void onMessage(@PathParam("sid") String sid, String message) {
        log.info("sid：{}，收到消息：{}", sid, message);

    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        log.info("关闭ws连接，sid：{}", sid);
        sessionMap.remove(sid);
    }

    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
