package controller.person;

import controller.role.RoleBO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Бизнес-объект «Пользователь» (UML: {@code User}).
 *
 * <p>Содержит дату регистрации, признак блокировки и роли. Связан с
 * комментариями ({@code User 1 -- 0..* Comment}). Избранное вынесено в
 * отдельный бизнес-объект {@code FavoriteBO} и контроллер
 * {@code FavoriteController}, поэтому список избранного здесь не хранится.</p>
 */
public class UserBO extends PersonBO {

    private Date registrationDate;
    private boolean blocked;
    private List<RoleBO> roles = new ArrayList<RoleBO>();

    public UserBO(Long id, String name, String email) {
        super(id, name, email);
        this.registrationDate = new Date();
    }

    /** Добавить роль, если её ещё нет. */
    public boolean addRole(RoleBO role) {
        if (role == null) {
            return false;
        }
        for (RoleBO existing : roles) {
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(role.getName())) {
                return false;
            }
        }
        roles.add(role);
        return true;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public List<RoleBO> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleBO> roles) {
        this.roles = roles == null ? new ArrayList<RoleBO>() : roles;
    }
}
