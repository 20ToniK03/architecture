package controller.repository;

import controller.category.CategoryBO;
import controller.news.NewsBO;
import controller.news.NewsStatus;
import controller.person.JournalistBO;
import controller.person.UserBO;
import controller.tag.TagBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.NoSqlDao;
import model.dao.SqlDao;
import model.data.NewsContentDO;
import model.data.NewsDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Новость» (ЛР6, ЛР7).
 *
 * <p>Новость разделена на две модели данных: {@link NewsDO} хранит «ядро +
 * превью» в реляционном хранилище SQLite, а {@link NewsContentDO} - полный
 * текст в документном хранилище MongoDB. Начиная с ЛР7 вместо заглушек-карт
 * используются DAO-классы: {@link SqlDao} и {@link NoSqlDao}. Если сервер
 * MongoDB недоступен, контент временно хранится в памяти.</p>
 */
public class NewsRepository implements IRepository<NewsBO> {

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    /** DAO реляционного хранилища ядра и превью новости. */
    private final SqlDao<NewsDO, Long> sqlDao =
            new SqlDao<NewsDO, Long>(Database.sqlite(), NewsDO.class, "news", "id");

    /** DAO документного хранилища контента новости. */
    private final NoSqlDao<NewsContentDO, Long> nosqlDao =
            new NoSqlDao<NewsContentDO, Long>(Database.mongo(), NewsContentDO.class, "news_contents", "newsId");

    private final IdSequence ids = new IdSequence();
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    public NewsRepository(CategoryRepository categoryRepository,
                          TagRepository tagRepository,
                          UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        if (sqlDao.findAll().isEmpty()) {
            seed();
        }
        ids.ensureAtLeast(nextId());
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        seedNews(1L, "Технологии", new String[]{"технологии", "портал"},
                "Запущена новая версия городского портала",
                "Портал стал быстрее и получил удобный поиск по новостям.",
                "Городской портал обновился: ускорена загрузка страниц, появился умный поиск "
                        + "и раздел с персональными подборками новостей.",
                "Москва", 42, 0, true);

        seedNews(2L, "Общество", new String[]{"метро", "транспорт"},
                "Открытие новой станции метро",
                "Новая станция свяжет центр города с жилым районом.",
                "Сегодня открылась новая станция метро. Пассажирам доступны современные "
                        + "вестибюли, лифты и удобные пересадки на наземный транспорт.",
                "Москва", 35, 1, true);

        seedNews(3L, "Спорт", new String[]{"спорт", "плавание"},
                "Чемпионат города по плаванию",
                "Определены победители в пяти дистанциях.",
                "На городском чемпионате по плаванию спортсмены установили три новых рекорда. "
                        + "Соревнования собрали более двухсот участников.",
                "Санкт-Петербург", 18, 3, true);

        seedNews(4L, "Наука", new String[]{"наука", "фестиваль"},
                "Городской фестиваль науки",
                "Школьники и студенты представили свои исследования.",
                "На фестивале науки работали интерактивные площадки по робототехнике, "
                        + "химии и программированию. Лучшие проекты получили гранты.",
                "Казань", 27, 5, true);

        seedNews(5L, "Экономика", new String[]{"экономика", "форум"},
                "Итоги экономического форума",
                "Обсудили поддержку малого бизнеса и инвестиции.",
                "На форуме представили программу льготного кредитования малого бизнеса "
                        + "и новые меры поддержки экспорта.",
                "Москва", 12, 10, true);

        seedNews(6L, "Культура", new String[]{"культура", "архитектура"},
                "Ретроспектива городской архитектуры",
                "Выставка рассказывает, как менялся облик города.",
                "В музее открылась выставка архивных фотографий, макетов и чертежей, "
                        + "посвящённая истории городской архитектуры XX века.",
                "Москва", 9, 30, true);

        seedNews(7L, "Экономика", new String[]{"экономика", "кризис"},
                "Кризис на рынке недвижимости",
                "Цены на жильё продолжают падение.",
                "Аналитики отмечают затяжной кризис на рынке недвижимости.",
                "Москва", 5, 2, true);

        seedNews(8L, "Технологии", new String[]{"технологии"},
                "Черновик: грядущее обновление портала",
                "Материал в работе.",
                "Этот материал ещё не опубликован и не отображается читателям.",
                "Москва", 0, 0, false);
    }

    private void seedNews(long id, String categoryName, String[] tagNames,
                          String title, String annotation, String text, String region,
                          int rating, int daysAgo, boolean published) {
        NewsBO news = new NewsBO(id, title, text);
        news.setAuthor(findAuthor(1L));
        news.setAnnotation(annotation);
        news.setRegion(region);
        CategoryBO category = findCategory(categoryName);
        if (category != null) {
            news.addCategory(category);
        }
        for (String tagName : tagNames) {
            TagBO tag = findTag(tagName);
            if (tag != null) {
                news.addTag(tag);
            }
        }
        Date createdAt = daysAgo(daysAgo);
        news.setCreatedAt(createdAt);
        news.setRating(rating);
        if (published) {
            news.publish();
        }
        news.setUpdatedAt(createdAt);
        save(news);
    }

    @Override
    public List<NewsBO> findAll() {
        List<NewsBO> result = new ArrayList<NewsBO>();
        for (NewsDO entity : sqlDao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public NewsBO findById(Long id) {
        NewsDO entity = sqlDao.findById(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public NewsBO save(NewsBO item) {
        if (item == null) {
            return null;
        }
        if (item.getId() == null) {
            item.setId(ids.next());
        }
        sqlDao.save(toCoreDO(item));
        nosqlDao.save(toContentDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || sqlDao.findById(id) == null) {
            return false;
        }
        sqlDao.deleteById(id);
        nosqlDao.deleteById(id);
        return true;
    }

    private long nextId() {
        long max = 0L;
        for (NewsDO entity : sqlDao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private NewsBO toBO(NewsDO entity) {
        NewsBO news = new NewsBO(entity.id, entity.title, contentOf(entity.id),
                parseStatus(entity.status));
        news.setAnnotation(entity.annotation);
        news.setRegion(entity.region);
        if (entity.createdAt > 0L) {
            news.setCreatedAt(new Date(entity.createdAt));
        }
        if (entity.updatedAt > 0L) {
            news.setUpdatedAt(new Date(entity.updatedAt));
        }
        news.setRating(entity.rating);
        for (String url : entity.media) {
            news.addMedia(url);
        }
        for (Long categoryId : entity.categoryIds) {
            CategoryBO category = categoryRepository.findById(categoryId);
            if (category != null) {
                news.addCategory(category);
            }
        }
        for (Long tagId : entity.tagIds) {
            TagBO tag = tagRepository.findById(tagId);
            if (tag != null) {
                news.addTag(tag);
            }
        }
        news.setAuthor(findAuthor(entity.authorId));
        return news;
    }

    private String contentOf(Long newsId) {
        NewsContentDO content = nosqlDao.findById(newsId);
        return content == null ? null : content.text;
    }

    private NewsDO toCoreDO(NewsBO item) {
        NewsDO entity = new NewsDO();
        entity.id = item.getId();
        entity.title = item.getTitle();
        entity.annotation = item.getAnnotation();
        entity.region = item.getRegion();
        entity.status = item.getStatus() == null ? NewsStatus.DRAFT.name() : item.getStatus().name();
        entity.authorId = item.getAuthor() == null ? null : item.getAuthor().getId();
        entity.rating = item.getRating();
        entity.createdAt = item.getCreatedAt() == null ? 0L : item.getCreatedAt().getTime();
        entity.updatedAt = item.getUpdatedAt() == null ? 0L : item.getUpdatedAt().getTime();
        for (String url : item.getMedia()) {
            entity.media.add(url);
        }
        for (CategoryBO category : item.getCategories()) {
            if (category != null && category.getId() != null) {
                entity.categoryIds.add(category.getId());
            }
        }
        for (TagBO tag : item.getTags()) {
            if (tag != null && tag.getId() != null) {
                entity.tagIds.add(tag.getId());
            }
        }
        return entity;
    }

    private NewsContentDO toContentDO(NewsBO item) {
        NewsContentDO entity = new NewsContentDO();
        entity.newsId = item.getId();
        entity.text = item.getText();
        return entity;
    }

    private CategoryBO findCategory(String name) {
        if (name == null) {
            return null;
        }
        for (CategoryBO category : categoryRepository.findAll()) {
            if (name.equalsIgnoreCase(category.getName())) {
                return category;
            }
        }
        return null;
    }

    private TagBO findTag(String name) {
        if (name == null) {
            return null;
        }
        for (TagBO tag : tagRepository.findAll()) {
            if (name.equalsIgnoreCase(tag.getName())) {
                return tag;
            }
        }
        return null;
    }

    private JournalistBO findAuthor(Long id) {
        if (id == null) {
            return null;
        }
        UserBO user = userRepository.findById(id);
        if (user == null) {
            return null;
        }
        if (user instanceof JournalistBO) {
            return (JournalistBO) user;
        }
        return new JournalistBO(user.getId(), user.getName(), user.getEmail());
    }

    private static NewsStatus parseStatus(String value) {
        if (value == null) {
            return NewsStatus.DRAFT;
        }
        try {
            return NewsStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return NewsStatus.DRAFT;
        }
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - (long) days * DAY_MILLIS);
    }
}
