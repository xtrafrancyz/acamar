package net.xtrafrancyz.acamar.mysql;

import net.xtrafrancyz.acamar.Acamar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * @author xtrafrancyz
 */
public class MysqlThread extends Thread {
    private static final int TICK_INTERVAL = 1000;
    
    private MysqlConfig config;
    
    private final Object lock = new Object();
    private final Queue<Query> queries;
    private volatile boolean running = false;
    private volatile boolean connected = false;
    
    private Connection db;
    
    public MysqlThread(String url, String user, String pass) {
        this(new MysqlConfigString(url, user, pass));
    }
    
    public MysqlThread(Supplier<String> url, Supplier<String> user, Supplier<String> pass) {
        this(new MysqlConfigSupplier(url, user, pass));
    }
    
    public MysqlThread(MysqlConfig config) {
        this.setName("MySQL Thread");
        this.setDaemon(true);
        this.config = config;
        this.queries = new ConcurrentLinkedQueue<>();
        
        // Preload class
        SafeRunnable.class.getName();
    }
    
    public void query(String query) {
        update(query, null);
    }
    
    public void select(String query, SelectCallback callback) {
        queries.add(new Query(query, callback));
        synchronized (lock) {
            lock.notify();
        }
    }
    
    public void update(String query, UpdateCallback callback) {
        queries.add(new Query(query, callback));
        synchronized (lock) {
            lock.notify();
        }
    }
    
    @Override
    public void start() {
        if (running)
            return;
        
        running = true;
        super.start();
    }
    
    public void finish() {
        if (!running)
            return;
        
        running = false;
        
        safe(this::join);
        
        if (db != null) {
            safe(this::checkConnection);
            safe(this::executeQueries);
            safe(db::close);
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Выполнение без бросания исключений
     *
     * @param r задача
     */
    protected void safe(SafeRunnable r) {
        try {
            r.run();
        } catch (Exception ignored) {}
    }
    
    @Override
    public void run() {
        checkConnection();
        while (running) {
            if (!queries.isEmpty()) {
                if (checkConnection()) {
                    executeQueries();
                } else {
                    queries.clear();
                }
            }
            
            try {
                synchronized (lock) {
                    lock.wait(TICK_INTERVAL);
                }
            } catch (InterruptedException e) {
                running = false;
            }
        }
    }
    
    private void executeQueries() {
        while (!queries.isEmpty()) {
            Query query = queries.poll();
            try (Statement statement = db.createStatement()) {
                boolean isSelect = statement.execute(query.query);
                
                try {
                    if (isSelect) {
                        if (query.callback != null) {
                            ResultSet rs = statement.getResultSet();
                            ((SelectCallback) query.callback).done(rs);
                            rs.close();
                        }
                    } else {
                        if (query.callback != null)
                            ((UpdateCallback) query.callback).done(statement.getUpdateCount());
                    }
                } catch (Exception e) {
                    // Catch all callback exceptions
                    Acamar.log.log(Level.SEVERE, "Query " + query.query + " is failed!", e);
                }
            } catch (Exception e) {
                // Catch all sql exceptions (log without stacktrace)
                if (e.getMessage() != null && e.getMessage().contains("try restarting transaction")) {
                    queries.add(query);
                    Acamar.log.warning("Query " + query.query + " is failed! Restarting: " + e.getMessage());
                } else {
                    Acamar.log.severe("Query " + query.query + " is failed! Message: " + e.getMessage());
                }
            }
        }
    }
    
    private boolean checkConnection() {
        boolean state = false;
        try {
            if (db != null && !isValid()) {
                safe(db::close);
                db = null;
            }
            if (db == null)
                connect();
            state = db != null && isValid();
        } catch (Exception e) {
            Acamar.log.log(Level.WARNING, "Error while connecting to database: {0}", e.getMessage());
        }
        if (connected != state)
            connected = state;
        return state;
    }
    
    private void connect() {
        try {
            db = DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPass());
            if (isValid()) {
                Acamar.log.info("Mysql connected.");
            }
        } catch (SQLException ex) {
            Acamar.log.warning(ex.getMessage());
        }
    }
    
    private boolean isValid() throws SQLException {
        return db.isValid(40);
    }
    
    public interface MysqlConfig {
        String getUrl();
        
        String getUser();
        
        String getPass();
    }
    
    protected interface SafeRunnable {
        void run() throws Exception;
    }
    
    public static class MysqlConfigString implements MysqlConfig {
        private final String url;
        private final String user;
        private final String pass;
        
        public MysqlConfigString(String url, String user, String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }
        
        @Override
        public String getUrl() {
            return url;
        }
        
        @Override
        public String getUser() {
            return user;
        }
        
        @Override
        public String getPass() {
            return pass;
        }
    }
    
    public static class MysqlConfigSupplier implements MysqlConfig {
        private final Supplier<String> url;
        private final Supplier<String> user;
        private final Supplier<String> pass;
        
        public MysqlConfigSupplier(Supplier<String> url, Supplier<String> user, Supplier<String> pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }
        
        @Override
        public String getUrl() {
            return url.get();
        }
        
        @Override
        public String getUser() {
            return user.get();
        }
        
        @Override
        public String getPass() {
            return pass.get();
        }
    }
    
    private static class Query {
        String query;
        Callback callback;
        
        public Query(String query, Callback callback) {
            this.query = query;
            this.callback = callback;
        }
    }
}
