package controller.repository;

import controller.favorite.FavoriteBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.SqlDao;
import model.data.FavoriteDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Избранное» (ЛР6, ЛР7).
 *
 * <p>Хранит связи «пользователь - новость» в реляционном хранилище SQLite в
 * виде {@link FavoriteDO}. Ссылки хранятся идентификаторами, поэтому сам
 * пользователь и новость живут в своих репозиториях. Тестовый набор пуст:
 * избранное появляется только при работе пользователя с сервисом.</p>
 */
public class FavoriteRepository implements IRepository<FavoriteBO> {

    private final SqlDao<FavoriteDO, Long> dao =
            new SqlDao<FavoriteDO, Long>(Database.sqlite(), FavoriteDO.class, "favorites", "id");
    private final IdSequence ids = new IdSequence();

    public FavoriteRepository() {
        // Тестовый набор данных пуст (задача 3 ЛР6).
        ids.ensureAtLeast(nextId());
    }

    @Override
    public List<FavoriteBO> findAll() {
        List<FavoriteBO> result = new ArrayList<FavoriteBO>();
        for (FavoriteDO entity : dao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public FavoriteBO findById(Long id) {
        FavoriteDO entity = dao.findById(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public FavoriteBO save(FavoriteBO item) {
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
        for (FavoriteDO entity : dao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private static FavoriteBO toBO(FavoriteDO entity) {
        return new FavoriteBO(entity.id,
                entity.addedAt > 0L ? new Date(entity.addedAt) : null,
                entity.userId, entity.newsId);
    }

    private static FavoriteDO toDO(FavoriteBO item) {
        FavoriteDO entity = new FavoriteDO();
        entity.id = item.getId();
        entity.userId = item.getUserId();
        entity.newsId = item.getNewsId();
        entity.addedAt = item.getAddedAt() == null ? 0L : item.getAddedAt().getTime();
        return entity;
    }
}
