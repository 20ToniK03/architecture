package controller.repository;

import controller.category.CategoryBO;
import model.IdSequence;
import model.data.CategoryDO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Категория» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Хранит категории в памяти в виде {@link CategoryDO} и преобразует их в
 * {@link CategoryBO} и обратно. Хранилище - потокобезопасная карта,
 * упорядоченная по идентификатору; в дальнейшем её можно заменить обращением
 * к настоящей СУБД, не меняя контроллер.</p>
 */
public class CategoryRepository implements IRepository<CategoryBO> {

    private final ConcurrentSkipListMap<Long, CategoryDO> store = new ConcurrentSkipListMap<Long, CategoryDO>();
    private final IdSequence ids = new IdSequence();

    public CategoryRepository() {
        seed();
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        save(new CategoryBO(1L, "Технологии", "IT, гаджеты и инновации"));
        save(new CategoryBO(2L, "Общество", "Городские события и жизнь горожан"));
        save(new CategoryBO(3L, "Спорт", "Соревнования и спортивные результаты"));
        save(new CategoryBO(4L, "Наука", "Открытия, исследования и образование"));
        save(new CategoryBO(5L, "Экономика", "Финансы, бизнес и экономическая политика"));
        save(new CategoryBO(6L, "Культура", "Выставки, театр, музыка и кино"));
        ids.ensureAtLeast(7L);
    }

    @Override
    public List<CategoryBO> findAll() {
        List<CategoryBO> result = new ArrayList<CategoryBO>();
        for (CategoryDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public CategoryBO findById(Long id) {
        if (id == null) {
            return null;
        }
        CategoryDO entity = store.get(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public CategoryBO save(CategoryBO item) {
        if (item == null) {
            return null;
        }
        if (item.getId() == null) {
            item.setId(ids.next());
        }
        store.put(item.getId(), toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        return id != null && store.remove(id) != null;
    }

    private static CategoryBO toBO(CategoryDO entity) {
        return new CategoryBO(entity.id, entity.name, entity.description);
    }

    private static CategoryDO toDO(CategoryBO item) {
        return new CategoryDO(item.getId(), item.getName(), item.getDescription());
    }
}
