package controller.favorite;

import controller.repository.FavoriteRepository;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Контроллер сущности «Избранное» (ЛР5, ЛР6).
 *
 * <p>Отвечает за существование записей избранного: хранит их в кэше и
 * не допускает дублей «пользователь + новость». Сама новость и пользователь
 * здесь не изменяются, связи хранятся идентификаторами. Начиная с ЛР6
 * хранение данных вынесено в {@link FavoriteRepository}.</p>
 */
public class FavoriteController {

    private final InMemoryStore<FavoriteBO> cache = new InMemoryStore<FavoriteBO>();
    private final FavoriteRepository repository;

    public FavoriteController(FavoriteRepository repository) {
        this.repository = repository;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (FavoriteBO favorite : repository.findAll()) {
            cache.put(favorite.getId(), favorite);
        }
    }

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
        FavoriteBO favorite = new FavoriteBO(null, new Date(), userId, newsId);
        repository.save(favorite);
        cache.put(favorite.getId(), favorite);
        return true;
    }

    /** Убрать новость из избранного. {@code false}, если записи не было. */
    public boolean remove(long userId, long newsId) {
        FavoriteBO favorite = find(userId, newsId);
        if (favorite == null) {
            return false;
        }
        cache.remove(favorite.getId());
        repository.delete(favorite.getId());
        return true;
    }

    /** Удалить всё избранное пользователя (например, при удалении пользователя). */
    public void removeByUser(long userId) {
        for (FavoriteBO favorite : findByUser(userId)) {
            cache.remove(favorite.getId());
            repository.delete(favorite.getId());
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
