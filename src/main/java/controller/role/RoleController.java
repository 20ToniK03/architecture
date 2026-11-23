package controller.role;

import controller.repository.RoleRepository;
import model.InMemoryStore;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Роль» (ЛР5, ЛР6).
 *
 * <p>Отвечает за существование ролей: кэш, выдача идентификаторов, поиск
 * по названию. Список прав хранится внутри {@link RoleBO}. Начиная с ЛР6
 * хранение данных вынесено в {@link RoleRepository}.</p>
 */
public class RoleController {

    private final InMemoryStore<RoleBO> cache = new InMemoryStore<RoleBO>();
    private final RoleRepository repository;

    public RoleController(RoleRepository repository) {
        this.repository = repository;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (RoleBO role : repository.findAll()) {
            cache.put(role.getId(), role);
        }
    }

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

    /** Создать роль и сохранить её в репозитории. */
    public RoleBO create(String name, List<String> permissions) {
        RoleBO role = new RoleBO(null, name, permissions);
        repository.save(role);
        cache.put(role.getId(), role);
        return role;
    }

    /** Удалить роль. {@code false}, если её не было. */
    public boolean delete(long id) {
        boolean removed = cache.remove(id);
        if (removed) {
            repository.delete(id);
        }
        return removed;
    }
}
