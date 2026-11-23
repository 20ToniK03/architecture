package controller.repository;

import controller.favorite.FavoriteBO;
import model.IdSequence;
import model.data.FavoriteDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Избранное» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Хранит связи «пользователь - новость» в виде {@link FavoriteDO}. Ссылки
 * хранятся идентификаторами, поэтому сам пользователь и новость живут в своих
 * репозиториях. Тестовый набор пуст: избранное появляется только при работе
 * пользователя с сервисом.</p>
 */
public class FavoriteRepository implements IRepository<FavoriteBO> {

    private final ConcurrentSkipListMap<Long, FavoriteDO> store = new ConcurrentSkipListMap<Long, FavoriteDO>();
    private final IdSequence ids = new IdSequence();

    public FavoriteRepository() {
        // Тестовый набор данных пуст (задача 3 ЛР6).
        ids.ensureAtLeast(1L);
    }

    @Override
    public List<FavoriteBO> findAll() {
        List<FavoriteBO> result = new ArrayList<FavoriteBO>();
        for (FavoriteDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public FavoriteBO findById(Long id) {
        if (id == null) {
            return null;
        }
        FavoriteDO entity = store.get(id);
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
        store.put(item.getId(), toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        return id != null && store.remove(id) != null;
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
