/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package base3;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Kevin
 */
public class KvWebSocketServer extends WebSocketServer {

    // 定义一个集合用来存储和当前websocket服务器保持连接的websocket客户端
    List<WebSocket> onLine = new ArrayList<>();

    KvWebSocketServer cla;
    JSONObject mtxJson = new JSONObject();
    JSONObject webSockOutJson = new JSONObject();
    JSONObject wsSysJson = new JSONObject();
    static public String retCommand = "";

    public void putJson(JSONObject jobj, String key, Object value) {
        try {
            jobj.put(key, value);//添加元素
        } catch (JSONException ex) {
        }
    }

    Object getJson(JSONObject jobj, String key) {
        try {
            return jobj.get(key);//添加元素
        } catch (JSONException ex) {
        }
        return null;
    }

    public KvWebSocketServer(String ip, Integer port) {
        super(new InetSocketAddress(ip, port));
        putJson(wsSysJson, "serialTime", 0);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        //System.out.println("Server: a new client connect in：" + webSocket.getRemoteSocketAddress().getHostName() + ":" + webSocket.getRemoteSocketAddress().getPort());
        // 当有客户端连接将其加入onLine集合中
        //onLine.add(webSocket);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {

        //System.out.println("Server: disconncet from client：" + webSocket.getRemoteSocketAddress().getHostName() + ":" + webSocket.getRemoteSocketAddress().getPort());
        // 当有客户端断开连接将其从onLine集合中移除
        //onLine.remove(webSocket);
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        cla = this;
        Object obj;
        JSONObject mesJson;
        try {
            mesJson = new JSONObject(message);
        } catch (JSONException ex) {
            Logger.getLogger(WebSocketConn.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        //======================================================================
        obj = getJson(wsSysJson, "serialTime");
        int serialTime = (int) obj;
        serialTime++;
        serialTime = serialTime % 10000;
        //System.out.println("testBackValue " + serialTime);
        putJson(wsSysJson, "serialTime", serialTime);
        //======================================================================
        String userName = "";
        try {
            obj = mesJson.get("userName");
            if (obj != null) {
                userName = obj.toString();
                ConnectCla conObj = GB.connectMap.get(userName);
                if (conObj != null) {
                    conObj.time = 0;
                } else {
                    conObj = new ConnectCla(userName, 100);//unit 20ms
                    GB.connectMap.put(userName, conObj);
                    //Root.log(1, "UserName: " + userName  + " jmp in.");

                }
            }
        } catch (Exception ex) {

        }

        obj = getJson(mesJson, "deviceId");
        String deviceId = (String) obj;
        obj = getJson(mesJson, "act");
        String actStr = (String) obj;
        JSONObject outJson = new JSONObject();
        putJson(outJson, "act", actStr + "~react");
        putJson(outJson, "wsSysJson", wsSysJson.toString());
        switch (deviceId) {
            case "icsUi":
                outJson = Ics.wsCallBack(userName, mesJson, actStr, outJson);
                break;
        }
        webSocket.send(outJson.toString());
        return;

        //System.out.println("Server: receive a message from client:" + webSocket.getRemoteSocketAddress().getHostName() + ":" + webSocket.getRemoteSocketAddress().getPort() + " ：" + s);
        // 向客户端回馈消息
        //webSocket.send("this is server callback message.");
        // 亦可以群发消息
        /*
        for (WebSocket socket : onLine) {
            socket.send(webSocket.getRemoteSocketAddress().getHostName() + ":" + webSocket.getRemoteSocketAddress().getPort() + " 群发消息：" + s);
        }
         */
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        //e.printStackTrace();
        //System.out.println(e.getStackTrace());
        //System.out.println("Server: communicate error ");
        //System.out.println("Server: communicate error " + webSocket.getRemoteSocketAddress().getHostName() + ":" + webSocket.getRemoteSocketAddress().getPort() + " 通信发生异常");
    }

    @Override
    public void onStart() {
        System.out.println("Server: websocket start ...");
    }

    public static void startWebSocketServer(int port) {
        KvWebSocketServer server = new KvWebSocketServer(GB.real_ip_str, port);
        System.out.println("WebSocket Address: "+GB.real_ip_str+":"+port);
        server.start();
    }
    public static void startWebSocketServer(String ipAddr,int port) {
        KvWebSocketServer server = new KvWebSocketServer(ipAddr, port);
        System.out.println("WebSocket Address: "+ipAddr+":"+port);
        server.start();
    }

    public static void testServer(int port) {
        KvWebSocketServer server = new KvWebSocketServer("127.0.0.1", 8899);
        // 启动服务端websocket
        server.start();
        // 循环启动多个客户端连接服务端
        for (int i = 0; i < 4; i++) {
            // 连接服务端websocket的地址
            URI uri;
            try {
                uri = new URI("ws://192.168.0.28:8899");
                // 创建客户端websocket对象
                KvWebSocketClient client = new KvWebSocketClient(uri);
                // 阻塞式连接
                client.connectBlocking();
                // 向服务端发送消息
                client.send("this is a messaage of client " + i);
            } catch (Exception ex) {
                Logger.getLogger(KvWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
