package controller;

import controller.category.CategoryController;
import controller.comment.CommentController;
import controller.news.NewsBO;
import controller.news.NewsData;
import controller.news.NewsInterface;
import controller.person.AdminBO;
import controller.person.JournalistBO;
import controller.person.UserBO;
import controller.person.UserController;
import controller.role.RoleController;
import controller.tag.TagController;

import java.util.Arrays;
import java.util.Date;

/**
 * Начальные (тестовые) данные приложения.
 *
 * <p>Класс относится к слою контроллеров и только наполняет кэши через их
 * публичные методы: сам он не хранит бизнес-объекты и не знает, как они
 * устроены внутри. Благодаря этому тестовые данные появляются ровно так же,
 * как и данные, созданные через API.</p>
 */
public final class SeedData {

    /** Email журналиста по умолчанию (автор создаваемых новостей). */
    public static final String JOURNALIST_EMAIL = "maria@example.com";

    /** Email тестового читателя. */
    public static final String READER_EMAIL = "ivan@example.com";

    /** Email администратора. */
    public static final String ADMIN_EMAIL = "admin@example.com";

    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private SeedData() {
    }

    /** Создать базовые роли с правами. */
    public static void seedRoles(RoleController roles) {
        roles.create("GUEST", Arrays.asList("news.view"));
        roles.create("USER", Arrays.asList("news.view", "news.comment", "news.favorite"));
        roles.create("JOURNALIST", Arrays.asList("news.view", "news.create", "news.edit", "news.delete"));
        roles.create("ADMIN", Arrays.asList("user.block", "user.role", "comment.moderate"));
    }

    /**
     * Создать тестовых пользователей.
     *
     * @return журналист, который будет автором новостей по умолчанию
     */
    public static JournalistBO seedUsers(UserController users, RoleController roles) {
        JournalistBO journalist = new JournalistBO(null, "Мария Соколова", JOURNALIST_EMAIL);
        journalist.setPasswordHash("pass");
        journalist.setRating(4.8);
        journalist.addRole(roles.findByName("JOURNALIST"));
        users.save(journalist);

        UserBO reader = new UserBO(null, "Иван Петров", READER_EMAIL);
        reader.setPasswordHash("pass");
        reader.addRole(roles.findByName("USER"));
        users.save(reader);

        AdminBO admin = new AdminBO(null, "Алексей Админов", ADMIN_EMAIL);
        admin.setPasswordHash("admin");
        admin.addRole(roles.findByName("ADMIN"));
        users.save(admin);

        return journalist;
    }

    /** Создать категории, теги, новости и один комментарий. */
    public static void seedContent(CategoryController categories, TagController tags,
                                   NewsInterface news, UserController users,
                                   CommentController comments) {
        seedCategories(categories);
        seedTags(tags);
        seedNews(categories, news);
        seedComments(news, users, comments);
    }

    private static void seedCategories(CategoryController categories) {
        categories.create("Технологии", "IT, гаджеты и инновации");
        categories.create("Общество", "Городские события и жизнь горожан");
        categories.create("Спорт", "Соревнования и спортивные результаты");
        categories.create("Наука", "Открытия, исследования и образование");
        categories.create("Экономика", "Финансы, бизнес и экономическая политика");
        categories.create("Культура", "Выставки, театр, музыка и кино");
    }

    private static void seedTags(TagController tags) {
        for (String name : new String[]{"технологии", "портал", "метро", "транспорт",
                "спорт", "плавание", "наука", "фестиваль", "экономика", "форум",
                "культура", "архитектура", "кризис"}) {
            tags.findOrCreate(name);
        }
    }

    private static void seedNews(CategoryController categories, NewsInterface news) {
        addNews(news, categories.findByName("Технологии").getId(),
                "Запущена новая версия городского портала",
                "Портал стал быстрее и получил удобный поиск по новостям.",
                "Городской портал обновился: ускорена загрузка страниц, появился умный поиск "
                        + "и раздел с персональными подборками новостей.",
                "Москва", new String[]{"технологии", "портал"}, 42, daysAgo(0), true);

        addNews(news, categories.findByName("Общество").getId(),
                "Открытие новой станции метро",
                "Новая станция свяжет центр города с жилым районом.",
                "Сегодня открылась новая станция метро. Пассажирам доступны современные "
                        + "вестибюли, лифты и удобные пересадки на наземный транспорт.",
                "Москва", new String[]{"метро", "транспорт"}, 35, daysAgo(1), true);

        addNews(news, categories.findByName("Спорт").getId(),
                "Чемпионат города по плаванию",
                "Определены победители в пяти дистанциях.",
                "На городском чемпионате по плаванию спортсмены установили три новых рекорда. "
                        + "Соревнования собрали более двухсот участников.",
                "Санкт-Петербург", new String[]{"спорт", "плавание"}, 18, daysAgo(3), true);

        addNews(news, categories.findByName("Наука").getId(),
                "Городской фестиваль науки",
                "Школьники и студенты представили свои исследования.",
                "На фестивале науки работали интерактивные площадки по робототехнике, "
                        + "химии и программированию. Лучшие проекты получили гранты.",
                "Казань", new String[]{"наука", "фестиваль"}, 27, daysAgo(5), true);

        addNews(news, categories.findByName("Экономика").getId(),
                "Итоги экономического форума",
                "Обсудили поддержку малого бизнеса и инвестиции.",
                "На форуме представили программу льготного кредитования малого бизнеса "
                        + "и новые меры поддержки экспорта.",
                "Москва", new String[]{"экономика", "форум"}, 12, daysAgo(10), true);

        addNews(news, categories.findByName("Культура").getId(),
                "Ретроспектива городской архитектуры",
                "Выставка рассказывает, как менялся облик города.",
                "В музее открылась выставка архивных фотографий, макетов и чертежей, "
                        + "посвящённая истории городской архитектуры XX века.",
                "Москва", new String[]{"культура", "архитектура"}, 9, daysAgo(30), true);

        addNews(news, categories.findByName("Экономика").getId(),
                "Кризис на рынке недвижимости",
                "Цены на жильё продолжают падение.",
                "Аналитики отмечают затяжной кризис на рынке недвижимости.",
                "Москва", new String[]{"экономика", "кризис"}, 5, daysAgo(2), true);

        addNews(news, categories.findByName("Технологии").getId(),
                "Черновик: грядущее обновление портала",
                "Материал в работе.",
                "Этот материал ещё не опубликован и не отображается читателям.",
                "Москва", new String[]{"технологии"}, 0, daysAgo(0), false);
    }

    private static void addNews(NewsInterface news, Long categoryId,
                                String title, String annotation, String text, String region,
                                String[] tagNames, int rating, Date createdAt, boolean published) {
        NewsData data = new NewsData(title, text, annotation, region,
                Arrays.asList(categoryId), Arrays.asList(tagNames));
        NewsBO created = news.create(data);
        created.setCreatedAt(createdAt);
        created.setRating(rating);
        if (published) {
            created.publish();
        }
        created.setUpdatedAt(createdAt);
    }

    private static void seedComments(NewsInterface news, UserController users,
                                     CommentController comments) {
        NewsBO newsItem = news.findById(1L);
        UserBO author = users.findByEmail(READER_EMAIL);
        if (newsItem != null && author != null) {
            comments.create(newsItem,
                    "Отличная новость, давно ждал обновления портала!", author);
        }
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - (long) days * DAY_MILLIS);
    }
}
