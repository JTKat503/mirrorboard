package com.teamcreators.mirrorboard.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * A class that builds a certain user
 *
 * @author Jianwei Li
 */
public class User implements Serializable {
    public String name, phone, token, avatarUri;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return phone.equals(user.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phone);
    }

    /**
     * Get the name of this user.
     * @return the name of this user
     */
    public String getName() {
        return name;
    }
}
