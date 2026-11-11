package controller.news;

import view.api.dto.CategoryDTO;
import view.api.dto.CreateNewsRequestDTO;
import view.api.dto.NewsItemDTO;
import view.api.dto.NewsTitleDTO;
import view.api.dto.UpdateNewsRequestDTO;

import java.util.List;

/**
 * Интерфейс контроллера сущности «Новость» (ЛР4, паттерн «Декоратор»).
 *
 * <p>Фиксирует набор методов контроллера новостей, чтобы фасад и декораторы могли
 * работать с любой реализацией (обычной или «обёрнутой» в декоратор) единообразно.</p>
 */
public interface NewsInterface {

    /** Все опубликованные новости. */
    List<NewsTitleDTO> getAll();

    /** Новость по идентификатору. */
    NewsItemDTO getNewsById(long id);

    /** Новости за последнюю неделю. */
    List<NewsTitleDTO> getLastWeekNews();

    /** Поиск по ключевым словам. */
    List<NewsTitleDTO> searchNews(List<String> keywords);

    /** Новости региона. */
    List<NewsTitleDTO> getNewsByRegion(String region);

    /** Все категории. */
    List<CategoryDTO> getCategories();

    /** Создать новость. */
    NewsItemDTO createNews(CreateNewsRequestDTO request);

    /** Изменить новость. */
    NewsItemDTO updateNews(long id, UpdateNewsRequestDTO request);

    /** Удалить новость. */
    boolean deleteNews(long id);

    /** Опубликовать новость. */
    NewsItemDTO publishNews(long id);

    /** Снять с публикации. */
    NewsItemDTO unpublishNews(long id);

    /** Перевести в архив. */
    NewsItemDTO archiveNews(long id);

    /** Добавить тег. */
    boolean addTag(long newsId, String tagName);

    /** Прикрепить медиафайл. */
    boolean attachMedia(long newsId, String url);
}
