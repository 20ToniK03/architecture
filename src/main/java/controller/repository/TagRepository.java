package controller.repository;

import controller.tag.TagBO;
import model.IdSequence;
import model.data.TagDO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Тег» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Хранит теги в памяти в виде {@link TagDO}. Уникальность названия
 * обеспечивает контроллер, репозиторий отвечает только за хранение и
 * преобразование в бизнес-объекты.</p>
 */
public class TagRepository implements IRepository<TagBO> {

    private final ConcurrentSkipListMap<Long, TagDO> store = new ConcurrentSkipListMap<Long, TagDO>();
    private final IdSequence ids = new IdSequence();

    public TagRepository() {
        seed();
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        int id = 1;
        for (String name : new String[]{"технологии", "портал", "метро", "транспорт",
                "спорт", "плавание", "наука", "фестиваль", "экономика", "форум",
                "культура", "архитектура", "кризис"}) {
            save(new TagBO((long) id++, name));
        }
        ids.ensureAtLeast(id);
    }

    @Override
    public List<TagBO> findAll() {
        List<TagBO> result = new ArrayList<TagBO>();
        for (TagDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public TagBO findById(Long id) {
        if (id == null) {
            return null;
        }
        TagDO entity = store.get(id);
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
        store.put(item.getId(), toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        return id != null && store.remove(id) != null;
    }

    private static TagBO toBO(TagDO entity) {
        return new TagBO(entity.id, entity.name);
    }

    private static TagDO toDO(TagBO item) {
        return new TagDO(item.getId(), item.getName());
    }
}
