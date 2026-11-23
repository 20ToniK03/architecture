package model.data;

/**
 * Модель данных бизнес-объекта «Тег» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Тег хранится идентификатором и названием. Название уникально без учёта
 * регистра: уникальность проверяет контроллер, а репозиторий только хранит
 * данные.</p>
 */
public class TagDO {

    public Long id;
    public String name;

    public TagDO() {
    }

    public TagDO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
