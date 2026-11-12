package controller.role;

import model.IdSequence;
import model.InMemoryStore;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Роль» (ЛР5).
 *
 * <p>Отвечает за существование ролей: кэш, выдача идентификаторов, поиск
 * по названию. Список прав хранится внутри {@link RoleBO}.</p>
 */
public class RoleController {

    private final InMemoryStore<RoleBO> cache = new InMemoryStore<RoleBO>();
    private final IdSequence ids = new IdSequence();

    /** Все роли из кэша. */
    public List<RoleBO> findAll() {
        return cache.findAll();
    }

    /**
     * Роль по идентификатору.
     *
     * @throws NoSuchElementException если роли с таким идентификатором нет
     */
    public RoleBO findById(long id) {
        RoleBO role = cache.find(id);
        if (role == null) {
            throw new NoSuchElementException("Роль не найдена: " + id);
        }
        return role;
    }

    /** Роль по названию или {@code null}. */
    public RoleBO findByName(String name) {
        if (name == null) {
            return null;
        }
        for (RoleBO role : cache.findAll()) {
            if (role.getName() != null && role.getName().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return null;
    }

    /** Создать роль и положить её в кэш. */
    public RoleBO create(String name, List<String> permissions) {
        RoleBO role = new RoleBO(ids.next(), name, permissions);
        cache.put(role.getId(), role);
        return role;
    }

    /** Удалить роль. {@code false}, если её не было. */
    public boolean delete(long id) {
        return cache.remove(id);
    }
}
