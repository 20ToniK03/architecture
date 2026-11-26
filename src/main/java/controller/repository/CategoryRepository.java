package controller.repository;

import controller.category.CategoryBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.SqlDao;
import model.data.CategoryDO;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Категория» (ЛР6, ЛР7).
 *
 * <p>Хранит категории в реляционном хранилище SQLite в виде {@link CategoryDO}
 * и преобразует их в {@link CategoryBO} и обратно. Начиная с ЛР7 доступ к
 * данным выполняется через {@link SqlDao}, а не через карту в памяти.</p>
 */
public class CategoryRepository implements IRepository<CategoryBO> {

    private final SqlDao<CategoryDO, Long> dao =
            new SqlDao<CategoryDO, Long>(Database.sqlite(), CategoryDO.class, "categories", "id");
    private final IdSequence ids = new IdSequence();

    public CategoryRepository() {
        if (dao.findAll().isEmpty()) {
            seed();
        }
        ids.ensureAtLeast(nextId());
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        save(new CategoryBO(1L, "Технологии", "IT, гаджеты и инновации"));
        save(new CategoryBO(2L, "Общество", "Городские события и жизнь горожан"));
        save(new CategoryBO(3L, "Спорт", "Соревнования и спортивные результаты"));
        save(new CategoryBO(4L, "Наука", "Открытия, исследования и образование"));
        save(new CategoryBO(5L, "Экономика", "Финансы, бизнес и экономическая политика"));
        save(new CategoryBO(6L, "Культура", "Выставки, театр, музыка и кино"));
    }

    @Override
    public List<CategoryBO> findAll() {
        List<CategoryBO> result = new ArrayList<CategoryBO>();
        for (CategoryDO entity : dao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public CategoryBO findById(Long id) {
        CategoryDO entity = dao.findById(id);
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
        dao.save(toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || dao.findById(id) == null) {
            return false;
        }
        dao.deleteById(id);
        return true;
    }

    private long nextId() {
        long max = 0L;
        for (CategoryDO entity : dao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private static CategoryBO toBO(CategoryDO entity) {
        return new CategoryBO(entity.id, entity.name, entity.description);
    }

    private static CategoryDO toDO(CategoryBO item) {
        return new CategoryDO(item.getId(), item.getName(), item.getDescription());
    }
}
