package model.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель данных бизнес-объекта «Пользователь» (ЛР6, паттерн «Репозиторий»).
 *
 * <p>Один DO-класс описывает все виды персон: обычного пользователя,
 * журналиста и администратора. Поле {@code type} хранит конкретный класс
 * бизнес-объекта, а поле {@code rating} - рейтинг журналиста. Роли хранятся
 * идентификаторами ({@code roleIds}) и восстанавливаются из репозитория
 * ролей.</p>
 */
public class UserDO {

    public Long id;
    public String type;
    public String name;
    public String email;
    public String passwordHash;
    public long registrationDate;
    public boolean blocked;
    public double rating;
    public List<Long> roleIds = new ArrayList<Long>();
}
