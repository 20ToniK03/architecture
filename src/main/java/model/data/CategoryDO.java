package model.data;

/**
 * Модель данных бизнес-объекта «Категория» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Плоская структура данных: у категории нет ссылок на другие объекты,
 * поэтому одного DO-класса достаточно. Поле {@code description} -
 * вспомогательное (для подсказок в интерфейсе).</p>
 */
public class CategoryDO {

    public Long id;
    public String name;
    public String description;

    public CategoryDO() {
    }

    public CategoryDO(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
