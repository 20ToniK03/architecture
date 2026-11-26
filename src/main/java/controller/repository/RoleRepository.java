package controller.repository;

import controller.role.RoleBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.SqlDao;
import model.data.RoleDO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Роль» (ЛР6, ЛР7).
 *
 * <p>Хранит роли в реляционном хранилище SQLite в виде {@link RoleDO}.
 * Список прав - простой список строк внутри DO-класса, доступ к данным -
 * через {@link SqlDao}.</p>
 */
public class RoleRepository implements IRepository<RoleBO> {

    private final SqlDao<RoleDO, Long> dao =
            new SqlDao<RoleDO, Long>(Database.sqlite(), RoleDO.class, "roles", "id");
    private final IdSequence ids = new IdSequence();

    public RoleRepository() {
        if (dao.findAll().isEmpty()) {
            seed();
        }
        ids.ensureAtLeast(nextId());
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        save(new RoleBO(1L, "GUEST", Arrays.asList("news.view")));
        save(new RoleBO(2L, "USER", Arrays.asList("news.view", "news.comment", "news.favorite")));
        save(new RoleBO(3L, "JOURNALIST", Arrays.asList("news.view", "news.create", "news.edit", "news.delete")));
        save(new RoleBO(4L, "ADMIN", Arrays.asList("user.block", "user.role", "comment.moderate")));
    }

    @Override
    public List<RoleBO> findAll() {
        List<RoleBO> result = new ArrayList<RoleBO>();
        for (RoleDO entity : dao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public RoleBO findById(Long id) {
        RoleDO entity = dao.findById(id);
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
        for (RoleDO entity : dao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private static RoleBO toBO(RoleDO entity) {
        return new RoleBO(entity.id, entity.name, new ArrayList<String>(entity.permissions));
    }

    private static RoleDO toDO(RoleBO item) {
        return new RoleDO(item.getId(), item.getName(), new ArrayList<String>(item.getPermissions()));
    }
}
