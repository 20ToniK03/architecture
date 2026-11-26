package model.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO для документного хранилища MongoDB (ЛР7).
 *
 * <p>Универсальный класс: работает с любым DO-классом через рефлексию. Каждое
 * поле DO-класса становится ключом BSON-документа, имя коллекции и имя
 * ключевого поля задаются при создании.</p>
 *
 * <p>Пример (из задания ЛР7):
 * {@code new NoSqlDao<>(mongoDatabase, NewsContentDO.class, "news_contents", "newsId")}.</p>
 *
 * <p>Если сервер MongoDB недоступен, {@code db} равен {@code null}, и DAO
 * прозрачно работает с временным хранилищем в памяти: приложение остаётся
 * работоспособным без внешней СУБД.</p>
 *
 * @param <T> тип DO-класса
 * @param <ID> тип ключа
 */
public class NoSqlDao<T, ID> implements IDao<T, ID> {

    private final Class<T> type;
    private final String idField;
    private final List<Field> columns = new ArrayList<Field>();
    private final MongoCollection<Document> collection;
    private final Map<Object, T> memory = new ConcurrentHashMap<Object, T>();

    public NoSqlDao(MongoDatabase db, Class<T> type, String collectionName, String idField) {
        this.type = type;
        this.idField = idField;
        this.collection = db == null ? null : db.getCollection(collectionName);
        collectColumns();
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

    @Override
    public void save(T entity) {
        if (entity == null) {
            return;
        }
        Object id = idOf(entity);
        if (collection == null) {
            memory.put(id, entity);
            return;
        }
        collection.replaceOne(new Document(idField, id), toDocument(entity),
                new ReplaceOptions().upsert(true));
    }

    @Override
    @SuppressWarnings("unchecked")
    public T findById(ID id) {
        if (id == null) {
            return null;
        }
        if (collection == null) {
            return memory.get(id);
        }
        Document document = collection.find(new Document(idField, id)).first();
        return document == null ? null : toEntity(document);
    }

    @Override
    public List<T> findAll() {
        if (collection == null) {
            return new ArrayList<T>(memory.values());
        }
        List<T> result = new ArrayList<T>();
        for (Document document : collection.find()) {
            result.add(toEntity(document));
        }
        return result;
    }

    @Override
    public void deleteById(ID id) {
        if (id == null) {
            return;
        }
        if (collection == null) {
            memory.remove(id);
            return;
        }
        collection.deleteOne(new Document(idField, id));
    }

    private Object idOf(T entity) {
        try {
            for (Field field : columns) {
                if (field.getName().equals(idField)) {
                    return field.get(entity);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Не удалось прочитать ключ " + idField + ": " + e.getMessage(), e);
        }
        return null;
    }

    private Document toDocument(T entity) {
        Document document = new Document();
        for (Field field : columns) {
            try {
                document.append(field.getName(), field.get(entity));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Не удалось прочитать поле " + field.getName() + ": " + e.getMessage(), e);
            }
        }
        return document;
    }

    private T toEntity(Document document) {
        try {
            T entity = type.getDeclaredConstructor().newInstance();
            for (Field field : columns) {
                Object value = document.get(field.getName());
                if (value != null) {
                    field.set(entity, value);
                }
            }
            return entity;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось восстановить объект из документа: " + e.getMessage(), e);
        }
    }
}
