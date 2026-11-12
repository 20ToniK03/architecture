package controller.news.decor;

import controller.news.NewsBO;
import controller.news.NewsData;
import controller.news.NewsInterface;

import java.util.List;

/**
 * Абстрактный декоратор контроллера новостей (ЛР4, ЛР5).
 *
 * <p>Реализует {@link NewsInterface}, хранит внутри декорируемый контроллер и
 * делегирует ему все вызовы. Наследники переопределяют только те методы,
 * поведение которых нужно дополнить. В ЛР5 декорируется именно контроллер,
 * потому что операции со списками теперь реализованы в нём.</p>
 */
public abstract class NewsDecorator implements NewsInterface {

    protected final NewsInterface newsController;

    public NewsDecorator(NewsInterface newsController) {
        this.newsController = newsController;
    }

    @Override
    public List<NewsBO> findAll() {
        return newsController.findAll();
    }

    @Override
    public List<NewsBO> findVisible() {
        return newsController.findVisible();
    }

    @Override
    public NewsBO findById(long id) {
        return newsController.findById(id);
    }

    @Override
    public NewsBO findVisibleById(long id) {
        return newsController.findVisibleById(id);
    }

    @Override
    public List<NewsBO> findByCategory(long categoryId) {
        return newsController.findByCategory(categoryId);
    }

    @Override
    public List<NewsBO> findLatest(int days) {
        return newsController.findLatest(days);
    }

    @Override
    public List<NewsBO> findByRegion(String region) {
        return newsController.findByRegion(region);
    }

    @Override
    public List<NewsBO> search(List<String> keywords) {
        return newsController.search(keywords);
    }

    @Override
    public NewsBO create(NewsData data) {
        return newsController.create(data);
    }

    @Override
    public NewsBO update(long id, NewsData data) {
        return newsController.update(id, data);
    }

    @Override
    public boolean delete(long id) {
        return newsController.delete(id);
    }

    @Override
    public NewsBO publish(long id) {
        return newsController.publish(id);
    }

    @Override
    public NewsBO unpublish(long id) {
        return newsController.unpublish(id);
    }

    @Override
    public NewsBO archive(long id) {
        return newsController.archive(id);
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
