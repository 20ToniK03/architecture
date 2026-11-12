package controller.category;

import model.IdSequence;
import model.InMemoryStore;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Категория» (ЛР5).
 *
 * <p>Отвечает за существование экземпляров {@link CategoryBO}: хранит их в кэше,
 * выдаёт идентификаторы и выполняет операции чтения/создания/изменения/удаления.
 * Проверка корректности полей - задача самого бизнес-объекта, преобразование
 * в DTO - задача фасада.</p>
 */
public class CategoryController {

    private final InMemoryStore<CategoryBO> cache = new InMemoryStore<CategoryBO>();
    private final IdSequence ids = new IdSequence();

    /** Все категории из кэша. */
    public List<CategoryBO> findAll() {
        return cache.findAll();
    }

    /**
     * Категория по идентификатору.
     *
     * @throws NoSuchElementException если категории с таким идентификатором нет
     */
    public CategoryBO findById(long id) {
        CategoryBO category = cache.find(id);
        if (category == null) {
            throw new NoSuchElementException("Категория не найдена: " + id);
        }
        return category;
    }

    /** Категория по названию или {@code null}, если такой нет. */
    public CategoryBO findByName(String name) {
        if (name == null) {
            return null;
        }
        for (CategoryBO category : cache.findAll()) {
            if (category.getName() != null && category.getName().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    /** Создать категорию и положить её в кэш. */
    public CategoryBO create(String name, String description) {
        CategoryBO category = new CategoryBO(ids.next(), name, description);
        cache.put(category.getId(), category);
        return category;
    }

    /** Изменить поля категории. {@code null} означает «не менять». */
    public CategoryBO update(long id, String name, String description) {
        CategoryBO category = findById(id);
        if (name != null) {
            category.setName(name);
        }
        if (description != null) {
            category.setDescription(description);
        }
        return category;
    }

    /** Удалить категорию. {@code false}, если её не было. */
    public boolean delete(long id) {
        return cache.remove(id);
    }
}
