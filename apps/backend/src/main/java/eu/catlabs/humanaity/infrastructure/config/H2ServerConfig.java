package eu.catlabs.humanaity.infrastructure.config;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class H2ServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(H2ServerConfig.class);

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws SQLException {
        logger.info("Starting H2 TCP Server on port 9092...");
        Server server = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
        logger.info("H2 TCP Server started successfully");
        return server;
    }
}
