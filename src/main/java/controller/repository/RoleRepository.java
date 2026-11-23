package controller.repository;

import controller.role.RoleBO;
import model.IdSequence;
import model.data.RoleDO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Роль» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Хранит роли в памяти в виде {@link RoleDO}. Список прав - простой список
 * строк внутри DO-класса.</p>
 */
public class RoleRepository implements IRepository<RoleBO> {

    private final ConcurrentSkipListMap<Long, RoleDO> store = new ConcurrentSkipListMap<Long, RoleDO>();
    private final IdSequence ids = new IdSequence();

    public RoleRepository() {
        seed();
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        save(new RoleBO(1L, "GUEST", Arrays.asList("news.view")));
        save(new RoleBO(2L, "USER", Arrays.asList("news.view", "news.comment", "news.favorite")));
        save(new RoleBO(3L, "JOURNALIST", Arrays.asList("news.view", "news.create", "news.edit", "news.delete")));
        save(new RoleBO(4L, "ADMIN", Arrays.asList("user.block", "user.role", "comment.moderate")));
        ids.ensureAtLeast(5L);
    }

    @Override
    public List<RoleBO> findAll() {
        List<RoleBO> result = new ArrayList<RoleBO>();
        for (RoleDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public RoleBO findById(Long id) {
        if (id == null) {
            return null;
        }
        RoleDO entity = store.get(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public RoleBO save(RoleBO item) {
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

    private static RoleBO toBO(RoleDO entity) {
        return new RoleBO(entity.id, entity.name, new ArrayList<String>(entity.permissions));
    }

    private static RoleDO toDO(RoleBO item) {
        return new RoleDO(item.getId(), item.getName(), new ArrayList<String>(item.getPermissions()));
    }
}
