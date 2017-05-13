# Acamar
Acamar is an open source service for pinging Minecraft servers and write results into MySQL backend.
It supports all Minecraft versions from 1.4 to 1.11 and probably future versions.

## Prerequisites
- Java 8 or newer
- Running MySQL or another compatible database (MariaDB, Amazon Aurora)

## Installation
1. Grab the latest version from the releases
2. Run `java -Xmx128M -jar acamar-VERSION.jar`
3. Edit `config.json` file
4. Restart application

## Configuration
```D
{
  "pollDelay": 3000, // Delay between pings in milliseconds
  "timeout": 2000, // If the server does not respond after this time, then it will be considered offline
  "threads": 3, // Maximum amount of concurrent ping requests
  "mysql": {
    "url": "jdbc:mysql://127.0.0.1/database?useUnicode=true&characterEncoding=utf-8", // JDBC URL
    "user": "root", // Username for database
    "pass": "", // Password for database
    // Next query will be executed every time when server is online
    "onlineQuery": "UPDATE servers SET updated = {time}, online = {online}, max = {max} WHERE id = {id}",
    // Offline query will be executed every time when server is offline
    "offlineQuery": "UPDATE servers SET updated = 1 WHERE id = {id}",
    // Query will be executed only once, when nothing is updated with any of previous queries
    "insertQuery": "INSERT IGNORE INTO servers (id) VALUES ({id})"
  },
  "servers": {
    "hypixel": { // Server ID
      "host": "mc.hypixel.net", // Server hostname or IP
      "port": 25565, // Server port
      "version": "1.9" // Server version (1.6, 1.7, 1.8, 1.9, 1.10 and so on)
    },
    "vimeworld": { // One more server
      "host": "vimeworld.net",
      "port": 25565,
      "version": "1.6"
    }
  }
}
```
In MySQL queries you can insert some data from ping result:
- `{id}` - Server ID from config
- `{motd}` - Server's Message of the day (description in servers list)
- `{online}` - Amount of players online
- `{max}` - Maximum amount of players
- `{time}` - Current unix timestamp in seconds

If you don't have suitable table in your database, you can create one:
```SQL
CREATE TABLE `servers` (
  `id` varchar(30) NOT NULL DEFAULT '',
  `updated` int(11) DEFAULT '0',
  `max` int(11) DEFAULT '0',
  `online` int(11) DEFAULT '0',
  `motd` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
