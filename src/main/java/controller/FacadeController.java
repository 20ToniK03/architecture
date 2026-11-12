package controller;

import controller.category.CategoryBO;
import controller.category.CategoryController;
import controller.comment.CommentBO;
import controller.comment.CommentController;
import controller.favorite.FavoriteBO;
import controller.favorite.FavoriteController;
import controller.news.NewsBO;
import controller.news.NewsController;
import controller.news.NewsData;
import controller.news.NewsInterface;
import controller.news.decor.PositiveNewsDecorator;
import controller.person.JournalistBO;
import controller.person.RegistrationData;
import controller.person.UserBO;
import controller.person.UserController;
import controller.role.RoleBO;
import controller.role.RoleController;
import controller.tag.TagBO;
import controller.tag.TagController;
import view.api.dto.CategoryDTO;
import view.api.dto.CommentDTO;
import view.api.dto.CreateCommentRequestDTO;
import view.api.dto.CreateNewsRequestDTO;
import view.api.dto.NewsItemDTO;
import view.api.dto.NewsTitleDTO;
import view.api.dto.RoleDTO;
import view.api.dto.UpdateCommentRequestDTO;
import view.api.dto.UpdateNewsRequestDTO;
import view.api.dto.UserDTO;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Фасад (паттерн Facade) между слоем представления и предметной областью.
 *
 * <p>Скрывает контроллеры бизнес-объектов за единым интерфейсом, принимает DTO
 * запросов и возвращает DTO ответов. Начиная с ЛР5 каждый бизнес-объект имеет
 * собственный контроллер, который отвечает за существование его экземпляров.
 * Фасад связывает контроллеры между собой и выполняет преобразование
 * бизнес-объектов в DTO.</p>
 */
public final class FacadeController {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    /** Порог в днях: считается, что до конца года осталось «меньше двух месяцев». */
    private static final int DAYS_UNTIL_END_OF_YEAR = 60;

    private static final FacadeController INSTANCE = new FacadeController();

    private final RoleController roleController = new RoleController();
    private final UserController userController = new UserController();
    private final CategoryController categoryController = new CategoryController();
    private final TagController tagController = new TagController();
    private final FavoriteController favoriteController = new FavoriteController();

    private final NewsInterface newsController;
    private final CommentController commentController;

    private FacadeController() {
        SeedData.seedRoles(roleController);
        JournalistBO journalist = SeedData.seedUsers(userController, roleController);
        this.newsController = createNewsController(journalist);
        this.commentController = new CommentController(newsController);
        SeedData.seedContent(categoryController, tagController, newsController,
                userController, commentController);
    }

    public static FacadeController getInstance() {
        return INSTANCE;
    }

    // --- Новости (делегирование NewsController, ЛР4) ---

    public synchronized List<NewsTitleDTO> getAllNews() {
        return toTitleList(newsController.findVisible());
    }

    public synchronized NewsItemDTO getNewsById(long id) {
        NewsBO news = newsController.findVisibleById(id);
        return news == null ? null : toNewsItemDTO(news);
    }

    public synchronized List<NewsTitleDTO> getLastWeekNews() {
        return toTitleList(newsController.findLatest(7));
    }

    public synchronized List<NewsTitleDTO> searchNews(List<String> keywords) {
        return toTitleList(newsController.search(keywords));
    }

    public synchronized List<NewsTitleDTO> getNewsByRegion(String region) {
        return toTitleList(newsController.findByRegion(region));
    }

    public synchronized List<CategoryDTO> getCategories() {
        List<CategoryDTO> result = new ArrayList<CategoryDTO>();
        for (CategoryBO category : categoryController.findAll()) {
            result.add(toCategoryDTO(category));
        }
        return result;
    }

    public synchronized NewsItemDTO createNews(CreateNewsRequestDTO request) {
        if (request == null || request.getCategoryId() == null) {
            return null;
        }
        CategoryBO category;
        try {
            category = categoryController.findById(request.getCategoryId());
        } catch (NoSuchElementException e) {
            return null;
        }
        NewsData data = new NewsData(
                trimToNull(request.getTitle()),
                trimToNull(request.getText()),
                trimToNull(request.getAnnotation()),
                isBlank(request.getRegion()) ? "Москва" : request.getRegion().trim(),
                Arrays.asList(category.getId()),
                request.getTags() == null ? new ArrayList<String>() : request.getTags()
        );
        NewsBO news = newsController.create(data);
        newsController.publish(news.getId());
        return toNewsItemDTO(news);
    }

    public synchronized NewsItemDTO updateNews(long id, UpdateNewsRequestDTO request) {
        NewsBO news;
        try {
            news = newsController.findById(id);
        } catch (NoSuchElementException e) {
            return null;
        }
        if (request == null) {
            return toNewsItemDTO(news);
        }

        List<Long> categoryIds = new ArrayList<Long>();
        for (CategoryBO category : news.getCategories()) {
            categoryIds.add(category.getId());
        }
        if (request.getCategoryId() != null && categoryExists(request.getCategoryId())) {
            categoryIds.add(request.getCategoryId());
        }

        NewsData data = new NewsData(
                request.getTitle() == null ? news.getTitle() : request.getTitle(),
                request.getText() == null ? news.getText() : request.getText(),
                request.getAnnotation() == null ? news.getAnnotation() : request.getAnnotation(),
                request.getRegion() == null ? news.getRegion() : request.getRegion(),
                categoryIds,
                request.getTags()
        );
        return toNewsItemDTO(newsController.update(id, data));
    }

    public synchronized boolean deleteNews(long id) {
        return newsController.delete(id);
    }

    public synchronized NewsItemDTO publishNews(long id) {
        try {
            return toNewsItemDTO(newsController.publish(id));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized NewsItemDTO unpublishNews(long id) {
        try {
            return toNewsItemDTO(newsController.unpublish(id));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized NewsItemDTO archiveNews(long id) {
        try {
            return toNewsItemDTO(newsController.archive(id));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized boolean addTag(long newsId, String tagName) {
        try {
            return newsController.addTag(newsId, tagName);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public synchronized boolean attachMedia(long newsId, String url) {
        try {
            return newsController.attachMedia(newsId, url);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // --- Авторизация (use case «Регистрация / Авторизация») ---

    public synchronized UserDTO register(RegistrationData data) {
        if (data == null || isBlank(data.getName()) || isBlank(data.getEmail())) {
            return null;
        }
        RoleBO role = roleController.findByName("USER");
        try {
            return toUserDTO(userController.register(data, role));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public synchronized boolean login(String login, String password) {
        return userController.login(login, password);
    }

    // --- Избранное (use case «Избранное») ---

    public synchronized boolean addBookmark(long userId, long newsId) {
        if (!userExists(userId) || !newsExists(newsId)) {
            return false;
        }
        return favoriteController.add(userId, newsId);
    }

    public synchronized boolean removeBookmark(long userId, long newsId) {
        if (!userExists(userId)) {
            return false;
        }
        favoriteController.remove(userId, newsId);
        return true;
    }

    public synchronized List<NewsTitleDTO> getFavorites(long userId) {
        List<NewsTitleDTO> result = new ArrayList<NewsTitleDTO>();
        if (!userExists(userId)) {
            return result;
        }
        for (FavoriteBO favorite : favoriteController.findByUser(userId)) {
            try {
                NewsBO news = newsController.findById(favorite.getNewsId());
                result.add(toNewsTitleDTO(news));
            } catch (NoSuchElementException ignored) {
                // Новость удалена: запись избранного просто пропускаем.
            }
        }
        return result;
    }

    // --- Комментарии (use case «Комментировать») ---

    public synchronized CommentDTO createComment(CreateCommentRequestDTO request) {
        if (request == null || request.getNewsId() == null || isBlank(request.getText())) {
            return null;
        }
        NewsBO news;
        try {
            news = newsController.findById(request.getNewsId());
        } catch (NoSuchElementException e) {
            return null;
        }
        CommentBO comment = commentController.create(
                news, request.getText().trim(), resolveAuthor(request.getAuthor()));
        return toCommentDTO(comment);
    }

    public synchronized CommentDTO updateComment(long id, UpdateCommentRequestDTO request) {
        if (request == null || isBlank(request.getText())) {
            return null;
        }
        try {
            return toCommentDTO(commentController.update(id, request.getText().trim()));
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    public synchronized boolean deleteComment(long id) {
        return commentController.delete(id);
    }

    // --- Администрирование (актёр «Администратор») ---

    public synchronized List<UserDTO> getUsers() {
        List<UserDTO> result = new ArrayList<UserDTO>();
        for (UserBO user : userController.findAll()) {
            result.add(toUserDTO(user));
        }
        return result;
    }

    public synchronized List<RoleDTO> getRoles() {
        List<RoleDTO> result = new ArrayList<RoleDTO>();
        for (RoleBO role : roleController.findAll()) {
            result.add(toRoleDTO(role));
        }
        return result;
    }

    public synchronized boolean blockUser(long userId) {
        return userController.block(userId);
    }

    public synchronized boolean assignRole(long userId, String roleName) {
        RoleBO role = roleController.findByName(roleName);
        return userController.assignRole(userId, role);
    }

    public synchronized boolean moderateComment(long commentId, String action) {
        try {
            return commentController.moderate(commentId, action);
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // --- ЛР4: проверка даты и создание контроллера с декоратором ---

    /**
     * Проверка текущей даты: если до конца года осталось меньше двух месяцев,
     * возвращает true, иначе false.
     */
    public boolean isEndOfYearNear() {
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);
        long daysLeft = ChronoUnit.DAYS.between(today, endOfYear);
        return daysLeft < DAYS_UNTIL_END_OF_YEAR;
    }

    private NewsInterface createNewsController(JournalistBO journalist) {
        NewsController controller = new NewsController(categoryController, tagController, journalist);
        if (isEndOfYearNear()) {
            return new PositiveNewsDecorator(controller);
        }
        return controller;
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
        return new NewsTitleDTO(news.getId(), news.getTitle(),
                firstCategoryName(news), format(news.getCreatedAt()));
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

    private UserDTO toUserDTO(UserBO user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail(), user.isBlocked(), roleNames(user));
    }

    private RoleDTO toRoleDTO(RoleBO role) {
        return new RoleDTO(role.getId(), role.getName(), new ArrayList<String>(role.getPermissions()));
    }

    private CommentDTO toCommentDTO(CommentBO comment) {
        return new CommentDTO(
                comment.getId(),
                comment.getNewsId(),
                comment.getAuthor() == null ? null : comment.getAuthor().getName(),
                comment.getText(),
                format(comment.getCreatedAt())
        );
    }

    // --- Вспомогательные методы ---

    private List<String> roleNames(UserBO user) {
        List<String> names = new ArrayList<String>();
        for (RoleBO role : user.getRoles()) {
            names.add(role.getName());
        }
        return names;
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

    private UserBO resolveAuthor(String name) {
        UserBO user = userController.findByName(name);
        if (user != null) {
            return user;
        }
        return new UserBO(null, isBlank(name) ? "Аноним" : name.trim(), null);
    }

    private boolean userExists(long id) {
        try {
            userController.findById(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean newsExists(long id) {
        try {
            newsController.findById(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean categoryExists(long id) {
        try {
            categoryController.findById(id);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
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
}
