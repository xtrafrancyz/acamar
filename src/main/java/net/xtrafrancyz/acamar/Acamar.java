package net.xtrafrancyz.acamar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import net.xtrafrancyz.acamar.Config.Server;
import net.xtrafrancyz.acamar.mysql.MysqlThread;
import net.xtrafrancyz.acamar.ping.ServerPingTask;
import net.xtrafrancyz.acamar.utils.LogFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

/**
 * @author xtrafrancyz
 */
public class Acamar {
    public static final Logger log = Logger.getLogger("Acamar");
    public static final Gson gson = new Gson();
    
    static {
        log.setUseParentHandlers(false);
        ConsoleHandler cs = new ConsoleHandler();
        cs.setFormatter(new LogFormatter());
        log.addHandler(cs);
    }
    
    public Config config;
    public final MysqlThread mysql;
    
    public Acamar() throws Exception {
        readConfig();
        mysql = new MysqlThread(config.mysql.url, config.mysql.user, config.mysql.pass);
    }
    
    public void start() {
        mysql.start();
        final ScheduledExecutorService executor = Executors.newScheduledThreadPool(config.threads);
        for (Map.Entry<String, Server> entry : config.servers.entrySet())
            executor.scheduleWithFixedDelay(new ServerPingTask(this, entry.getValue()), 0, config.pollDelay, TimeUnit.MILLISECONDS);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            mysql.finish();
        }));
    }
    
    public void readConfig() throws IOException {
        File confFile = new File("config.json");
        if (!confFile.exists()) {
            this.config = new Config();
            JsonWriter writer = new JsonWriter(new FileWriter(confFile));
            writer.setIndent("  ");
            new GsonBuilder().disableHtmlEscaping().create().toJson(config, Config.class, writer);
            writer.close();
            log.info("Created config.json");
        } else {
            this.config = gson.fromJson(
                Files.readAllLines(confFile.toPath()).stream()
                    .map(String::trim)
                    .filter(s -> !s.startsWith("#") && !s.isEmpty())
                    .reduce((a, b) -> a += b)
                    .orElse(""),
                Config.class
            );
        }
        this.config.servers.forEach((id, server) -> server.id = id);
    }
    
    public static void main(String[] args) throws Exception {
        new Acamar().start();
    }
}
