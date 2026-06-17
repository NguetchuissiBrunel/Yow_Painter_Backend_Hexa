package com.yowpainter.shared.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class MultiTenantConnectionProviderImpl implements MultiTenantConnectionProvider {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(Object tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        String tenantSchema = tenantIdentifier != null ? tenantIdentifier.toString() : "public";
        try (Statement statement = connection.createStatement()) {
            if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")) {
                statement.execute("SET SCHEMA " + tenantSchema);
            } else {
                statement.execute("SET search_path TO " + tenantSchema + ", public");
            }
        } catch (SQLException e) {
            log.error("Failed to set search_path to {}, public for tenant connection", tenantSchema, e);
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(Object tenantIdentifier, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")) {
                statement.execute("SET SCHEMA PUBLIC");
            } else {
                statement.execute("RESET search_path");
            }
        } catch (SQLException e) {
            log.warn("Failed to reset search_path after tenant connection release", e);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
