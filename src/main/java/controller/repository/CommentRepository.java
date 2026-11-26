package controller.repository;

import controller.comment.CommentBO;
import controller.person.UserBO;
import model.IdSequence;
import model.dao.Database;
import model.dao.SqlDao;
import model.data.CommentDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Репозиторий бизнес-объекта «Комментарий» (ЛР6, ЛР7).
 *
 * <p>Хранит комментарии в реляционном хранилище SQLite в виде {@link CommentDO}.
 * Связи с новостью и автором хранятся идентификаторами: новость и пользователь
 * живут в своих репозиториях. Автор восстанавливается из {@link UserRepository},
 * доступ к данным - через {@link SqlDao}.</p>
 */
public class CommentRepository implements IRepository<CommentBO> {

    private final SqlDao<CommentDO, Long> dao =
            new SqlDao<CommentDO, Long>(Database.sqlite(), CommentDO.class, "comments", "id");
    private final IdSequence ids = new IdSequence();
    private final UserRepository userRepository;

    public CommentRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        if (dao.findAll().isEmpty()) {
            seed();
        }
        ids.ensureAtLeast(nextId());
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        UserBO reader = userRepository.findById(2L);
        if (reader != null) {
            save(new CommentBO(1L, 1L,
                    "Отличная новость, давно ждал обновления портала!",
                    new Date(), reader));
        }
    }

    @Override
    public List<CommentBO> findAll() {
        List<CommentBO> result = new ArrayList<CommentBO>();
        for (CommentDO entity : dao.findAll()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public CommentBO findById(Long id) {
        CommentDO entity = dao.findById(id);
        return entity == null ? null : toBO(entity);
    }

    @Override
    public CommentBO save(CommentBO item) {
        if (item == null) {
            return null;
        }
        if (item.getId() == null) {
            item.setId(ids.next());
        }
        dao.save(toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        if (id == null || dao.findById(id) == null) {
            return false;
        }
        dao.deleteById(id);
        return true;
    }

    private long nextId() {
        long max = 0L;
        for (CommentDO entity : dao.findAll()) {
            if (entity.id != null && entity.id > max) {
                max = entity.id;
            }
        }
        return max + 1L;
    }

    private CommentBO toBO(CommentDO entity) {
        CommentBO comment = new CommentBO(entity.id, entity.newsId, entity.text,
                entity.createdAt > 0L ? new Date(entity.createdAt) : new Date(),
                userRepository.findById(entity.authorId));
        if (!entity.approved) {
            comment.reject();
        }
        return comment;
    }

    private CommentDO toDO(CommentBO item) {
        CommentDO entity = new CommentDO();
        entity.id = item.getId();
        entity.newsId = item.getNewsId();
        entity.text = item.getText();
        entity.createdAt = item.getCreatedAt() == null ? 0L : item.getCreatedAt().getTime();
        entity.authorId = item.getAuthor() == null ? null : item.getAuthor().getId();
        entity.approved = item.isApproved();
        return entity;
    }
}
