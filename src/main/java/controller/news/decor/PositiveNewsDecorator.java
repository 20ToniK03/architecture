package controller.news.decor;

import controller.news.NewsInterface;
import view.api.dto.NewsTitleDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Декоратор позитивных новостей (ЛР4).
 *
 * <p>Наследник абстрактного {@link NewsDecorator}. Дополняет методы выдачи списка
 * новостей проверкой на «позитивность»: новости, в заголовке которых встречаются
 * негативные слова, исключаются из результата.</p>
 */
public class PositiveNewsDecorator extends NewsDecorator {

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
            "катастрофа", "кризис", "авария", "пожар", "война", "трагедия", "бедствие");

    public PositiveNewsDecorator(NewsInterface newsController) {
        super(newsController);
    }

    @Override
    public List<NewsTitleDTO> getAll() {
        return filterPositive(super.getAll());
    }

    @Override
    public List<NewsTitleDTO> searchNews(List<String> keywords) {
        return filterPositive(super.searchNews(keywords));
    }

    /** Фильтрация списка по ключевым словам (вынесена в отдельный метод). */
    private List<NewsTitleDTO> filterPositive(List<NewsTitleDTO> news) {
        List<NewsTitleDTO> result = new ArrayList<NewsTitleDTO>();
        for (NewsTitleDTO item : news) {
            if (isPositive(item)) {
                result.add(item);
            }
        }
        return result;
    }

    private boolean isPositive(NewsTitleDTO item) {
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase(Locale.ROOT);
        for (String word : NEGATIVE_WORDS) {
            if (title.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
