package model.data;

/**
 * Модель данных бизнес-объекта «Избранное» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Связь «пользователь - новость» хранится идентификаторами: сами
 * пользователь и новость живут в своих репозиториях, здесь только факт
 * добавления в избранное и его дата.</p>
 */
public class FavoriteDO {

    public Long id;
    public Long userId;
    public Long newsId;
    public long addedAt;
}
