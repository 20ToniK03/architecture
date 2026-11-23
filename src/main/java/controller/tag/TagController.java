package controller.tag;

import controller.repository.TagRepository;
import model.InMemoryStore;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Тег» (ЛР5, ЛР6).
 *
 * <p>Хранит теги в кэше и отвечает за их существование. Метод
 * {@link #findOrCreate(String)} совмещает поиск и создание: теги уникальны
 * по названию без учёта регистра. Начиная с ЛР6 хранение данных вынесено в
 * {@link TagRepository}.</p>
 */
public class TagController {

    private final InMemoryStore<TagBO> cache = new InMemoryStore<TagBO>();
    private final TagRepository repository;

    public TagController(TagRepository repository) {
        this.repository = repository;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (TagBO tag : repository.findAll()) {
            cache.put(tag.getId(), tag);
        }
    }

    /** Все теги из кэша. */
    public List<TagBO> findAll() {
        return cache.findAll();
    }

    /**
     * Тег по идентификатору.
     *
     * @throws NoSuchElementException если тега с таким идентификатором нет
     */
    public TagBO findById(long id) {
        TagBO tag = cache.find(id);
        if (tag == null) {
            throw new NoSuchElementException("Тег не найден: " + id);
        }
        return tag;
    }

    /** Тег по названию или {@code null}. */
    public TagBO findByName(String name) {
        if (name == null) {
            return null;
        }
        for (TagBO tag : cache.findAll()) {
            if (tag.getName() != null && tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        return null;
    }

    /** Найти тег по названию или создать новый. */
    public TagBO findOrCreate(String name) {
        TagBO existing = findByName(name);
        if (existing != null) {
            return existing;
        }
        return create(name);
    }

    /** Создать тег и сохранить его в репозитории. */
    public TagBO create(String name) {
        TagBO tag = new TagBO(null, name);
        repository.save(tag);
        cache.put(tag.getId(), tag);
        return tag;
    }

    /** Удалить тег. {@code false}, если его не было. */
    public boolean delete(long id) {
        boolean removed = cache.remove(id);
        if (removed) {
            repository.delete(id);
        }
        return removed;
    }
}
