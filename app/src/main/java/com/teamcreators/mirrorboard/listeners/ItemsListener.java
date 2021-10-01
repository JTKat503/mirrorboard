package com.teamcreators.mirrorboard.listeners;

import com.teamcreators.mirrorboard.models.Hobby;
import com.teamcreators.mirrorboard.models.User;

public interface ItemsListener {

    /**
     * Implementation of ItemsAdapter interface method.
     * Jump to the InfoContactActivity and display the personal information of the selected user
     * @param user the contact selected by clicking, whose information is to be displayed
     */
    void displayContactInformation(User user);

    /**
     * Implementation of ItemsAdapter interface method.
     * Jump to the MatchHobbyActivity and display the hobby information of the selected hobby
     * @param hobby the hobby selected by clicking, whose information is to be displayed
     */
    void displayHobbyInformation(Hobby hobby);

    /**
     * Implementation of ItemsAdapter interface method.
     * If multiple contacts are selected, start CallOutgoingActivity,
     * and pass the selected contacts to the next activity.
     * @param isMultipleUsersSelected boolean:  true -> multiple contacts are selected
     *                                         false -> no contact is selected
     */
    void onMultipleUsersAction(Boolean isMultipleUsersSelected);
}