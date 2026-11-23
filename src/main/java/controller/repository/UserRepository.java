package controller.repository;

import controller.person.AdminBO;
import controller.person.JournalistBO;
import controller.person.UserBO;
import controller.role.RoleBO;
import model.IdSequence;
import model.data.UserDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Пользователь» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Один DO-класс {@link UserDO} описывает все виды персон. Поле {@code type}
 * хранит конкретный класс бизнес-объекта, поэтому при загрузке восстанавливается
 * нужный наследник {@link UserBO}: журналист, администратор или обычный
 * пользователь. Роли восстанавливаются из {@link RoleRepository}.</p>
 */
public class UserRepository implements IRepository<UserBO> {

    /** Email журналиста по умолчанию (автор создаваемых новостей). */
    public static final String JOURNALIST_EMAIL = "maria@example.com";

    /** Email тестового читателя. */
    public static final String READER_EMAIL = "ivan@example.com";

    /** Email администратора. */
    public static final String ADMIN_EMAIL = "admin@example.com";

    private static final String TYPE_JOURNALIST = "JOURNALIST";
    private static final String TYPE_ADMIN = "ADMIN";
    private static final String TYPE_USER = "USER";

    private final ConcurrentSkipListMap<Long, UserDO> store = new ConcurrentSkipListMap<Long, UserDO>();
    private final IdSequence ids = new IdSequence();
    private final RoleRepository roleRepository;

    public UserRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
        seed();
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        JournalistBO journalist = new JournalistBO(1L, "Мария Соколова", JOURNALIST_EMAIL);
        journalist.setPasswordHash("pass");
        journalist.setRating(4.8);
        journalist.addRole(roleRepository.findById(3L));
        save(journalist);

        UserBO reader = new UserBO(2L, "Иван Петров", READER_EMAIL);
        reader.setPasswordHash("pass");
        reader.addRole(roleRepository.findById(2L));
        save(reader);

        AdminBO admin = new AdminBO(3L, "Алексей Админов", ADMIN_EMAIL);
        admin.setPasswordHash("admin");
        admin.addRole(roleRepository.findById(4L));
        save(admin);

        ids.ensureAtLeast(4L);
    }

    @Override
    public List<UserBO> findAll() {
        List<UserBO> result = new ArrayList<UserBO>();
        for (UserDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public UserBO findById(Long id) {
        if (id == null) {
            return null;
        }
        UserDO entity = store.get(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public UserBO save(UserBO item) {
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

    private UserBO toBO(UserDO entity) {
        UserBO user;
        if (TYPE_JOURNALIST.equals(entity.type)) {
            JournalistBO journalist = new JournalistBO(entity.id, entity.name, entity.email);
            journalist.setRating(entity.rating);
            user = journalist;
        } else if (TYPE_ADMIN.equals(entity.type)) {
            user = new AdminBO(entity.id, entity.name, entity.email);
        } else {
            user = new UserBO(entity.id, entity.name, entity.email);
        }
        user.setPasswordHash(entity.passwordHash);
        if (entity.registrationDate > 0L) {
            user.setRegistrationDate(new Date(entity.registrationDate));
        }
        user.setBlocked(entity.blocked);
        for (Long roleId : entity.roleIds) {
            RoleBO role = findRole(roleId);
            if (role != null) {
                user.addRole(role);
            }
        }
        return user;
    }

    private UserDO toDO(UserBO item) {
        UserDO entity = new UserDO();
        entity.id = item.getId();
        entity.type = typeOf(item);
        entity.name = item.getName();
        entity.email = item.getEmail();
        entity.passwordHash = item.getPasswordHash();
        entity.registrationDate = item.getRegistrationDate() == null
                ? 0L : item.getRegistrationDate().getTime();
        entity.blocked = item.isBlocked();
        for (RoleBO role : item.getRoles()) {
            if (role != null && role.getId() != null) {
                entity.roleIds.add(role.getId());
            }
        }
        if (item instanceof JournalistBO) {
            entity.rating = ((JournalistBO) item).getRating();
        }
        return entity;
    }

    private RoleBO findRole(Long id) {
        if (id == null) {
            return null;
        }
        for (RoleBO role : roleRepository.findAll()) {
            if (id.equals(role.getId())) {
                return role;
            }
        }
        return null;
    }

    private static String typeOf(UserBO item) {
        if (item instanceof JournalistBO) {
            return TYPE_JOURNALIST;
        }
        if (item instanceof AdminBO) {
            return TYPE_ADMIN;
        }
        return TYPE_USER;
    }
}
