package model.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель данных бизнес-объекта «Роль» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Список прав роли хранится в виде обычного списка строк: это простые
 * значения, которым не нужен отдельный DO-класс.</p>
 */
public class RoleDO {

    public Long id;
    public String name;
    public List<String> permissions = new ArrayList<String>();

    public RoleDO() {
    }

    public RoleDO(Long id, String name, List<String> permissions) {
        this.id = id;
        this.name = name;
        this.permissions = permissions == null ? new ArrayList<String>() : permissions;
    }
}
