package model.dao;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Настройки подключения приложения к СУБД (ЛР7).
 *
 * <p>Содержит параметры подключения к SQLite (реляционное хранилище) и
 * MongoDB (документное хранилище), а также открывает соединения. SQLite -
 * встроенная файловая СУБД, поэтому доступна всегда. MongoDB - клиент-серверная
 * СУБД: если сервер не запущен, {@link #mongo()} возвращает {@code null}, и
 * репозитории продолжают работать с временным хранилищем в памяти.</p>
 */
public final class Database {

    /** Файл базы SQLite (создаётся рядом с приложением). */
    public static final String SQLITE_URL = "jdbc:sqlite:news.db";

    /** Адрес сервера MongoDB и имя базы данных. */
    public static final String MONGO_URI = "mongodb://localhost:27017/?serverSelectionTimeoutMS=2000";
    public static final String MONGO_DB_NAME = "newsdb";

    private static Connection sqliteConnection;
    private static MongoDatabase mongoDatabase;
    private static boolean mongoChecked;

    private Database() {
    }

    /** Открывает (однократно) соединение с SQLite. */
    public static synchronized Connection sqlite() {
        if (sqliteConnection == null) {
            try {
                sqliteConnection = DriverManager.getConnection(SQLITE_URL);
            } catch (SQLException e) {
                throw new IllegalStateException("Не удалось подключиться к SQLite: " + e.getMessage(), e);
            }
        }
        return sqliteConnection;
    }

    /**
     * Открывает (однократно) подключение к MongoDB.
     *
     * @return база данных MongoDB или {@code null}, если сервер недоступен
     */
    public static synchronized MongoDatabase mongo() {
        if (!mongoChecked) {
            mongoChecked = true;
            try {
                MongoClient client = MongoClients.create(new ConnectionString(MONGO_URI));
                MongoDatabase db = client.getDatabase(MONGO_DB_NAME);
                // Проверка доступности: если сервера нет, здесь будет исключение.
                db.listCollectionNames().first();
                mongoDatabase = db;
            } catch (RuntimeException e) {
                mongoDatabase = null;
            }
        }
        return mongoDatabase;
    }

    /** Закрывает соединение с SQLite (для корректного завершения). */
    public static synchronized void close() {
        if (sqliteConnection != null) {
            try {
                sqliteConnection.close();
            } catch (SQLException ignored) {
                // Игнорируем: приложение завершается.
            }
            sqliteConnection = null;
        }
    }
}
