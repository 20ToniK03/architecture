package controller.comment;

import controller.news.NewsBO;
import controller.news.NewsInterface;
import controller.person.UserBO;
import controller.repository.CommentRepository;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Контроллер сущности «Комментарий» (ЛР5, ЛР6).
 *
 * <p>Отвечает за существование комментариев: кэш, поиск по новости и модерация.
 * Связь с новостью поддерживается в двух местах: в собственном кэше контроллера
 * и в коллекции бизнес-объекта новости (композиция {@code News *-- Comment}).
 * Начиная с ЛР6 хранение данных вынесено в {@link CommentRepository}.</p>
 */
public class CommentController {

    private final InMemoryStore<CommentBO> cache = new InMemoryStore<CommentBO>();
    private final CommentRepository repository;

    private final NewsInterface newsController;

    public CommentController(CommentRepository repository, NewsInterface newsController) {
        this.repository = repository;
        this.newsController = newsController;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (CommentBO comment : repository.findAll()) {
            cache.put(comment.getId(), comment);
        }
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

    /** Создать комментарий, сохранить в репозитории и связать с новостью. */
    public CommentBO create(NewsBO news, String text, UserBO author) {
        CommentBO comment = new CommentBO(null, news.getId(), text, new Date(), author);
        repository.save(comment);
        cache.put(comment.getId(), comment);
        news.addComment(comment);
        return comment;
    }

    /** Изменить текст комментария. */
    public CommentBO update(long id, String text) {
        CommentBO comment = findById(id);
        comment.edit(text);
        repository.save(comment);
        return comment;
    }

    /** Удалить комментарий и убрать его из коллекции новости. */
    public boolean delete(long id) {
        CommentBO comment = cache.find(id);
        if (comment == null) {
            return false;
        }
        cache.remove(id);
        repository.delete(id);
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
            comment.approve();
            repository.save(comment);
            return true;
        }
        if ("reject".equalsIgnoreCase(action)) {
            comment.reject();
            repository.save(comment);
            return true;
        }
        return false;
    }
}
