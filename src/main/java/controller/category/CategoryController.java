package controller.category;

import controller.repository.CategoryRepository;
import model.InMemoryStore;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Категория» (ЛР5, ЛР6).
 *
 * <p>Отвечает за существование экземпляров {@link CategoryBO}: хранит их в кэше,
 * выдаёт идентификаторы и выполняет операции чтения/создания/изменения/удаления.
 * Проверка корректности полей - задача самого бизнес-объекта, преобразование
 * в DTO - задача фасада. Начиная с ЛР6 хранение данных вынесено в
 * {@link CategoryRepository}: кэш загружается из репозитория, а каждая
 * изменяющая операция сразу сохраняется в нём.</p>
 */
public class CategoryController {

    private final InMemoryStore<CategoryBO> cache = new InMemoryStore<CategoryBO>();
    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (CategoryBO category : repository.findAll()) {
            cache.put(category.getId(), category);
        }
    }

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

    /** Создать категорию и сохранить её в репозитории. */
    public CategoryBO create(String name, String description) {
        CategoryBO category = new CategoryBO(null, name, description);
        repository.save(category);
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
        repository.save(category);
        return category;
    }

    /** Удалить категорию. {@code false}, если её не было. */
    public boolean delete(long id) {
        boolean removed = cache.remove(id);
        if (removed) {
            repository.delete(id);
        }
        return removed;
    }
}
