package controller.news.decor;

import controller.news.NewsInterface;
import view.api.dto.CategoryDTO;
import view.api.dto.CreateNewsRequestDTO;
import view.api.dto.NewsItemDTO;
import view.api.dto.NewsTitleDTO;
import view.api.dto.UpdateNewsRequestDTO;

import java.util.List;

/**
 * Абстрактный декоратор контроллера новостей (ЛР4).
 *
 * <p>Класс-обёртка: реализует {@link NewsInterface}, хранит внутри декорируемый
 * контроллер и делегирует ему все вызовы. Наследники переопределяют только те
 * методы, поведение которых нужно дополнить.</p>
 */
public abstract class NewsDecorator implements NewsInterface {

    protected final NewsInterface newsController;

    public NewsDecorator(NewsInterface newsController) {
        this.newsController = newsController;
    }

    @Override
    public List<NewsTitleDTO> getAll() {
        return newsController.getAll();
    }

    @Override
    public NewsItemDTO getNewsById(long id) {
        return newsController.getNewsById(id);
    }

    @Override
    public List<NewsTitleDTO> getLastWeekNews() {
        return newsController.getLastWeekNews();
    }

    @Override
    public List<NewsTitleDTO> searchNews(List<String> keywords) {
        return newsController.searchNews(keywords);
    }

    @Override
    public List<NewsTitleDTO> getNewsByRegion(String region) {
        return newsController.getNewsByRegion(region);
    }

    @Override
    public List<CategoryDTO> getCategories() {
        return newsController.getCategories();
    }

    @Override
    public NewsItemDTO createNews(CreateNewsRequestDTO request) {
        return newsController.createNews(request);
    }

    @Override
    public NewsItemDTO updateNews(long id, UpdateNewsRequestDTO request) {
        return newsController.updateNews(id, request);
    }

    @Override
    public boolean deleteNews(long id) {
        return newsController.deleteNews(id);
    }

    @Override
    public NewsItemDTO publishNews(long id) {
        return newsController.publishNews(id);
    }

    @Override
    public NewsItemDTO unpublishNews(long id) {
        return newsController.unpublishNews(id);
    }

    @Override
    public NewsItemDTO archiveNews(long id) {
        return newsController.archiveNews(id);
    }

    @Override
    public boolean addTag(long newsId, String tagName) {
        return newsController.addTag(newsId, tagName);
    }

    @Override
    public boolean attachMedia(long newsId, String url) {
        return newsController.attachMedia(newsId, url);
    }
}
