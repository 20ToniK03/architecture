package controller;

import controller.comment.CommentBO;
import controller.favorite.FavoriteBO;
import controller.news.NewsController;
import controller.news.NewsInterface;
import controller.news.decor.PositiveNewsDecorator;
import controller.person.AdminBO;
import controller.person.GuestBO;
import controller.person.JournalistBO;
import controller.person.RegistrationData;
import controller.person.UserBO;
import controller.role.RoleBO;
import model.IdSequence;
import model.InMemoryStore;
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

/**
 * Фасад (паттерн Facade) между слоем представления и предметной областью.
 *
 * <p>Скрывает бизнес-объекты и хранилища за единым интерфейсом, принимает DTO
 * запросов и возвращает DTO ответов. В ЛР4 работа с новостями вынесена в
 * {@link NewsController}, а фасад обращается к нему через интерфейс
 * {@link NewsInterface}, что позволяет оборачивать контроллер декораторами.</p>
 */
public final class FacadeController {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    /** Порог в днях: считается, что до конца года осталось «меньше двух месяцев». */
    private static final int DAYS_UNTIL_END_OF_YEAR = 60;

    private static final FacadeController INSTANCE = new FacadeController();

    private final NewsInterface newsController;

    private final InMemoryStore<CommentBO> commentStore = new InMemoryStore<CommentBO>();
    private final InMemoryStore<UserBO> userStore = new InMemoryStore<UserBO>();
    private final InMemoryStore<RoleBO> roleStore = new InMemoryStore<RoleBO>();

    private final IdSequence commentIds = new IdSequence();
    private final IdSequence userIds = new IdSequence();
    private final IdSequence roleIds = new IdSequence();

    private final GuestBO guest = new GuestBO();
    private JournalistBO journalist;
    private AdminBO admin;

    private FacadeController() {
        seedRoles();
        seedUsers();
        this.newsController = createNewsController();
        seedComments();
    }

    public static FacadeController getInstance() {
        return INSTANCE;
    }

    // --- Новости (делегирование NewsController, ЛР4) ---

    public synchronized List<NewsTitleDTO> getAllNews() {
        return newsController.getAll();
    }

    public synchronized NewsItemDTO getNewsById(long id) {
        return newsController.getNewsById(id);
    }

    public synchronized List<NewsTitleDTO> getLastWeekNews() {
        return newsController.getLastWeekNews();
    }

    public synchronized List<NewsTitleDTO> searchNews(List<String> keywords) {
        return newsController.searchNews(keywords);
    }

    public synchronized List<NewsTitleDTO> getNewsByRegion(String region) {
        return newsController.getNewsByRegion(region);
    }

    public synchronized List<CategoryDTO> getCategories() {
        return newsController.getCategories();
    }

    public synchronized NewsItemDTO createNews(CreateNewsRequestDTO request) {
        return newsController.createNews(request);
    }

    public synchronized NewsItemDTO updateNews(long id, UpdateNewsRequestDTO request) {
        return newsController.updateNews(id, request);
    }

    public synchronized boolean deleteNews(long id) {
        return newsController.deleteNews(id);
    }

    public synchronized NewsItemDTO publishNews(long id) {
        return newsController.publishNews(id);
    }

    public synchronized NewsItemDTO unpublishNews(long id) {
        return newsController.unpublishNews(id);
    }

    public synchronized NewsItemDTO archiveNews(long id) {
        return newsController.archiveNews(id);
    }

    public synchronized boolean addTag(long newsId, String tagName) {
        return newsController.addTag(newsId, tagName);
    }

    public synchronized boolean attachMedia(long newsId, String url) {
        return newsController.attachMedia(newsId, url);
    }

    // --- Авторизация (use case «Регистрация / Авторизация») ---

    public synchronized UserDTO register(RegistrationData data) {
        if (data == null || isBlank(data.getName()) || isBlank(data.getEmail())) {
            return null;
        }
        if (findUserByEmail(data.getEmail()) != null) {
            return null;
        }
        UserBO user = guest.register(data);
        user.setId(userIds.next());
        RoleBO role = roleStore.find(2L);
        if (role != null) {
            user.addRole(role);
        }
        userStore.put(user.getId(), user);
        return toUserDTO(user);
    }

    public synchronized boolean login(String login, String password) {
        UserBO user = findUserByEmail(login);
        return user != null && user.login(login, password);
    }

    // --- Избранное (use case «Избранное») ---

    public synchronized boolean addBookmark(long userId, long newsId) {
        UserBO user = userStore.find(userId);
        if (user == null || newsController.getNewsById(newsId) == null) {
            return false;
        }
        user.addToFavorites(newsId);
        return true;
    }

    public synchronized boolean removeBookmark(long userId, long newsId) {
        UserBO user = userStore.find(userId);
        if (user == null) {
            return false;
        }
        user.removeFromFavorites(newsId);
        return true;
    }

    public synchronized List<NewsTitleDTO> getFavorites(long userId) {
        UserBO user = userStore.find(userId);
        if (user == null) {
            return new ArrayList<NewsTitleDTO>();
        }
        List<NewsTitleDTO> result = new ArrayList<NewsTitleDTO>();
        for (FavoriteBO favorite : user.getFavorites()) {
            NewsItemDTO news = newsController.getNewsById(favorite.getNewsId());
            if (news != null) {
                result.add(new NewsTitleDTO(news.getId(), news.getTitle(), news.getCategory(), news.getPublishedAt()));
            }
        }
        return result;
    }

    // --- Комментарии (use case «Комментировать») ---

    public synchronized CommentDTO createComment(CreateCommentRequestDTO request) {
        if (request == null || request.getNewsId() == null || isBlank(request.getText())) {
            return null;
        }
        if (newsController.getNewsById(request.getNewsId()) == null) {
            return null;
        }
        CommentBO comment = new CommentBO(
                commentIds.next(),
                request.getNewsId(),
                request.getText().trim(),
                new Date(),
                resolveAuthor(request.getAuthor())
        );
        commentStore.put(comment.getId(), comment);
        return toCommentDTO(comment);
    }

    public synchronized CommentDTO updateComment(long id, UpdateCommentRequestDTO request) {
        CommentBO comment = commentStore.find(id);
        if (comment == null || request == null || isBlank(request.getText())) {
            return null;
        }
        comment.edit(request.getText().trim());
        return toCommentDTO(comment);
    }

    public synchronized boolean deleteComment(long id) {
        return commentStore.remove(id);
    }

    // --- Администрирование (актёр «Администратор») ---

    public synchronized List<UserDTO> getUsers() {
        List<UserDTO> result = new ArrayList<UserDTO>();
        for (UserBO user : userStore.findAll()) {
            result.add(toUserDTO(user));
        }
        return result;
    }

    public synchronized List<RoleDTO> getRoles() {
        List<RoleDTO> result = new ArrayList<RoleDTO>();
        for (RoleBO role : roleStore.findAll()) {
            result.add(toRoleDTO(role));
        }
        return result;
    }

    public synchronized boolean blockUser(long userId) {
        UserBO user = userStore.find(userId);
        if (user == null) {
            return false;
        }
        admin.blockUser(user);
        return true;
    }

    public synchronized boolean assignRole(long userId, String roleName) {
        UserBO user = userStore.find(userId);
        RoleBO role = findRoleByName(roleName);
        if (user == null || role == null) {
            return false;
        }
        admin.assignRole(user, role);
        return true;
    }

    public synchronized boolean moderateComment(long commentId, String action) {
        CommentBO comment = commentStore.find(commentId);
        if (comment == null) {
            return false;
        }
        if ("approve".equalsIgnoreCase(action)) {
            admin.moderateComment(comment);
        } else if ("reject".equalsIgnoreCase(action)) {
            comment.setApproved(false);
        } else {
            return false;
        }
        return true;
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

    private NewsInterface createNewsController() {
        NewsController controller = new NewsController(journalist);
        if (isEndOfYearNear()) {
            return new PositiveNewsDecorator(controller);
        }
        return controller;
    }

    // --- Преобразование BO -> DTO ---

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

    private UserBO findUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        for (UserBO user : userStore.findAll()) {
            if (email.equalsIgnoreCase(user.getEmail())) {
                return user;
            }
        }
        return null;
    }

    private RoleBO findRoleByName(String name) {
        if (name == null) {
            return null;
        }
        for (RoleBO role : roleStore.findAll()) {
            if (name.equalsIgnoreCase(role.getName())) {
                return role;
            }
        }
        return null;
    }

    private UserBO resolveAuthor(String name) {
        if (name != null) {
            for (UserBO user : userStore.findAll()) {
                if (name.equalsIgnoreCase(user.getName())) {
                    return user;
                }
            }
        }
        return new UserBO(null, isBlank(name) ? "Аноним" : name.trim(), null);
    }

    private String format(Date date) {
        return date == null ? null : DATE_TIME_FORMAT.format(date);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // --- Начальные (тестовые) данные ---

    private void seedRoles() {
        addRole("GUEST", Arrays.asList("news.view"));
        addRole("USER", Arrays.asList("news.view", "news.comment", "news.favorite"));
        addRole("JOURNALIST", Arrays.asList("news.view", "news.create", "news.edit", "news.delete"));
        addRole("ADMIN", Arrays.asList("user.block", "user.role", "comment.moderate"));
    }

    private void addRole(String name, List<String> permissions) {
        RoleBO role = new RoleBO(roleIds.next(), name, permissions);
        roleStore.put(role.getId(), role);
    }

    private void seedUsers() {
        journalist = new JournalistBO(userIds.next(), "Мария Соколова", "maria@example.com");
        journalist.setPasswordHash("pass");
        journalist.setRating(4.8);
        journalist.addRole(findRoleByName("JOURNALIST"));
        userStore.put(journalist.getId(), journalist);

        UserBO reader = new UserBO(userIds.next(), "Иван Петров", "ivan@example.com");
        reader.setPasswordHash("pass");
        reader.addRole(findRoleByName("USER"));
        userStore.put(reader.getId(), reader);

        admin = new AdminBO(userIds.next(), "Алексей Админов", "admin@example.com");
        admin.setPasswordHash("admin");
        admin.addRole(findRoleByName("ADMIN"));
        userStore.put(admin.getId(), admin);
    }

    private void seedComments() {
        CommentBO comment = new CommentBO(
                commentIds.next(),
                1L,
                "Отличная новость, давно ждал обновления портала!",
                new Date(),
                findUserByEmail("ivan@example.com")
        );
        commentStore.put(comment.getId(), comment);
    }
}
