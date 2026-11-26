package model.dao;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO для реляционного хранилища SQLite (ЛР7).
 *
 * <p>Универсальный класс: работает с любым DO-классом через рефлексию.
 * Структура таблицы строится по полям DO-класса: строковые поля и списки
 * хранятся как {@code TEXT} (списки - в виде JSON), целочисленные - как
 * {@code INTEGER}, вещественные - как {@code REAL}. Имя таблицы и имя
 * ключевого столбца задаются при создании.</p>
 *
 * <p>Пример (из задания ЛР7):
 * {@code new SqlDao<>(connection, NewsDO.class, "news", "id")}.</p>
 *
 * @param <T> тип DO-класса
 * @param <ID> тип ключа
 */
public class SqlDao<T, ID> implements IDao<T, ID> {

    private final Connection connection;
    private final Class<T> type;
    private final String table;
    private final String idColumn;
    private final List<Field> columns = new ArrayList<Field>();
    private final ObjectMapper json = new ObjectMapper();

    public SqlDao(Connection connection, Class<T> type, String table, String idColumn) {
        this.connection = connection;
        this.type = type;
        this.table = table;
        this.idColumn = idColumn;
        collectColumns();
        createTable();
    }

    private void collectColumns() {
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            columns.add(field);
        }
    }

    private void createTable() {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(table).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            Field field = columns.get(i);
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(field.getName()).append(' ').append(sqlType(field));
            if (field.getName().equals(idColumn)) {
                sql.append(" PRIMARY KEY");
            }
        }
        sql.append(')');
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql.toString());
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось создать таблицу " + table + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void save(T entity) {
        if (entity == null) {
            return;
        }
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                cols.append(", ");
                placeholders.append(", ");
            }
            cols.append(columns.get(i).getName());
            placeholders.append('?');
        }
        String sql = "INSERT OR REPLACE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < columns.size(); i++) {
                bind(statement, i + 1, columns.get(i), columns.get(i).get(entity));
            }
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось сохранить объект в таблицу " + table + ": " + e.getMessage(), e);
        }
    }

    @Override
    public T findById(ID id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT * FROM " + table + " WHERE " + idColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? toEntity(result) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать объект из таблицы " + table + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> findAll() {
        List<T> result = new ArrayList<T>();
        String sql = "SELECT * FROM " + table;
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.add(toEntity(rs));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать таблицу " + table + ": " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void deleteById(ID id) {
        if (id == null) {
            return;
        }
        String sql = "DELETE FROM " + table + " WHERE " + idColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось удалить объект из таблицы " + table + ": " + e.getMessage(), e);
        }
    }

    private T toEntity(ResultSet rs) throws Exception {
        T entity = type.getDeclaredConstructor().newInstance();
        for (Field field : columns) {
            field.set(entity, read(rs, field));
        }
        return entity;
    }

    private void bind(PreparedStatement statement, int index, Field field, Object value) throws Exception {
        Class<?> c = field.getType();
        if (value == null) {
            statement.setObject(index, null);
        } else if (c == boolean.class || c == Boolean.class) {
            statement.setInt(index, ((Boolean) value) ? 1 : 0);
        } else if (List.class.isAssignableFrom(c)) {
            statement.setString(index, json.writeValueAsString(value));
        } else {
            statement.setObject(index, value);
        }
    }

    private Object read(ResultSet rs, Field field) throws Exception {
        Class<?> c = field.getType();
        if (List.class.isAssignableFrom(c)) {
            String value = rs.getString(field.getName());
            if (value == null || value.isEmpty()) {
                return new ArrayList<Object>();
            }
            JavaType javaType = json.getTypeFactory().constructType(field.getGenericType());
            return json.readValue(value, javaType);
        }
        if (c == boolean.class || c == Boolean.class) {
            return rs.getInt(field.getName()) != 0;
        }
        if (c == long.class) {
            return rs.getLong(field.getName());
        }
        if (c == int.class) {
            return rs.getInt(field.getName());
        }
        if (c == double.class) {
            return rs.getDouble(field.getName());
        }
        if (c == Long.class) {
            long value = rs.getLong(field.getName());
            return rs.wasNull() ? null : Long.valueOf(value);
        }
        if (c == Integer.class) {
            int value = rs.getInt(field.getName());
            return rs.wasNull() ? null : Integer.valueOf(value);
        }
        if (c == Double.class) {
            double value = rs.getDouble(field.getName());
            return rs.wasNull() ? null : Double.valueOf(value);
        }
        return rs.getString(field.getName());
    }

    private static String sqlType(Field field) {
        Class<?> c = field.getType();
        if (c == long.class || c == Long.class
                || c == int.class || c == Integer.class
                || c == boolean.class || c == Boolean.class) {
            return "INTEGER";
        }
        if (c == double.class || c == Double.class) {
            return "REAL";
        }
        return "TEXT";
    }
}
