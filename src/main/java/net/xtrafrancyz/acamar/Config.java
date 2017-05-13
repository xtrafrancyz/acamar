package net.xtrafrancyz.acamar;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xtrafrancyz
 */
public class Config {
    public long pollDelay = 3000;
    public int timeout = 2000;
    public int threads = 3;
    
    public Mysql mysql = new Mysql();
    public Map<String, Server> servers = new HashMap<>();
    
    Config() {
        Server testServer = new Server();
        testServer.host = "mc.hypixel.net";
        testServer.port = 25565;
        testServer.version = "1.9";
        servers.put("hypixel", testServer);
        testServer = new Server();
        testServer.host = "vimeworld.net";
        testServer.port = 25565;
        testServer.version = "1.6";
        servers.put("vimeworld", testServer);
    }
    
    public static class Mysql {
        public String url = "jdbc:mysql://127.0.0.1/database?useUnicode=true&characterEncoding=utf-8";
        public String user = "root";
        public String pass = "";
        public String onlineQuery = "UPDATE servers SET updated = {time}, online = {online}, max = {max} WHERE id = {id}";
        public String offlineQuery = "UPDATE servers SET updated = 1 WHERE id = {id}";
        public String insertQuery = "INSERT IGNORE INTO servers (id) VALUES ({id})";
    }
    
    public static class Server {
        public transient String id;
        public transient boolean dbRowInserted = false;
        public String host;
        public int port;
        public String version;
        
        @Override
        public String toString() {
            if (port == 25565)
                return id + "[" + host + "]";
            else
                return id + "[" + host + ":" + port + "]";
        }
    }
}
