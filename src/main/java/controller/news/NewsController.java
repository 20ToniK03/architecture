package controller.news;

import controller.category.CategoryBO;
import controller.person.JournalistBO;
import controller.tag.TagBO;
import model.IdSequence;
import model.InMemoryStore;
import view.api.dto.CategoryDTO;
import view.api.dto.CreateNewsRequestDTO;
import view.api.dto.NewsItemDTO;
import view.api.dto.NewsTitleDTO;
import view.api.dto.UpdateNewsRequestDTO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Контроллер сущности «Новость» (ЛР4).
 *
 * <p>Реализует {@link NewsInterface} и содержит всю логику работы с новостями,
 * категориями и тегами. Вынесен из фасада, чтобы фасад мог работать с ним через
 * интерфейс и при необходимости «оборачивать» его декораторами.</p>
 */
public class NewsController implements NewsInterface {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private final InMemoryStore<NewsBO> newsStore = new InMemoryStore<NewsBO>();
    private final InMemoryStore<CategoryBO> categoryStore = new InMemoryStore<CategoryBO>();
    private final InMemoryStore<TagBO> tagStore = new InMemoryStore<TagBO>();

    private final IdSequence newsIds = new IdSequence();
    private final IdSequence categoryIds = new IdSequence();
    private final IdSequence tagIds = new IdSequence();

    private final JournalistBO journalist;

    public NewsController(JournalistBO journalist) {
        this.journalist = journalist;
        seedCategories();
        seedTags();
        seedNews();
    }

    // --- Чтение ---

    @Override
    public List<NewsTitleDTO> getAll() {
        List<NewsBO> visible = new ArrayList<NewsBO>();
        for (NewsBO news : newsStore.findAll()) {
            if (isVisible(news)) {
                visible.add(news);
            }
        }
        return toTitleList(visible);
    }

    @Override
    public NewsItemDTO getNewsById(long id) {
        NewsBO news = newsStore.find(id);
        if (news == null || !isVisible(news)) {
            return null;
        }
        return toNewsItemDTO(news);
    }

    @Override
    public List<NewsTitleDTO> getLastWeekNews() {
        long from = System.currentTimeMillis() - 7 * DAY_MILLIS;
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsBO news : newsStore.findAll()) {
            if (isVisible(news) && news.getCreatedAt() != null
                    && news.getCreatedAt().getTime() >= from) {
                result.add(news);
            }
        }
        return toTitleList(result);
    }

    @Override
    public List<NewsTitleDTO> searchNews(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<NewsTitleDTO>();
        }
        Set<NewsBO> found = new LinkedHashSet<NewsBO>();
        for (NewsBO news : newsStore.findAll()) {
            if (!isVisible(news)) {
                continue;
            }
            for (String keyword : keywords) {
                if (news.matchesKeyword(keyword) || categoryMatches(news, keyword)) {
                    found.add(news);
                    break;
                }
            }
        }
        return toTitleList(new ArrayList<NewsBO>(found));
    }

    @Override
    public List<NewsTitleDTO> getNewsByRegion(String region) {
        if (region == null) {
            return new ArrayList<NewsTitleDTO>();
        }
        String needle = region.trim();
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsBO news : newsStore.findAll()) {
            if (isVisible(news) && news.getRegion() != null
                    && news.getRegion().equalsIgnoreCase(needle)) {
                result.add(news);
            }
        }
        return toTitleList(result);
    }

    @Override
    public List<CategoryDTO> getCategories() {
        List<CategoryDTO> result = new ArrayList<CategoryDTO>();
        for (CategoryBO category : categoryStore.findAll()) {
            result.add(toCategoryDTO(category));
        }
        return result;
    }

    // --- Изменение ---

    @Override
    public NewsItemDTO createNews(CreateNewsRequestDTO request) {
        if (request == null || isBlank(request.getTitle())) {
            return null;
        }
        CategoryBO category = request.getCategoryId() == null
                ? null : categoryStore.find(request.getCategoryId());
        if (category == null) {
            return null;
        }

        NewsData data = new NewsData(
                request.getTitle().trim(),
                trimToNull(request.getText()),
                trimToNull(request.getAnnotation()),
                isBlank(request.getRegion()) ? "Москва" : request.getRegion().trim(),
                Arrays.asList(category.getId()),
                request.getTags() == null ? new ArrayList<String>() : request.getTags()
        );
        NewsBO news = journalist.createNews(data);

        news.setId(newsIds.next());
        news.addCategory(category);
        linkTags(news, data.getTagNames());
        news.setRating(0);
        news.publish();
        newsStore.put(news.getId(), news);
        return toNewsItemDTO(news);
    }

    @Override
    public NewsItemDTO updateNews(long id, UpdateNewsRequestDTO request) {
        NewsBO news = newsStore.find(id);
        if (news == null || request == null) {
            return null;
        }
        NewsData data = new NewsData(
                request.getTitle() == null ? news.getTitle() : request.getTitle(),
                request.getText() == null ? news.getText() : request.getText(),
                request.getAnnotation() == null ? news.getAnnotation() : request.getAnnotation(),
                request.getRegion() == null ? news.getRegion() : request.getRegion(),
                news.getCategories().isEmpty() ? new ArrayList<Long>()
                        : Arrays.asList(news.getCategories().get(0).getId()),
                new ArrayList<String>()
        );
        if (request.getCategoryId() != null) {
            CategoryBO category = categoryStore.find(request.getCategoryId());
            if (category != null) {
                news.addCategory(category);
            }
        }
        if (request.getTags() != null) {
            linkTags(news, request.getTags());
        }
        journalist.editNews(news, data);
        return toNewsItemDTO(news);
    }

    @Override
    public boolean deleteNews(long id) {
        NewsBO news = newsStore.find(id);
        if (news == null) {
            return false;
        }
        journalist.deleteNews(news);
        return true;
    }

    @Override
    public NewsItemDTO publishNews(long id) {
        NewsBO news = newsStore.find(id);
        if (news == null) {
            return null;
        }
        news.publish();
        return toNewsItemDTO(news);
    }

    @Override
    public NewsItemDTO unpublishNews(long id) {
        NewsBO news = newsStore.find(id);
        if (news == null) {
            return null;
        }
        news.unpublish();
        return toNewsItemDTO(news);
    }

    @Override
    public NewsItemDTO archiveNews(long id) {
        NewsBO news = newsStore.find(id);
        if (news == null) {
            return null;
        }
        news.archive();
        return toNewsItemDTO(news);
    }

    @Override
    public boolean addTag(long newsId, String tagName) {
        NewsBO news = newsStore.find(newsId);
        if (news == null || isBlank(tagName)) {
            return false;
        }
        news.addTag(findOrCreateTag(tagName.trim()));
        return true;
    }

    @Override
    public boolean attachMedia(long newsId, String url) {
        NewsBO news = newsStore.find(newsId);
        if (news == null || isBlank(url)) {
            return false;
        }
        news.addMedia(url.trim());
        return true;
    }

    // --- Преобразование BO -> DTO ---

    private List<NewsTitleDTO> toTitleList(List<NewsBO> newsList) {
        List<NewsBO> sorted = new ArrayList<NewsBO>(newsList);
        sorted.sort((first, second) -> {
            long a = first.getCreatedAt() == null ? 0L : first.getCreatedAt().getTime();
            long b = second.getCreatedAt() == null ? 0L : second.getCreatedAt().getTime();
            return Long.compare(b, a);
        });

        List<NewsTitleDTO> result = new ArrayList<NewsTitleDTO>();
        for (NewsBO news : sorted) {
            result.add(toNewsTitleDTO(news));
        }
        return result;
    }

    private NewsTitleDTO toNewsTitleDTO(NewsBO news) {
        return new NewsTitleDTO(news.getId(), news.getTitle(), firstCategoryName(news), format(news.getCreatedAt()));
    }

    private NewsItemDTO toNewsItemDTO(NewsBO news) {
        NewsItemDTO dto = new NewsItemDTO(
                news.getId(),
                news.getTitle(),
                news.getAnnotation(),
                news.getText(),
                firstCategoryName(news),
                news.getAuthor() == null ? null : news.getAuthor().getName(),
                format(news.getCreatedAt()),
                news.getRegion(),
                tagNames(news),
                new ArrayList<String>(news.getMedia()),
                news.getRating()
        );
        dto.setStatus(news.getStatus() == null ? null : news.getStatus().name());
        return dto;
    }

    private CategoryDTO toCategoryDTO(CategoryBO category) {
        return new CategoryDTO(category.getId(), category.getName(), category.getDescription());
    }

    // --- Вспомогательные методы ---

    private boolean isVisible(NewsBO news) {
        return news.getStatus() == NewsStatus.PUBLISHED || news.getStatus() == NewsStatus.EDITED;
    }

    private boolean categoryMatches(NewsBO news, String keyword) {
        for (CategoryBO category : news.getCategories()) {
            if (category.matchesKeyword(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String firstCategoryName(NewsBO news) {
        return news.getCategories().isEmpty() ? null : news.getCategories().get(0).getName();
    }

    private List<String> tagNames(NewsBO news) {
        List<String> names = new ArrayList<String>();
        for (TagBO tag : news.getTags()) {
            names.add(tag.getName());
        }
        return names;
    }

    private void linkTags(NewsBO news, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        for (String name : tagNames) {
            if (!isBlank(name)) {
                news.addTag(findOrCreateTag(name.trim()));
            }
        }
    }

    private TagBO findOrCreateTag(String name) {
        for (TagBO tag : tagStore.findAll()) {
            if (tag.getName() != null && tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }
        TagBO tag = new TagBO(tagIds.next(), name);
        tagStore.put(tag.getId(), tag);
        return tag;
    }

    private String format(Date date) {
        return date == null ? null : DATE_TIME_FORMAT.format(date);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - (long) days * DAY_MILLIS);
    }

    // --- Начальные (тестовые) данные ---

    private void seedCategories() {
        addCategory("Технологии", "IT, гаджеты и инновации");
        addCategory("Общество", "Городские события и жизнь горожан");
        addCategory("Спорт", "Соревнования и спортивные результаты");
        addCategory("Наука", "Открытия, исследования и образование");
        addCategory("Экономика", "Финансы, бизнес и экономическая политика");
        addCategory("Культура", "Выставки, театр, музыка и кино");
    }

    private void addCategory(String name, String description) {
        CategoryBO category = new CategoryBO(categoryIds.next(), name, description);
        categoryStore.put(category.getId(), category);
    }

    private void seedTags() {
        for (String name : new String[]{"технологии", "портал", "метро", "транспорт",
                "спорт", "плавание", "наука", "фестиваль", "экономика", "форум",
                "культура", "архитектура", "кризис"}) {
            TagBO tag = new TagBO(tagIds.next(), name);
            tagStore.put(tag.getId(), tag);
        }
    }

    private void seedNews() {
        addNews("Запущена новая версия городского портала",
                "Портал стал быстрее и получил удобный поиск по новостям.",
                "Городской портал обновился: ускорена загрузка страниц, появился умный поиск "
                        + "и раздел с персональными подборками новостей.",
                "Москва", 1L, new String[]{"технологии", "портал"}, 42, daysAgo(0), true);

        addNews("Открытие новой станции метро",
                "Новая станция свяжет центр города с жилым районом.",
                "Сегодня открылась новая станция метро. Пассажирам доступны современные "
                        + "вестибюли, лифты и удобные пересадки на наземный транспорт.",
                "Москва", 2L, new String[]{"метро", "транспорт"}, 35, daysAgo(1), true);

        addNews("Чемпионат города по плаванию",
                "Определены победители в пяти дистанциях.",
                "На городском чемпионате по плаванию спортсмены установили три новых рекорда. "
                        + "Соревнования собрали более двухсот участников.",
                "Санкт-Петербург", 3L, new String[]{"спорт", "плавание"}, 18, daysAgo(3), true);

        addNews("Городской фестиваль науки",
                "Школьники и студенты представили свои исследования.",
                "На фестивале науки работали интерактивные площадки по робототехнике, "
                        + "химии и программированию. Лучшие проекты получили гранты.",
                "Казань", 4L, new String[]{"наука", "фестиваль"}, 27, daysAgo(5), true);

        addNews("Итоги экономического форума",
                "Обсудили поддержку малого бизнеса и инвестиции.",
                "На форуме представили программу льготного кредитования малого бизнеса "
                        + "и новые меры поддержки экспорта.",
                "Москва", 5L, new String[]{"экономика", "форум"}, 12, daysAgo(10), true);

        addNews("Ретроспектива городской архитектуры",
                "Выставка рассказывает, как менялся облик города.",
                "В музее открылась выставка архивных фотографий, макетов и чертежей, "
                        + "посвящённая истории городской архитектуры XX века.",
                "Москва", 6L, new String[]{"культура", "архитектура"}, 9, daysAgo(30), true);

        // Негативная новость - отсеивается декоратором позитивных новостей.
        addNews("Кризис на рынке недвижимости",
                "Цены на жильё продолжают падение.",
                "Аналитики отмечают затяжной кризис на рынке недвижимости.",
                "Москва", 5L, new String[]{"экономика", "кризис"}, 5, daysAgo(2), true);

        // Черновик - демонстрирует состояние Draft и метод publish.
        addNews("Черновик: грядущее обновление портала",
                "Материал в работе.",
                "Этот материал ещё не опубликован и не отображается читателям.",
                "Москва", 1L, new String[]{"технологии"}, 0, daysAgo(0), false);
    }

    private void addNews(String title, String annotation, String text, String region,
                         Long categoryId, String[] tags, int rating, Date createdAt, boolean published) {
        NewsBO news = journalist.createNews(new NewsData(title, text, annotation, region,
                Arrays.asList(categoryId), Arrays.asList(tags)));
        news.setId(newsIds.next());
        news.setCreatedAt(createdAt);
        news.setUpdatedAt(createdAt);
        CategoryBO category = categoryStore.find(categoryId);
        if (category != null) {
            news.addCategory(category);
        }
        linkTags(news, Arrays.asList(tags));
        news.setRating(rating);
        if (published) {
            news.publish();
        } else {
            news.setStatus(NewsStatus.DRAFT);
        }
        newsStore.put(news.getId(), news);
    }
}
