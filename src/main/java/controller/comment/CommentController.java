package controller.comment;

import controller.news.NewsBO;
import controller.news.NewsInterface;
import controller.person.UserBO;
import model.IdSequence;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Комментарий» (ЛР5).
 *
 * <p>Отвечает за существование комментариев: кэш, идентификаторы, поиск по
 * новости и модерация. Связь с новостью поддерживается в двух местах:
 * в собственном кэше контроллера и в коллекции бизнес-объекта новости
 * (композиция {@code News *-- Comment}).</p>
 */
public class CommentController {

    private final InMemoryStore<CommentBO> cache = new InMemoryStore<CommentBO>();
    private final IdSequence ids = new IdSequence();

    private final NewsInterface newsController;

    public CommentController(NewsInterface newsController) {
        this.newsController = newsController;
    }

    /** Все комментарии из кэша. */
    public List<CommentBO> findAll() {
        return cache.findAll();
    }

    /**
     * Комментарий по идентификатору.
     *
     * @throws NoSuchElementException если комментария нет
     */
    public CommentBO findById(long id) {
        CommentBO comment = cache.find(id);
        if (comment == null) {
            throw new NoSuchElementException("Комментарий не найден: " + id);
        }
        return comment;
    }

    /** Комментарии конкретной новости. */
    public List<CommentBO> findByNews(long newsId) {
        List<CommentBO> result = new ArrayList<CommentBO>();
        for (CommentBO comment : cache.findAll()) {
            if (comment.getNewsId() != null && comment.getNewsId() == newsId) {
                result.add(comment);
            }
        }
        return result;
    }

    /** Создать комментарий и связать его с новостью. */
    public CommentBO create(NewsBO news, String text, UserBO author) {
        CommentBO comment = new CommentBO(ids.next(), news.getId(), text, new Date(), author);
        cache.put(comment.getId(), comment);
        news.addComment(comment);
        return comment;
    }

    /** Изменить текст комментария. */
    public CommentBO update(long id, String text) {
        CommentBO comment = findById(id);
        comment.edit(text);
        return comment;
    }

    /** Удалить комментарий и убрать его из коллекции новости. */
    public boolean delete(long id) {
        CommentBO comment = cache.find(id);
        if (comment == null) {
            return false;
        }
        cache.remove(id);
        if (comment.getNewsId() != null) {
            try {
                newsController.findById(comment.getNewsId()).removeComment(id);
            } catch (NoSuchElementException ignored) {
                // Новость уже удалена: убирать ссылку не из чего.
            }
        }
        return true;
    }

    /**
     * Промодерировать комментарий.
     *
     * @param action {@code approve} или {@code reject}
     * @return {@code false}, если действие неизвестно
     * @throws NoSuchElementException если комментария нет
     */
    public boolean moderate(long id, String action) {
        CommentBO comment = findById(id);
        if ("approve".equalsIgnoreCase(action)) {
            comment.setApproved(true);
            return true;
        }
        if ("reject".equalsIgnoreCase(action)) {
            comment.setApproved(false);
            return true;
        }
        return false;
    }
}
