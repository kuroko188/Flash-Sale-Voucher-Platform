package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserHolderTest {

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void saveGetAndRemoveUser() {
        UserDTO user = new UserDTO();
        user.setId(1L);
        user.setNickName("tester");

        UserHolder.saveUser(user);
        assertEquals(user, UserHolder.getUser());

        UserHolder.removeUser();
        assertNull(UserHolder.getUser());
    }
}
