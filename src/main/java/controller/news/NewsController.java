package controller.news;

import controller.category.CategoryBO;
import controller.category.CategoryController;
import controller.person.JournalistBO;
import controller.repository.NewsRepository;
import controller.tag.TagController;
import model.InMemoryStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Контроллер сущности «Новость» (ЛР4, ЛР5, ЛР6).
 *
 * <p>Отвечает за существование новостей: кэш, создание, изменение, удаление
 * и смена состояния публикации. Никаких DTO здесь нет, преобразование
 * выполняет фасад. Категории и теги не хранятся в этом контроллере: за них
 * отвечают {@link CategoryController} и {@link TagController}. Начиная с ЛР6
 * хранение данных вынесено в {@link NewsRepository}: кэш загружается из
 * репозитория, а изменения сохраняются в нём.</p>
 */
public class NewsController implements NewsInterface {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final InMemoryStore<NewsBO> cache = new InMemoryStore<NewsBO>();
    private final NewsRepository repository;

    private final CategoryController categoryController;
    private final TagController tagController;
    private final JournalistBO defaultAuthor;

    public NewsController(NewsRepository repository,
                          CategoryController categoryController,
                          TagController tagController,
                          JournalistBO defaultAuthor) {
        this.repository = repository;
        this.categoryController = categoryController;
        this.tagController = tagController;
        this.defaultAuthor = defaultAuthor;
        loadFromStorage();
    }

    private void loadFromStorage() {
        for (NewsBO news : repository.findAll()) {
            cache.put(news.getId(), news);
        }
    }

    // --- Чтение ---

    @Override
    public List<NewsBO> findAll() {
        return cache.findAll();
    }

    @Override
    public List<NewsBO> findVisible() {
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsBO news : cache.findAll()) {
            if (news.isPublished()) {
                result.add(news);
            }
        }
        return result;
    }

    @Override
    public NewsBO findById(long id) {
        NewsBO news = cache.find(id);
        if (news == null) {
            throw new NoSuchElementException("Новость не найдена: " + id);
        }
        return news;
    }

    @Override
    public NewsBO findVisibleById(long id) {
        NewsBO news = cache.find(id);
        if (news == null || !news.isPublished()) {
            return null;
        }
        return news;
    }

    @Override
    public List<NewsBO> findByCategory(long categoryId) {
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsBO news : cache.findAll()) {
            if (!news.isPublished()) {
                continue;
            }
            for (CategoryBO category : news.getCategories()) {
                if (category.getId() != null && category.getId() == categoryId) {
                    result.add(news);
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public List<NewsBO> findLatest(int days) {
        long from = System.currentTimeMillis() - (long) days * DAY_MILLIS;
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsBO news : cache.findAll()) {
            if (news.isPublished() && news.getCreatedAt() != null
                    && news.getCreatedAt().getTime() >= from) {
                result.add(news);
            }
        }
        return result;
    }

    @Override
    public List<NewsBO> findByRegion(String region) {
        List<NewsBO> result = new ArrayList<NewsBO>();
        if (region == null) {
            return result;
        }
        String needle = region.trim();
        for (NewsBO news : cache.findAll()) {
            if (news.isPublished() && news.getRegion() != null
                    && news.getRegion().equalsIgnoreCase(needle)) {
                result.add(news);
            }
        }
        return result;
    }

    @Override
    public List<NewsBO> search(List<String> keywords) {
        List<NewsBO> result = new ArrayList<NewsBO>();
        if (keywords == null || keywords.isEmpty()) {
            return result;
        }
        Set<NewsBO> found = new LinkedHashSet<NewsBO>();
        for (NewsBO news : cache.findAll()) {
            if (!news.isPublished()) {
                continue;
            }
            for (String keyword : keywords) {
                if (news.matchesKeyword(keyword) || categoryMatches(news, keyword)) {
                    found.add(news);
                    break;
                }
            }
        }
        result.addAll(found);
        return result;
    }

    // --- Изменение ---

    @Override
    public NewsBO create(NewsData data) {
        if (data == null) {
            throw new IllegalArgumentException("Нет данных для создания новости");
        }
        NewsBO news = new NewsBO(null, data.getTitle(), data.getText());
        news.setAuthor(defaultAuthor);
        news.setAnnotation(data.getAnnotation());
        news.setRegion(data.getRegion());

        if (data.getCategoryIds() != null) {
            for (Long categoryId : data.getCategoryIds()) {
                if (categoryId != null) {
                    news.addCategory(categoryController.findById(categoryId));
                }
            }
        }
        linkTags(news, data.getTagNames());
        news.setRating(0);
        repository.save(news);
        cache.put(news.getId(), news);
        return news;
    }

    @Override
    public NewsBO update(long id, NewsData data) {
        NewsBO news = findById(id);
        if (data == null) {
            return news;
        }
        if (data.getTitle() != null && !data.getTitle().equals(news.getTitle())) {
            news.changeTitle(data.getTitle());
        }
        if (data.getText() != null) {
            news.setText(data.getText());
        }
        if (data.getAnnotation() != null) {
            news.setAnnotation(data.getAnnotation());
        }
        if (data.getRegion() != null) {
            news.setRegion(data.getRegion());
        }
        if (data.getCategoryIds() != null) {
            for (Long categoryId : data.getCategoryIds()) {
                if (categoryId != null) {
                    news.addCategory(categoryController.findById(categoryId));
                }
            }
        }
        linkTags(news, data.getTagNames());
        news.markEdited();
        repository.save(news);
        return news;
    }

    @Override
    public boolean delete(long id) {
        if (cache.find(id) == null) {
            return false;
        }
        boolean removed = cache.remove(id);
        if (removed) {
            repository.delete(id);
        }
        return removed;
    }

    @Override
    public NewsBO publish(long id) {
        NewsBO news = findById(id);
        news.publish();
        repository.save(news);
        return news;
    }

    @Override
    public NewsBO unpublish(long id) {
        NewsBO news = findById(id);
        news.unpublish();
        repository.save(news);
        return news;
    }

    @Override
    public NewsBO archive(long id) {
        NewsBO news = findById(id);
        news.archive();
        repository.save(news);
        return news;
    }

    @Override
    public boolean addTag(long newsId, String tagName) {
        if (isBlank(tagName)) {
            return false;
        }
        NewsBO news = findById(newsId);
        news.addTag(tagController.findOrCreate(tagName.trim()));
        repository.save(news);
        return true;
    }

    @Override
    public boolean attachMedia(long newsId, String url) {
        if (isBlank(url)) {
            return false;
        }
        NewsBO news = findById(newsId);
        news.addMedia(url.trim());
        repository.save(news);
        return true;
    }

    // --- Вспомогательные методы ---

    private void linkTags(NewsBO news, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        for (String name : tagNames) {
            if (!isBlank(name)) {
                news.addTag(tagController.findOrCreate(name.trim()));
            }
        }
    }

    private boolean categoryMatches(NewsBO news, String keyword) {
        for (CategoryBO category : news.getCategories()) {
            if (category.matchesKeyword(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Служебный метод для начальных данных: положить готовый объект в кэш и репозиторий. */
    public NewsBO save(NewsBO news) {
        repository.save(news);
        cache.put(news.getId(), news);
        return news;
    }
}
