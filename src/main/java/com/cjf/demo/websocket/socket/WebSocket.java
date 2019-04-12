package com.cjf.demo.websocket.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Descpription
 * @Author CJF
 * @Date 2019/4/12 11:38
 **/
@Component
@ServerEndpoint("/websocket/{user}")
public class WebSocket {
    @Autowired
    HttpServletRequest request;

    private static Logger logger = LoggerFactory.getLogger(WebSocket.class);
    private Session session;
    private static Map<String, Session> UserMap = new ConcurrentHashMap<>();
    private boolean flag = false;

    @OnOpen
    public void onOpen(Session session) {

        this.session = session;
        String user = session.getPathParameters().get("user");

        if (UserMap.get(user) != null) {
            try {
                this.session.close();
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info("一个新的连接建立了 " + WebSocketUtil.getRemoteAddress(session));
        UserMap.put(user, session);
        flag = true;
        //sendMessage(user,"已和你建立了Socket连接");
        sendMessageToUsers(user);
    }

    @OnMessage
    public void recMessage(String message, Session session) {
        logger.info("服务器收到了来自客户端的消息：" + message);
        sendMessageToAll(this.session.getPathParameters().get("user"), message);
    }

    public void sendMessage(String username, String message) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Session session = UserMap.get(username);
        try {
            session.getBasicRemote().sendText(format.format(date) + " 系统:" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        logger.error("发生错误", error);
    }

    @OnClose
    public void onClose() {
        if (!flag) {
            return;
        }
        String user = this.session.getPathParameters().get("user");
        UserMap.remove(user);
        logger.info(WebSocketUtil.getRemoteAddress(session) + " 主动关闭了连接，当前在线人数为:" + UserMap.size());
        sendMessageToAll("系统提示", user + "已退出聊天室，当前在线人数为 " + UserMap.size() + ".");
    }

    public void sendMessageToUsers(String user) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        for (String e : UserMap.keySet()
                ) {
            try {
                UserMap.get(e).getBasicRemote().sendText(format.format(date) + " 系统提示:" + "有新的伙伴(" + user + ")加入了聊天室！当前在线人数为:" + UserMap.size());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void sendMessageToAll(String user, String msg) {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        for (String e : UserMap.keySet()
                ) {
            try {
                UserMap.get(e).getBasicRemote().sendText(format.format(date) + " " + user + ":" + msg);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
