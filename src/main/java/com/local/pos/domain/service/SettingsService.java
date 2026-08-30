package com.local.pos.domain.service;

import com.local.pos.core.TechnicalException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class SettingsService {

    public static final String KEY_STORE_NAME = "STORE_NAME";
    public static final String KEY_STORE_ADDRESS = "STORE_ADDRESS";
    public static final String KEY_STORE_PHONE = "STORE_PHONE";
    public static final String KEY_TICKET_FOOTER = "TICKET_FOOTER";

    private static final Map<String, String> DEFAULTS = Map.of(
        KEY_STORE_NAME, "Mi Local",
        KEY_STORE_ADDRESS, "",
        KEY_STORE_PHONE, "",
        KEY_TICKET_FOOTER, "Gracias por su compra"
    );

    private final DataSource dataSource;
    private final Map<String, String> cache = new HashMap<>();

    public SettingsService(DataSource dataSource) {
        this.dataSource = dataSource;
        load();
    }

    private void load() {
        cache.clear();
        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT SETTING_KEY, SETTING_VALUE FROM SETTING");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cache.put(rs.getString(1), rs.getString(2));
                }
            }
        } catch (SQLException e) {
            throw new TechnicalException("Error al leer la configuración", e);
        }

        DEFAULTS.forEach((key, value) -> cache.putIfAbsent(key, value));
    }

    public String getStoreName() {
        return cache.get(KEY_STORE_NAME);
    }

    public String getStoreAddress() {
        return cache.get(KEY_STORE_ADDRESS);
    }

    public String getStorePhone() {
        return cache.get(KEY_STORE_PHONE);
    }

    public String getTicketFooter() {
        return cache.get(KEY_TICKET_FOOTER);
    }

    public void saveAll(String name, String address, String phone, String footer) {
        upsert(KEY_STORE_NAME, name);
        upsert(KEY_STORE_ADDRESS, address);
        upsert(KEY_STORE_PHONE, phone);
        upsert(KEY_TICKET_FOOTER, footer);
        load();
    }

    private void upsert(String key, String value) {
        String sql = """
                MERGE INTO SETTING (SETTING_KEY, SETTING_VALUE) KEY(SETTING_KEY)
                VALUES (?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value == null ? "" : value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new TechnicalException("Error al guardar la configuración", e);
        }
    }
}
