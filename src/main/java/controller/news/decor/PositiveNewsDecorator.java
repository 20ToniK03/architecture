package controller.news.decor;

import controller.news.NewsBO;
import controller.news.NewsInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Декоратор позитивных новостей (ЛР4, ЛР5).
 *
 * <p>Дополняет методы выдачи списков проверкой на «позитивность»: новости,
 * в заголовке которых встречаются негативные слова, исключаются из результата.
 * Поскольку в ЛР5 операции со списками живут в контроллере, декоратор
 * оборачивает именно контроллер.</p>
 */
public class PositiveNewsDecorator extends NewsDecorator {

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
            "катастрофа", "кризис", "авария", "пожар", "война", "трагедия", "бедствие");

    public PositiveNewsDecorator(NewsInterface newsController) {
        super(newsController);
    }

    @Override
    public List<NewsBO> findAll() {
        return filterPositive(super.findAll());
    }

    @Override
    public List<NewsBO> findVisible() {
        return filterPositive(super.findVisible());
    }

    @Override
    public List<NewsBO> findByCategory(long categoryId) {
        return filterPositive(super.findByCategory(categoryId));
    }

    @Override
    public List<NewsBO> findLatest(int days) {
        return filterPositive(super.findLatest(days));
    }

    @Override
    public List<NewsBO> findByRegion(String region) {
        return filterPositive(super.findByRegion(region));
    }

    @Override
    public List<NewsBO> search(List<String> keywords) {
        return filterPositive(super.search(keywords));
    }

    /** Фильтрация списка по заголовкам. */
    private List<NewsBO> filterPositive(List<NewsBO> newsList) {
        List<NewsBO> result = new ArrayList<NewsBO>();
        if (newsList == null) {
            return result;
        }
        for (NewsBO news : newsList) {
            if (isPositive(news)) {
                result.add(news);
            }
        }
        return result;
    }

    private boolean isPositive(NewsBO news) {
        String title = news.getTitle() == null ? "" : news.getTitle().toLowerCase(Locale.ROOT);
        for (String word : NEGATIVE_WORDS) {
            if (title.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
