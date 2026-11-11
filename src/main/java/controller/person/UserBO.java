package controller.person;

import controller.favorite.FavoriteBO;
import controller.role.RoleBO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Бизнес-объект «Пользователь» (UML: {@code User}).
 *
 * <p>Содержит дату регистрации, признак блокировки, роли и избранное.
 * Связан с комментариями ({@code User 1 -- 0..* Comment}) и избранным
 * ({@code User 1 -- 0..* Favorite}).</p>
 */
public class UserBO extends PersonBO {

    private Date registrationDate;
    private boolean blocked;
    private List<RoleBO> roles = new ArrayList<RoleBO>();
    private List<FavoriteBO> favorites = new ArrayList<FavoriteBO>();

    public UserBO(Long id, String name, String email) {
        super(id, name, email);
        this.registrationDate = new Date();
    }

    /** Добавить новость в избранное (метод {@code addToFavorites} из UML). */
    public void addToFavorites(long newsId) {
        for (FavoriteBO favorite : favorites) {
            if (favorite.getNewsId() != null && favorite.getNewsId() == newsId) {
                return;
            }
        }
        favorites.add(new FavoriteBO(null, new Date(), newsId));
    }

    /** Удалить новость из избранного (метод {@code removeFromFavorites} из UML). */
    public void removeFromFavorites(long newsId) {
        favorites.removeIf(favorite -> favorite.getNewsId() != null && favorite.getNewsId() == newsId);
    }

    /** Проверить, есть ли новость в избранном. */
    public boolean isFavorite(long newsId) {
        for (FavoriteBO favorite : favorites) {
            if (favorite.getNewsId() != null && favorite.getNewsId() == newsId) {
                return true;
            }
        }
        return false;
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

    public List<FavoriteBO> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<FavoriteBO> favorites) {
        this.favorites = favorites == null ? new ArrayList<FavoriteBO>() : favorites;
    }
}
