package com.sep.vox.application.port.input.command.user;

import com.sep.vox.application.port.input.command.schooluser.UpdateSchoolUserCommand;
import java.time.LocalDate;

/**
 * Updates the profile of the CALLER ONLY. It deliberately has no {@code userId}: the id always comes from
 * {@code UserContextPort}, so this mutation has no path to edit another person's profile. Editing someone
 * else's profile goes through {@code updateSchoolUser} (school admin, restricted to their own school).
 *
 * <p>The {@code x} + {@code xProvided} pair is the repo's PATCH convention -- see {@link UpdateSchoolUserCommand}.
 * The GraphQL input tells "the client did not send the field" from "the client sent null", so
 * {@code avatarUrl: null} means REMOVE the avatar while not sending {@code avatarUrl} keeps it -- no
 * removeAvatar mutation is needed just to delete one.
 */
public record UpdateProfileCommand(
        String fullName,
        boolean fullNameProvided,
        String phone,
        boolean phoneProvided,
        String address,
        boolean addressProvided,
        LocalDate dateOfBirth,
        boolean dateOfBirthProvided,
        String avatarUrl,
        boolean avatarUrlProvided) {
}
