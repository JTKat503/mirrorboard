package com.teamcreators.mirrorboard.listeners;

import com.teamcreators.mirrorboard.models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);

    void displayContactInformation(User user);

    void onMultipleUsersAction(Boolean isMultipleUsersSelected);
}

