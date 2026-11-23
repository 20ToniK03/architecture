package controller.person;

import controller.repository.UserRepository;
import controller.role.RoleBO;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Пользователь» (ЛР5, ЛР6).
 *
 * <p>Отвечает за существование пользователей: кэш, выдача идентификаторов,
 * регистрация, вход, блокировка и назначение ролей. Уникальность email,
 * как и любой поиск по кэшу, - тоже ответственность контроллера.
 * Бизнес-объект при этом проверяет только корректность своих полей.
 * Начиная с ЛР6 хранение данных вынесено в {@link UserRepository}.</p>
 */
public class UserController {

    private final InMemoryStore<UserBO> cache = new InMemoryStore<UserBO>();
    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (UserBO user : repository.findAll()) {
            cache.put(user.getId(), user);
        }
    }

    /** Все пользователи из кэша. */
    public List<UserBO> findAll() {
        return cache.findAll();
    }

    /**
     * Пользователь по идентификатору.
     *
     * @throws NoSuchElementException если пользователя нет
     */
    public UserBO findById(long id) {
        UserBO user = cache.find(id);
        if (user == null) {
            throw new NoSuchElementException("Пользователь не найден: " + id);
        }
        return user;
    }

    /** Пользователь по email или {@code null}. Регистр не важен. */
    public UserBO findByEmail(String email) {
        if (email == null) {
            return null;
        }
        for (UserBO user : cache.findAll()) {
            if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(email)) {
                return user;
            }
        }
        return null;
    }

    /** Пользователь по имени или {@code null}. */
    public UserBO findByName(String name) {
        if (name == null) {
            return null;
        }
        for (UserBO user : cache.findAll()) {
            if (user.getName() != null && user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        return null;
    }

    /** Занят ли email другим пользователем. */
    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    /**
     * Положить готовый бизнес-объект в кэш. Если идентификатора нет,
     * его выдаёт репозиторий: этим занимается хранилище, а не бизнес-объект.
     */
    public UserBO save(UserBO user) {
        repository.save(user);
        cache.put(user.getId(), user);
        return user;
    }

    /**
     * Зарегистрировать пользователя.
     *
     * @throws IllegalArgumentException если email уже занят
     */
    public UserBO register(RegistrationData data, RoleBO role) {
        if (data == null) {
            throw new IllegalArgumentException("Нет данных для регистрации");
        }
        if (data.getEmail() != null && existsByEmail(data.getEmail())) {
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        UserBO user = new UserBO(null, data.getName(), data.getEmail());
        user.setPasswordHash(data.getPassword());
        if (role != null) {
            user.addRole(role);
        }
        return save(user);
    }

    /** Проверить логин и пароль (делегирует бизнес-объекту). */
    public boolean login(String email, String password) {
        UserBO user = findByEmail(email);
        return user != null && user.login(email, password);
    }

    /** Заблокировать пользователя. {@code false}, если его нет. */
    public boolean block(long id) {
        UserBO user = cache.find(id);
        if (user == null) {
            return false;
        }
        user.setBlocked(true);
        repository.save(user);
        return true;
    }

    /** Назначить роль. {@code false}, если пользователя нет. */
    public boolean assignRole(long id, RoleBO role) {
        UserBO user = cache.find(id);
        if (user == null || role == null) {
            return false;
        }
        user.addRole(role);
        repository.save(user);
        return true;
    }

    /** Удалить пользователя из кэша и репозитория. {@code false}, если его не было. */
    public boolean delete(long id) {
        boolean removed = cache.remove(id);
        if (removed) {
            repository.delete(id);
        }
        return removed;
    }

    /** Идентификаторы всех пользователей (для отладочных сценариев). */
    public List<Long> findAllIds() {
        List<Long> result = new ArrayList<Long>();
        for (UserBO user : cache.findAll()) {
            result.add(user.getId());
        }
        return result;
    }
}
