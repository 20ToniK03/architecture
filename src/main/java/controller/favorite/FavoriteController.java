package controller.favorite;

import model.IdSequence;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Контроллер сущности «Избранное» (ЛР5).
 *
 * <p>Отвечает за существование записей избранного: хранит их в кэше и
 * не допускает дублей «пользователь + новость». Сама новость и пользователь
 * здесь не изменяются, связи хранятся идентификаторами.</p>
 */
public class FavoriteController {

    private final InMemoryStore<FavoriteBO> cache = new InMemoryStore<FavoriteBO>();
    private final IdSequence ids = new IdSequence();

    /** Все записи избранного из кэша. */
    public List<FavoriteBO> findAll() {
        return cache.findAll();
    }

    /** Избранное конкретного пользователя. */
    public List<FavoriteBO> findByUser(long userId) {
        List<FavoriteBO> result = new ArrayList<FavoriteBO>();
        for (FavoriteBO favorite : cache.findAll()) {
            if (favorite.getUserId() != null && favorite.getUserId() == userId) {
                result.add(favorite);
            }
        }
        return result;
    }

    /** Добавить новость в избранное. Повторное добавление ничего не меняет. */
    public boolean add(long userId, long newsId) {
        if (find(userId, newsId) != null) {
            return true;
        }
        FavoriteBO favorite = new FavoriteBO(ids.next(), new Date(), userId, newsId);
        cache.put(favorite.getId(), favorite);
        return true;
    }

    /** Убрать новость из избранного. {@code false}, если записи не было. */
    public boolean remove(long userId, long newsId) {
        FavoriteBO favorite = find(userId, newsId);
        if (favorite == null) {
            return false;
        }
        return cache.remove(favorite.getId());
    }

    /** Удалить всё избранное пользователя (например, при удалении пользователя). */
    public void removeByUser(long userId) {
        for (FavoriteBO favorite : findByUser(userId)) {
            cache.remove(favorite.getId());
        }
    }

    private FavoriteBO find(long userId, long newsId) {
        for (FavoriteBO favorite : cache.findAll()) {
            if (favorite.getUserId() != null && favorite.getUserId() == userId
                    && favorite.getNewsId() != null && favorite.getNewsId() == newsId) {
                return favorite;
            }
        }
        return null;
    }
}
