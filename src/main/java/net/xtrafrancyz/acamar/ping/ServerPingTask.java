package net.xtrafrancyz.acamar.ping;

import net.xtrafrancyz.acamar.Acamar;
import net.xtrafrancyz.acamar.Config.Server;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.xtrafrancyz.acamar.Acamar.log;

/**
 * Created in: 21.10.2014
 *
 * @author xtrafrancyz
 */
public class ServerPingTask implements Runnable {
    private static final Pattern REPLACE_PATTERN = Pattern.compile("\\{([a-z]+)\\}");
    
    private final Acamar app;
    private final Server server;
    
    public ServerPingTask(Acamar app, Server server) {
        this.app = app;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            PingResponse response = new PingResponse();
            try {
                switch (server.version) {
                    case "1.11":
                    case "1.10":
                        new ServerPing_1_10(app.config.timeout, server).fetchData(response);
                        break;
                    case "1.9":
                    case "1.8":
                    case "1.7":
                        new ServerPing_1_7(app.config.timeout, server).fetchData(response);
                        break;
                    case "1.6":
                    case "1.5":
                        new ServerPing_1_5(app.config.timeout, server).fetchData(response);
                        break;
                    default:
                        log.info("Unsupported version " + server.version + " of server " + server.id);
                        return;
                }
            } catch (IOException ex) {
                //ex.printStackTrace();
            }
            
            if (response.online)
                log.info("Server " + server + " updated " + response.onlinePlayers + "/" + response.maxPlayers);
            else
                log.info("Server " + server + " is offline");
            
            String updateQuery = fillQuery(server, response.online ? app.config.mysql.onlineQuery : app.config.mysql.offlineQuery, response);
            app.mysql.update(updateQuery, affectedRows -> {
                if (affectedRows == 0 && !server.dbRowInserted) {
                    server.dbRowInserted = true;
                    app.mysql.update(fillQuery(server, app.config.mysql.insertQuery, response), affectedRows0 -> {
                        app.mysql.query(updateQuery);
                    });
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static String fillQuery(Server server, String query, PingResponse response) {
        Matcher m = REPLACE_PATTERN.matcher(query);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            switch (m.group(1)) {
                case "id":
                    m.appendReplacement(sb, "\"" + server.id + "\"");
                    break;
                case "motd":
                    m.appendReplacement(sb, "\"" + response.motd + "\"");
                    break;
                case "time":
                    m.appendReplacement(sb, String.valueOf(System.currentTimeMillis() / 1000));
                    break;
                case "online":
                    m.appendReplacement(sb, String.valueOf(response.onlinePlayers));
                    break;
                case "max":
                    m.appendReplacement(sb, String.valueOf(response.maxPlayers));
                    break;
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
