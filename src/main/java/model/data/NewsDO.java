package model.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель данных «ядро + превью» бизнес-объекта «Новость» (ЛР6, паттерн
 * «Репозиторий»).
 *
 * <p>Этот DO-класс описывает ту часть новости, которая нужна для списков и
 * лент: идентификатор, заголовок, анонс, регион, рейтинг, статус и ссылки
 * на связанные объекты (автор, категории, теги). Полный текст вынесен в
 * отдельный {@link NewsContentDO}, потому что он требуется только при
 * открытии конкретной статьи. Так разные модели данных новости можно
 * хранить в разных хранилищах.</p>
 *
 * <p>Связанные объекты хранятся идентификаторами: {@code authorId} ссылается
 * на пользователя, {@code categoryIds} и {@code tagIds} - на категории и теги.
 * При восстановлении бизнес-объекта репозиторий заменяет идентификаторы
 * объектами.</p>
 */
public class NewsDO {

    public Long id;
    public String title;
    public String annotation;
    public String region;
    public String status;
    public Long authorId;
    public int rating;
    public List<Long> categoryIds = new ArrayList<Long>();
    public List<Long> tagIds = new ArrayList<Long>();
    public List<String> media = new ArrayList<String>();
    public long createdAt;
    public long updatedAt;
}
