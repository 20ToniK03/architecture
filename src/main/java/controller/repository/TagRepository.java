package controller.repository;

import controller.tag.TagBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.SqlDao;
import model.data.TagDO;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Тег» (ЛР6, ЛР7).
 *
 * <p>Хранит теги в реляционном хранилище SQLite в виде {@link TagDO}.
 * Уникальность названия обеспечивает контроллер, репозиторий отвечает только
 * за хранение и преобразование в бизнес-объекты через {@link SqlDao}.</p>
 */
public class TagRepository implements IRepository<TagBO> {

    private final SqlDao<TagDO, Long> dao =
            new SqlDao<TagDO, Long>(Database.sqlite(), TagDO.class, "tags", "id");
    private final IdSequence ids = new IdSequence();

    public TagRepository() {
        if (dao.findAll().isEmpty()) {
            seed();
        }
        ids.ensureAtLeast(nextId());
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        int id = 1;
        for (String name : new String[]{"технологии", "портал", "метро", "транспорт",
                "спорт", "плавание", "наука", "фестиваль", "экономика", "форум",
                "культура", "архитектура", "кризис"}) {
            save(new TagBO((long) id++, name));
        }
    }

    @Override
    public List<TagBO> findAll() {
        List<TagBO> result = new ArrayList<TagBO>();
        for (TagDO entity : dao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public TagBO findById(Long id) {
        TagDO entity = dao.findById(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public TagBO save(TagBO item) {
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
        for (TagDO entity : dao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private static TagBO toBO(TagDO entity) {
        return new TagBO(entity.id, entity.name);
    }

    private static TagDO toDO(TagBO item) {
        return new TagDO(item.getId(), item.getName());
    }
}
