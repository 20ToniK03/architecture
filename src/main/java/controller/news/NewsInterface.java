package controller.news;

import java.util.List;

/**
 * Интерфейс контроллера сущности «Новость» (ЛР4, паттерн «Декоратор»).
 *
 * <p>Фиксирует набор методов контроллера новостей. Фасад и декораторы работают
 * с интерфейсом, поэтому обычный контроллер можно заменить «обёрнутым»
 * в декоратор, не меняя код фасада.</p>
 *
 * <p>Начиная с ЛР5 контроллер возвращает бизнес-объекты {@link NewsBO},
 * а преобразование в DTO выполняет фасад.</p>
 */
public interface NewsInterface {

    /** Все новости из кэша контроллера. */
    List<NewsBO> findAll();

    /** Только опубликованные новости (видны читателю). */
    List<NewsBO> findVisible();

    /**
     * Новость по идентификатору.
     *
     * @throws java.util.NoSuchElementException если новости нет
     */
    NewsBO findById(long id);

    /** Опубликованная новость по идентификатору или {@code null}. */
    NewsBO findVisibleById(long id);

    /** Новости указанной категории. */
    List<NewsBO> findByCategory(long categoryId);

    /** Новости, созданные за последние {@code days} дней. */
    List<NewsBO> findLatest(int days);

    /** Новости указанного региона. */
    List<NewsBO> findByRegion(String region);

    /** Поиск по ключевым словам (текст новости, теги, категории). */
    List<NewsBO> search(List<String> keywords);

    /** Создать новость-черновик. */
    NewsBO create(NewsData data);

    /** Изменить новость. */
    NewsBO update(long id, NewsData data);

    /** Удалить новость. {@code false}, если её не было. */
    boolean delete(long id);

    /** Опубликовать новость. */
    NewsBO publish(long id);

    /** Снять с публикации. */
    NewsBO unpublish(long id);

    /** Перевести в архив. */
    NewsBO archive(long id);

    /** Добавить тег к новости. */
    boolean addTag(long newsId, String tagName);

    /** Прикрепить медиафайл к новости. */
    boolean attachMedia(long newsId, String url);
}
