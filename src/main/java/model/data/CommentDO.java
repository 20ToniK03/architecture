package model.data;

/**
 * Модель данных бизнес-объекта «Комментарий» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Комментарий связан с новостью и автором идентификаторами: {@code newsId}
 * и {@code authorId}. Признак {@code approved} отражает результат модерации.</p>
 */
public class CommentDO {

    public Long id;
    public Long newsId;
    public String text;
    public long createdAt;
    public Long authorId;
    public boolean approved;
}
