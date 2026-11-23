package controller.repository;

import controller.comment.CommentBO;
import controller.person.UserBO;
import model.IdSequence;
import model.data.CommentDO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Репозиторий бизнес-объекта «Комментарий» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Хранит комментарии в виде {@link CommentDO}. Связи с новостью и автором
 * хранятся идентификаторами: новость и пользователь живут в своих
 * репозиториях. Автор восстанавливается из {@link UserRepository}.</p>
 */
public class CommentRepository implements IRepository<CommentBO> {

    private final ConcurrentSkipListMap<Long, CommentDO> store = new ConcurrentSkipListMap<Long, CommentDO>();
    private final IdSequence ids = new IdSequence();
    private final UserRepository userRepository;

    public CommentRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
        seed();
    }

    /** Тестовый набор данных (задача 3 ЛР6). */
    private void seed() {
        UserBO reader = userRepository.findById(2L);
        if (reader != null) {
            save(new CommentBO(1L, 1L,
                    "Отличная новость, давно ждал обновления портала!",
                    new Date(), reader));
        }
        ids.ensureAtLeast(2L);
    }

    @Override
    public List<CommentBO> findAll() {
        List<CommentBO> result = new ArrayList<CommentBO>();
        for (CommentDO entity : store.values()) {
            result.add(toBO(entity));
        }
        return result;
    }

    @Override
    public CommentBO findById(Long id) {
        if (id == null) {
            return null;
        }
        CommentDO entity = store.get(id);
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
        store.put(item.getId(), toDO(item));
        return item;
    }

    @Override
    public boolean delete(Long id) {
        return id != null && store.remove(id) != null;
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
