package model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Потокобезопасный кэш бизнес-объектов, имитирующий хранилище в памяти.
 *
 * <p>Класс относится к модулю модели и ничего не знает о бизнес-объектах и DTO:
 * он параметризован типом {@code T}. Каждый контроллер бизнес-объекта держит
 * собственный экземпляр кэша, поэтому за существование объектов разных типов
 * отвечают разные контроллеры.</p>
 *
 * <p>Данные хранятся в {@link ConcurrentSkipListMap}: она потокобезопасна и
 * упорядочена по идентификатору, то есть по порядку создания объектов. HTTP-
 * запросы могут обрабатываться параллельно, поэтому все операции чтения и
 * записи должны быть безопасны без внешней синхронизации.</p>
 *
 * @param <T> тип хранимой сущности
 */
public class InMemoryStore<T> {

    private final ConcurrentSkipListMap<Long, T> data = new ConcurrentSkipListMap<Long, T>();

    /** Сохраняет элемент по ключу (перезаписывает существующий). */
    public void put(Long id, T item) {
        data.put(id, item);
    }

    /** Возвращает элемент по ключу или {@code null}, если его нет. */
    public T find(Long id) {
        return data.get(id);
    }

    /** Проверяет наличие элемента с указанным ключом. */
    public boolean contains(Long id) {
        return data.containsKey(id);
    }

    /** Возвращает снимок всех элементов в порядке возрастания идентификаторов. */
    public List<T> findAll() {
        return new ArrayList<T>(data.values());
    }

    /** Удаляет элемент по ключу. Возвращает {@code true}, если элемент был. */
    public boolean remove(Long id) {
        return data.remove(id) != null;
    }

    /** Количество элементов в кэше. */
    public int size() {
        return data.size();
    }

    /** Удаляет все элементы. */
    public void clear() {
        data.clear();
    }
}
